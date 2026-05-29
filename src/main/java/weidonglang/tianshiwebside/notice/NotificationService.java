package weidonglang.tianshiwebside.notice;

import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.notice.mapper.NoticeMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 通知生成服务。
 *
 * 管理员发布公告或系统生成审核/考试提醒时，通过该服务把一条公告转换成多个用户通知。
 * 这样公告内容和每个用户的已读未读状态可以分开维护。
 */
@Service
public class NotificationService {
    private static final int BATCH_SIZE = 500;

    private final NoticeMapper noticeMapper;

    public NotificationService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public void notifyUsers(Collection<Long> userIds, String title, String content, String category, String relatedType, Long relatedId) {
        Instant now = Instant.now();
        List<Long> distinctUserIds = userIds.stream().distinct().toList();
        for (int start = 0; start < distinctUserIds.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, distinctUserIds.size());
            noticeMapper.insertNotifications(
                    new ArrayList<>(distinctUserIds.subList(start, end)),
                    title,
                    content,
                    category,
                    now,
                    relatedType,
                    relatedId
            );
        }
    }

    public void notifyStudent(Long studentId, String title, String content, String category, String relatedType, Long relatedId) {
        Long userId = noticeMapper.findUserIdByStudentId(studentId);
        if (userId != null) {
            notifyUsers(java.util.List.of(userId), title, content, category, relatedType, relatedId);
        }
    }
}
