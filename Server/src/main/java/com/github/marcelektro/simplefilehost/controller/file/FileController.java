package com.github.marcelektro.simplefilehost.controller.file;

import com.github.marcelektro.simplefilehost.dto.ErrorResponse;
import com.github.marcelektro.simplefilehost.dto.file.FileMetaDto;
import com.github.marcelektro.simplefilehost.dto.file.ListFilesResponseDto;
import com.github.marcelektro.simplefilehost.dto.file.UploadFileResponseDto;
import com.github.marcelektro.simplefilehost.service.file.FileUploadService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileController {

    private final FileUploadService fileUploadService;

    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }


    public void handleUploadFile(Context ctx) throws Exception {

        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        var uploaded = ctx.uploadedFile("file");

        if (uploaded == null) {
            ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("FILE_MISSING", "No file uploaded as \"file\" form field"));
            return;
        }

        var originalFilename = uploaded.filename();
        var size = uploaded.size();

        try (var inputStream = uploaded.content()) {
            var fileRes = this.fileUploadService.uploadFile(userId, originalFilename, size, inputStream);

            if (!fileRes.isSuccess()) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of(fileRes.getErrorCode(), fileRes.getMessage()));
                return;
            }

            ctx.status(HttpStatus.CREATED).json(new UploadFileResponseDto(fileRes.getData()));
        }
    }


    public void handleDownloadFile(Context ctx) throws Exception {

        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String fileId = ctx.pathParam("fileId");
        if (fileId.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(ErrorResponse.of("FILE_ID_MISSING", "File ID must not be empty"));
            return;
        }

        var result = this.fileUploadService.downloadByFileId(userId, fileId);
        if (!result.isSuccess()) {

            switch (result.getErrorCode()) {
                case "FILE_NOT_FOUND" -> ctx.status(HttpStatus.NOT_FOUND).json(ErrorResponse.of("FILE_NOT_FOUND", "File not found or access denied"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorResponse.of(result.getErrorCode(), result.getMessage()));
            }

            return;
        }

        ctx.header("Content-Disposition", "attachment; filename=\"" + result.getData().originalFilename() + "\"");
        ctx.contentType("application/octet-stream");
        ctx.status(HttpStatus.OK).result(Files.newInputStream(result.getData().file().toPath()));

    }


    public void handleDownloadBySharedLink(Context ctx) throws Exception {
        String linkId = ctx.pathParam("linkId");

        if (linkId.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("LINK_ID_MISSING", "Link ID is required"));
            return;
        }

        String password = ctx.queryParam("password");


        var res = fileUploadService.downloadBySharedLink(linkId, password);

        if (!res.isSuccess()) {
            switch (res.getErrorCode()) {
                case "LINK_NOT_FOUND" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("LINK_NOT_FOUND", "Shared link not found or expired"));
                case "LINK_EXPIRED" -> ctx.status(HttpStatus.GONE)
                        .json(ErrorResponse.of("LINK_EXPIRED", "Shared link has expired"));
                case "INVALID_PASSWORD" -> ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(ErrorResponse.of("INVALID_PASSWORD", "Invalid password for shared link"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.header("Content-Disposition", "attachment; filename=\"" + res.getData().originalFilename() + "\"");
        ctx.contentType("application/octet-stream");
        ctx.status(HttpStatus.OK).result(Files.newInputStream(res.getData().file().toPath()));
    }


    public void handleListFiles(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String sortParam = ctx.queryParam("sort");
        FileUploadService.SortBy sortBy = FileUploadService.SortBy.DATE_DESC;
        if (sortParam != null) {
            try {
                sortBy = FileUploadService.SortBy.valueOf(sortParam);
            } catch (IllegalArgumentException ignored) {

                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(ErrorResponse.of("INVALID_SORT_MODE", "Invalid sort parameter. Valid values are: "
                                + Arrays.stream(FileUploadService.SortBy.values()).map(Enum::name).collect(Collectors.joining(", "))));
                return;
            }
        }


        var filesRes = fileUploadService.listFiles(userId, sortBy);

        var res = filesRes.getData().stream()
                .map(uploadedFileSummary -> new FileMetaDto(
                        uploadedFileSummary.fileId,
                        uploadedFileSummary.filename,
                        uploadedFileSummary.size,
                        uploadedFileSummary.uploadDate,
                        uploadedFileSummary.sharedLinksCount
                )).toList();

        ctx.json(new ListFilesResponseDto(res));

    }

    public void handleDeleteFile(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String fileId = ctx.pathParam("fileId");
        if (fileId.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("FILE_ID_MISSING", "File ID must not be empty"));
            return;
        }

        var res = fileUploadService.deleteFile(userId, fileId);

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "FILE_NOT_FOUND" -> ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("FILE_NOT_FOUND", "File not found or access denied"));
                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.status(HttpStatus.NO_CONTENT);

    }


}
