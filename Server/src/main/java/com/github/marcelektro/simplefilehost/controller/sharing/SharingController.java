package com.github.marcelektro.simplefilehost.controller.sharing;

import com.github.marcelektro.simplefilehost.dto.ErrorResponse;
import com.github.marcelektro.simplefilehost.dto.sharing.*;
import com.github.marcelektro.simplefilehost.service.sharing.ShareLinkService;
import com.github.marcelektro.simplefilehost.util.Checks;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class SharingController {

    private final ShareLinkService shareLinkService;

    public SharingController(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }


    public void handleCreateShareLink(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        var req = ctx.bodyValidator(CreateShareLinkRequestDto.class)
                .check(r -> Checks.nonEmpty(r.getFileId()), "File ID must not be empty")
                .get();

        var res = shareLinkService.createShareLink(
                userId,
                req.getFileId(),
                req.getPassword(),
                req.getExpiry()
        );

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "ACCESS_DENIED" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("FILE_NOT_FOUND", "No permission or file not found"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.status(HttpStatus.CREATED).json(new CreateShareLinkResponseDto(res.getData()));
    }


    public void handleUpdateShareLink(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String linkId = ctx.pathParam("linkId");
        if (Checks.empty(linkId)) {
            ctx.status(HttpStatus.BAD_REQUEST).json(ErrorResponse.of("LINK_ID_MISSING", "Link ID is required"));
            return;
        }

        var req = ctx.bodyValidator(UpdateShareLinkRequestDto.class)
                .get();

        var res = shareLinkService.updateShareLink(userId, linkId, req.getPassword(), req.getExpiry());

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "ACCESS_DENIED" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("FILE_NOT_FOUND", "No permission or file not found"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.status(HttpStatus.NO_CONTENT);

    }


    public void handleDeleteShareLink(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String linkId = ctx.pathParam("linkId");
        if (Checks.empty(linkId)) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(ErrorResponse.of("LINK_ID_MISSING", "Link ID is required"));
            return;
        }

        var res = shareLinkService.deleteShareLink(userId, linkId);

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "ACCESS_DENIED" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("FILE_NOT_FOUND", "No permission or file not found"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        ctx.status(HttpStatus.NO_CONTENT);

    }


    public void handleValidateShareLink(Context ctx) throws Exception {
        String linkId = ctx.pathParam("linkId");

        if (Checks.empty(linkId)) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(ErrorResponse.of("LINK_ID_MISSING", "Link ID is required"));
            return;
        }

        String password = ctx.queryParam("password");

        var res = shareLinkService.validateLink(linkId, password);

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "LINK_NOT_FOUND" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("LINK_NOT_FOUND", "Shared link not found"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }


        var resData = res.getData();

        ctx.status(HttpStatus.OK).json(new ShareLinkValidationResultDto(
                linkId,
                resData.fileId(),
                resData.originalFilename(),
                resData.fileSize(),
                resData.passwordProtected(),
                resData.validPassword(),
                resData.expiry(),
                resData.hasExpired()
        ));

    }

    public void handleListShareLinksForFile(Context ctx) throws Exception {
        String userId = ctx.attribute("userId");
        if (userId == null)
            throw new IllegalStateException("userId not found in context. Ensure authentication middleware is applied before this handler.");

        String fileId = ctx.pathParam("fileId");

        if (Checks.empty(fileId)) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(ErrorResponse.of("FILE_ID_MISSING", "File ID is required"));
            return;
        }

        var res = shareLinkService.listShareLinksForFile(userId, fileId);

        if (!res.isSuccess()) {

            switch (res.getErrorCode()) {
                case "ACCESS_DENIED" -> ctx.status(HttpStatus.NOT_FOUND)
                        .json(ErrorResponse.of("FILE_NOT_FOUND", "No permission or file not found"));

                default -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(ErrorResponse.of(res.getErrorCode(), res.getMessage()));
            }

            return;
        }

        var links = res.getData().stream()
                .map(link -> new ShareLinkDto(
                        link.shareLinkId(),
                        link.expiry(),
                        link.password()
                ))
                .toList();

        ctx.status(HttpStatus.OK).json(new ListShareLinksResponseDto(
                fileId,
                links
        ));

    }


}
