package weidonglang.tianshiwebside.file;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 管理端文件记录接口。
 *
 * 用于展示和下载学籍异动申请材料等附件。当前项目保留了文件管理基础结构，
 * 后续可以继续扩展 Excel 导入导出、材料归档和文件审计功能。
 */
@RestController
@RequestMapping("/api/admin/files")
@PreAuthorize("hasAuthority('STATUS_REVIEW')")
public class AdminFileController {
    private final StatusChangeAttachmentMapper mapper;

    public AdminFileController(StatusChangeAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 查询学生申请材料列表。
     *
     * 管理员审核学籍异动时，可以先在这里查看学生上传过哪些证明材料。
     */
    @GetMapping
    public ApiResponse<List<StatusChangeAttachmentMapper.AdminAttachmentRow>> files() {
        return ApiResponse.success(mapper.findAdminAttachments());
    }

    /**
     * 下载指定附件。
     *
     * 下载前会校验附件记录是否存在，并从 uploads 目录读取实际文件。
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long attachmentId) {
        StatusChangeAttachmentMapper.AttachmentRow row = requireAttachment(attachmentId);
        Path path = Paths.get(row.storedPath()).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在或已被移动");
        }
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

    @DeleteMapping("/{attachmentId}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long attachmentId) {
        StatusChangeAttachmentMapper.AttachmentRow row = requireAttachment(attachmentId);
        mapper.deleteById(attachmentId);
        Path path = Paths.get(row.storedPath()).toAbsolutePath().normalize();
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件记录已删除，但物理文件清理失败");
        }
        return ApiResponse.success();
    }

    private StatusChangeAttachmentMapper.AttachmentRow requireAttachment(Long attachmentId) {
        StatusChangeAttachmentMapper.AttachmentRow row = mapper.findById(attachmentId);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "附件不存在");
        }
        return row;
    }
}
