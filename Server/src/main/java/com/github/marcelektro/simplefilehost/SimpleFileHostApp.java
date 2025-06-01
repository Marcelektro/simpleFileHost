package com.github.marcelektro.simplefilehost;

import com.github.marcelektro.simplefilehost.config.ConfigManager;
import com.github.marcelektro.simplefilehost.console.ConsoleInputHandler;
import com.github.marcelektro.simplefilehost.controller.auth.AuthController;
import com.github.marcelektro.simplefilehost.controller.file.FileController;
import com.github.marcelektro.simplefilehost.controller.sharing.SharingController;
import com.github.marcelektro.simplefilehost.dto.ErrorResponse;
import com.github.marcelektro.simplefilehost.middleware.AuthMiddleware;
import com.github.marcelektro.simplefilehost.roles.DefaultRoles;
import com.github.marcelektro.simplefilehost.service.auth.AuthServiceImpl;
import com.github.marcelektro.simplefilehost.service.db.SQLiteDatabaseService;
import com.github.marcelektro.simplefilehost.service.file.FileUploadServiceImpl;
import com.github.marcelektro.simplefilehost.service.sharing.ShareLinkServiceImpl;
import com.github.marcelektro.simplefilehost.util.LocalDateTimeTypeAdapter;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinGson;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.*;

@Slf4j
public class SimpleFileHostApp {

    private final boolean debugMode;

    private final AuthController authController;
    private final AuthMiddleware authMiddleware;
    private final FileController fileController;
    private final SharingController sharingController;

    private final ConfigManager configManager;


    public SimpleFileHostApp() throws Exception {

        this.configManager = new ConfigManager(new File("./config.json"));
        this.configManager.initConfig();

        var config = this.configManager.get();

        this.debugMode = config.debugMode;

        var dataDirectory = new File(config.dataDirectory);
        if (dataDirectory.mkdirs())
            log.info("Creating data directory: {}", dataDirectory.getAbsolutePath());


        var uploadedDataDirectory = new File(dataDirectory, "blobs");
        if (uploadedDataDirectory.mkdirs())
            log.info("Creating uploaded data directory: {}", uploadedDataDirectory.getAbsolutePath());

        var databaseFile = new File(dataDirectory, "database.sqlite.db");

        var dbService = new SQLiteDatabaseService(databaseFile.getPath());
        dbService.initialSetup();
        var authService = new AuthServiceImpl(dbService, config.jwtSecretKey);
        var fileUploadService = new FileUploadServiceImpl(uploadedDataDirectory, dbService);
        var shareLinkService = new ShareLinkServiceImpl(dbService);

        new Thread(new ConsoleInputHandler(authService), "ConsoleInputHandler").start();

        this.authController = new AuthController(authService);
        this.fileController = new FileController(fileUploadService);
        this.sharingController = new SharingController(shareLinkService);

        this.authMiddleware = new AuthMiddleware(authService);


        var host = config.host;
        var port = config.port;

        log.info("Starting server on {}:{}", host, port);
        Javalin.create(this::config).start(host, port);

    }


    private void config(JavalinConfig config) {

        config.useVirtualThreads = true;

        config.showJavalinBanner = false;

        config.staticFiles.add(sfc -> {
            sfc.directory = "/public";
            sfc.hostedPath = "/"; // serve static files under / path
            sfc.location = Location.CLASSPATH;
            sfc.precompress = true;
        });

        var gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .serializeNulls()
                .create();

        // Setup Json mapper
        config.jsonMapper(new JavalinGson(gson, true));


        var corsAllowedOrigins = this.configManager.get().corsAllowedOrigins;

        if (corsAllowedOrigins.isEmpty()) {
            log.error("CORS allowed origins are empty, rendering CORS disabled!");
        } else {
            log.info("CORS allowed origins: {}", String.join(", ", corsAllowedOrigins));

            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> {
                it.path = "*";
                it.allowCredentials = true;
                it.allowHost(
                        corsAllowedOrigins.getFirst(),
                        corsAllowedOrigins.stream().skip(1).toArray(String[]::new)
                );
            }));
        }

        if (this.debugMode)
            config.bundledPlugins.enableDevLogging();


        config.router.apiBuilder(() -> path("api", () -> {

            path("auth", () -> {
                post("login", authController::handleLogin, DefaultRoles.ANONYMOUS);
                get("me", authController::handleMe, DefaultRoles.ANONYMOUS); // actually requires auth, but we check it in the handler for precise token validation
            });

            path("files", () -> {
                get(fileController::handleListFiles, DefaultRoles.USER); // get files
                post("upload", fileController::handleUploadFile, DefaultRoles.USER);
                get("{fileId}", fileController::handleDownloadFile, DefaultRoles.USER);
                delete("{fileId}", fileController::handleDeleteFile, DefaultRoles.USER);
                get("{fileId}/shareLinks", sharingController::handleListShareLinksForFile, DefaultRoles.USER);
            });

            path("sharing", () -> {
                post(sharingController::handleCreateShareLink, DefaultRoles.USER);
                put("{linkId}", sharingController::handleUpdateShareLink, DefaultRoles.USER);
                delete("{linkId}", sharingController::handleDeleteShareLink, DefaultRoles.USER);

                get("{linkId}", fileController::handleDownloadBySharedLink, DefaultRoles.ANONYMOUS);
                get("{linkId}/validate", sharingController::handleValidateShareLink, DefaultRoles.ANONYMOUS);
            });

        }));

        config.router.mount(s -> {

            s.beforeMatched("*", ctx -> {

                if (ctx.routeRoles().contains(DefaultRoles.USER)) {
                    authMiddleware.validateAuthToken(ctx);
                }

            });

            s.exception(Exception.class, (e, ctx) -> {

                log.error("Exception thrown while handling `{}`:", ctx.path(), e);

                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of("INTERNAL_SERVER_ERROR", "an unhandled error has occurred (" + e.getClass().getSimpleName() + ")"));
            });

            s.exception(ValidationException.class, (e, ctx) -> ctx.status(HttpStatus.BAD_REQUEST)
                    .json(ErrorResponse.of("MALFORMED_REQUEST", e.getErrors().values().stream()
                            .map(ve -> ve.stream()
                                    .map(ValidationError::getMessage)
                                    .collect(Collectors.joining(", ")))
                            .collect(Collectors.joining(", "))
                    ))
            );

            s.error(HttpStatus.NOT_FOUND, ctx -> {
                // handle only unhandled 404s
                if (!ctx.handlerType().equals(HandlerType.BEFORE))
                    return;

                ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of("ENDPOINT_NOT_FOUND", "the requested resource was not found"));
            });

        });

    }




    public static void main(String[] args) {
        try {
            new SimpleFileHostApp();
        } catch (Exception e) {
            System.err.println("Initialization failed!");
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(1);
        }
    }

}