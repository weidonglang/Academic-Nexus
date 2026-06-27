package weidonglang.tianshiwebside.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.audit.AuditLogService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/students/me/status-changes/{applicationId}/attachments")
public class StatusChangeAttachmentController {
    private final StatusChangeAttachmentMapper mapper;
    private final Path uploadRoot;
    private final long maxSizeBytes;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedContentTypes;
    private final AuditLogService auditLogService;

    public StatusChangeAttachmentController(
            StatusChangeAttachmentMapper mapper,
            @Value("${app.upload-root:uploads}") String uploadRoot,
            @Value("${app.upload.max-size-bytes:10485760}") long maxSizeBytes,
            @Value("${app.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}") String allowedExtensions,
            @Value("${app.upload.allowed-content-types:application/pdf,image/jpeg,image/png,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}") String allowedContentTypes,
            AuditLogService auditLogService
    ) {
        this.mapper = mapper;
        this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
        this.allowedExtensions = parseCsv(allowedExtensions);
        this.allowedContentTypes = parseCsv(allowedContentTypes);
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<List<StatusChangeAttachmentMapper.AttachmentRow>> list(Principal principal, @PathVariable Long applicationId) {
        ensureOwned(applicationId, principal);
        return ApiResponse.success(mapper.findByApplicationId(applicationId));
    }

    @PostMapping
    public ApiResponse<Void> upload(Principal principal, @PathVariable Long applicationId, @RequestParam("file") MultipartFile file) {
        ensureOwnedAndUploadable(applicationId, principal);
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        validateFile(file);
        try {
            Files.createDirectories(uploadRoot);
            String original = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
            String filename = UUID.randomUUID() + "-" + original.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path target = uploadRoot.resolve(filename).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件名");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            mapper.insertAttachment(applicationId, original, target.toString(), file.getContentType(), file.getSize(), Instant.now());
            auditLogService.record(principal.getName(), "UPLOAD_STATUS_CHANGE_ATTACHMENT", "STATUS_CHANGE", applicationId,
                    original + " (" + file.getSize() + " bytes)", null);
            return ApiResponse.success();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            Principal principal,
            @PathVariable Long applicationId,
            @PathVariable Long attachmentId
    ) {
        StatusChangeAttachmentMapper.AttachmentRow row = mapper.findOwnedById(applicationId, attachmentId, principal.getName());
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        Path path = resolveStoredPath(row);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或已被移动");
        }
        auditLogService.record(principal.getName(), "DOWNLOAD_STATUS_CHANGE_ATTACHMENT",
                "STATUS_CHANGE_ATTACHMENT", row.id(), row.originalFilename(), null);
        String encodedName = URLEncoder.encode(row.originalFilename(), StandardCharsets.UTF_8).replace("+", "%20");
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (row.contentType() != null && !row.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(row.contentType());
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new PathResource(path));
    }

    private void ensureOwned(Long applicationId, Principal principal) {
        if (principal == null || mapper.countOwnedApplication(applicationId, principal.getName()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        }
    }

    private void ensureOwnedAndUploadable(Long applicationId, Principal principal) {
        ensureOwned(applicationId, principal);
        String status = mapper.findOwnedApplicationStatus(applicationId, principal.getName());
        if (!"SUBMITTED".equals(status) && !"UNDER_REVIEW".equals(status)) {
            throw new BusinessException(ErrorCode.CONFLICT, "申请已审核，不能继续追加材料");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过 " + formatSize(maxSizeBytes));
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String extension = extensionOf(original);
        if (extension.isBlank() || !allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (contentType.isBlank() || !allowedContentTypes.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件内容类型");
        }
    }

    private Set<String> parseCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String formatSize(long bytes) {
        long megabytes = bytes / 1024 / 1024;
        return megabytes > 0 ? megabytes + "MB" : bytes + " bytes";
    }

    private Path resolveStoredPath(StatusChangeAttachmentMapper.AttachmentRow row) {
        Path path = Paths.get(row.storedPath()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
        }
        return path;
    }
}
