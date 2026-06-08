package weidonglang.tianshiwebside.notice;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.api.PageResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.notice.mapper.NoticeMapper;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;

/**
 * 学生端/教师端通知公告查询接口。
 *
 * 首页公告、个人通知、已读未读状态都从这里读取。管理端发布公告后，
 * NotificationService 会生成通知记录，前端首页再展示给对应角色用户。
 */
@RestController
public class NoticeController {
    private final NoticeMapper noticeMapper;
    private final QueryCacheService queryCacheService;

    public NoticeController(NoticeMapper noticeMapper, QueryCacheService queryCacheService) {
        this.noticeMapper = noticeMapper;
        this.queryCacheService = queryCacheService;
    }

    @GetMapping("/api/notices/home")
    public ApiResponse<PageResponse<NoticeMapper.NoticeRow>> home(@RequestParam(required = false) String category,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "6") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 20);
        return ApiResponse.success(queryCacheService.get(
                "query:notices:home:" + normalize(category) + ":" + safePage + ":" + safeSize,
                Duration.ofSeconds(30),
                new TypeReference<PageResponse<NoticeMapper.NoticeRow>>() {
                },
                () -> new PageResponse<>(
                        noticeMapper.findNotices(category, safeSize, (safePage - 1) * safeSize),
                        safePage,
                        safeSize,
                        noticeMapper.countNotices(category)
                )
        ));
    }

    @GetMapping("/api/notifications/me")
    public ApiResponse<PageResponse<NoticeMapper.NotificationRow>> notifications(Principal principal,
                                                                                 @RequestParam(required = false) Boolean read,
                                                                                 @RequestParam(defaultValue = "1") int page,
                                                                                 @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String username = principal.getName();
        return ApiResponse.success(queryCacheService.get(
                "query:notifications:" + username + ":" + (read == null ? "all" : read) + ":" + safePage + ":" + safeSize,
                Duration.ofSeconds(20),
                new TypeReference<PageResponse<NoticeMapper.NotificationRow>>() {
                },
                () -> new PageResponse<>(
                        noticeMapper.findNotifications(username, read, safeSize, (safePage - 1) * safeSize),
                        safePage,
                        safeSize,
                        noticeMapper.countNotifications(username, read)
                )
        ));
    }

    @PutMapping("/api/notifications/me/{id}/read")
    public ApiResponse<Void> markRead(Principal principal, @PathVariable Long id) {
        noticeMapper.markRead(id, principal.getName(), Instant.now());
        queryCacheService.evictByPrefix("query:notifications:" + principal.getName());
        queryCacheService.evictByPrefix("query:dashboard:" + principal.getName());
        return ApiResponse.success();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "all" : value.trim();
    }
}
