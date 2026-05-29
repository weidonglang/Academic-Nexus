package weidonglang.tianshiwebside.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;

import java.io.IOException;
import java.nio.file.*;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students/me/status-changes/{applicationId}/attachments")
public class StatusChangeAttachmentController {
    private final StatusChangeAttachmentMapper mapper;
    private final Path uploadRoot;

    public StatusChangeAttachmentController(
            StatusChangeAttachmentMapper mapper,
            @Value("${app.upload-root:uploads}") String uploadRoot
    ) {
        this.mapper = mapper;
        this.uploadRoot = Paths.get(uploadRoot).toAbsolutePath().normalize();
    }

    @GetMapping
    public ApiResponse<List<StatusChangeAttachmentMapper.AttachmentRow>> list(@PathVariable Long applicationId) {
        return ApiResponse.success(mapper.findByApplicationId(applicationId));
    }

    @PostMapping
    public ApiResponse<Void> upload(Principal principal, @PathVariable Long applicationId, @RequestParam("file") MultipartFile file) {
        if (mapper.countOwnedApplication(applicationId, principal.getName()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        }
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
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
            return ApiResponse.success();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
    }
}
