package com.github.marcelektro.simplefilehost.service.sharing;

import com.github.marcelektro.simplefilehost.service.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;

public interface ShareLinkService {

    ServiceResult<String> createShareLink(String userId, String fileId, String password, LocalDateTime expiry) throws Exception;

    ServiceResult<Void> updateShareLink(String userId, String linkId, String newPassword, LocalDateTime newExpiry) throws Exception;

    ServiceResult<Void> deleteShareLink(String userId, String linkId) throws Exception;

    ServiceResult<ShareLinkValidationResult> validateLink(String linkId, String password) throws Exception;

    ServiceResult<List<ShareLinkInfo>> listShareLinksForFile(String userId, String fileId) throws Exception;


    record ShareLinkValidationResult(
            String fileId,
            String originalFilename,
            long fileSize,
            boolean passwordProtected,
            boolean validPassword,
            @Nullable LocalDateTime expiry,
            boolean hasExpired
    ) { }

    record ShareLinkInfo(
            String shareLinkId,
            @Nullable LocalDateTime expiry,
            @Nullable String password
    ) { }
}
