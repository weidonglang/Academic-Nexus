package weidonglang.tianshiwebside.notice.mapper;

import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;

@Mapper
public interface NoticeMapper {
    @Select("""
            select id, title, content, category, pinned, published_at as published_at, publisher
            from notice
            where (#{category} is null or category = #{category})
            order by pinned desc, published_at desc
            limit #{size} offset #{offset}
            """)
    List<NoticeRow> findNotices(@Param("category") String category, @Param("size") int size, @Param("offset") int offset);

    @Select("select count(*) from notice where (#{category} is null or category = #{category})")
    long countNotices(@Param("category") String category);

    @Select("""
            select
              n.id,
              n.title,
              n.category,
              n.published_at as published_at,
              n.publisher,
              count(un.id) as target_total,
              coalesce(sum(case when un.read_flag = true then 1 else 0 end), 0) as read_count,
              coalesce(sum(case when un.read_flag = false then 1 else 0 end), 0) as unread_count
            from notice n
            left join user_notification un
              on un.related_type = 'NOTICE'
             and un.related_id = n.id
            group by n.id, n.title, n.category, n.published_at, n.publisher
            order by n.published_at desc
            """)
    List<NoticeStatRow> findNoticeStats();

    @Insert("""
            insert into notice (title, content, category, pinned, published_at, publisher)
            values (#{title}, #{content}, #{category}, #{pinned}, #{publishedAt}, #{publisher})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotice(NoticeCommand command);

    @Select("select id from sys_user")
    List<Long> findAllUserIds();

    @Select("""
            select u.id
            from sys_user u
            join sys_user_role ur on ur.user_id = u.id
            join sys_role r on r.id = ur.role_id
            where r.code = #{roleCode}
            """)
    List<Long> findUserIdsByRoleCode(@Param("roleCode") String roleCode);

    @Select("""
            select u.id
            from student s
            join sys_user u on u.id = s.user_id
            where s.id = #{studentId}
            """)
    Long findUserIdByStudentId(@Param("studentId") Long studentId);

    @Insert("""
            insert into user_notification (user_id, title, content, category, read_flag, created_at, related_type, related_id)
            values (#{userId}, #{title}, #{content}, #{category}, false, #{createdAt}, #{relatedType}, #{relatedId})
            """)
    int insertNotification(@Param("userId") Long userId, @Param("title") String title,
                           @Param("content") String content, @Param("category") String category,
                           @Param("createdAt") Instant createdAt, @Param("relatedType") String relatedType,
                           @Param("relatedId") Long relatedId);

    /**
     * 批量生成站内通知。
     *
     * 公告目标为空时会发送给全部用户，本项目包含 1 万多个压测账号，如果逐条 insert，
     * 页面会长时间等待。这里按批次使用一条 SQL 插入多条通知，避免“发送全部用户”看起来卡死。
     */
    @Insert("""
            <script>
            insert into user_notification (user_id, title, content, category, read_flag, created_at, related_type, related_id)
            values
            <foreach collection="userIds" item="userId" separator=",">
              (#{userId}, #{title}, #{content}, #{category}, false, #{createdAt}, #{relatedType}, #{relatedId})
            </foreach>
            </script>
            """)
    int insertNotifications(@Param("userIds") List<Long> userIds, @Param("title") String title,
                            @Param("content") String content, @Param("category") String category,
                            @Param("createdAt") Instant createdAt, @Param("relatedType") String relatedType,
                            @Param("relatedId") Long relatedId);

    @Select("""
            select n.id, n.title, n.content, n.category, n.read_flag as read_flag,
                   n.created_at as created_at, n.read_at as read_at, n.related_type as related_type, n.related_id as related_id
            from user_notification n
            join sys_user u on u.id = n.user_id
            where u.username = #{username}
              and (#{readFlag} is null or n.read_flag = #{readFlag})
            order by n.created_at desc
            limit #{size} offset #{offset}
            """)
    List<NotificationRow> findNotifications(@Param("username") String username, @Param("readFlag") Boolean readFlag,
                                            @Param("size") int size, @Param("offset") int offset);

    @Select("""
            select count(*)
            from user_notification n
            join sys_user u on u.id = n.user_id
            where u.username = #{username}
              and (#{readFlag} is null or n.read_flag = #{readFlag})
            """)
    long countNotifications(@Param("username") String username, @Param("readFlag") Boolean readFlag);

    @Update("""
            update user_notification
            set read_flag = true,
                read_at = #{readAt}
            where id = #{id}
              and user_id = (select id from sys_user where username = #{username})
            """)
    int markRead(@Param("id") Long id, @Param("username") String username, @Param("readAt") Instant readAt);

    record NoticeRow(Long id, String title, String content, String category, Boolean pinned, Instant publishedAt, String publisher) {
    }

    record NotificationRow(Long id, String title, String content, String category, Boolean readFlag, Instant createdAt,
                           Instant readAt, String relatedType, Long relatedId) {
    }

    record NoticeStatRow(Long id, String title, String category, Instant publishedAt, String publisher,
                         Long targetTotal, Long readCount, Long unreadCount) {
    }

    class NoticeCommand {
        private Long id;
        private final String title;
        private final String content;
        private final String category;
        private final Boolean pinned;
        private final Instant publishedAt;
        private final String publisher;

        public NoticeCommand(String title, String content, String category, Boolean pinned, Instant publishedAt, String publisher) {
            this.title = title;
            this.content = content;
            this.category = category;
            this.pinned = pinned;
            this.publishedAt = publishedAt;
            this.publisher = publisher;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getCategory() { return category; }
        public Boolean getPinned() { return pinned; }
        public Instant getPublishedAt() { return publishedAt; }
        public String getPublisher() { return publisher; }
    }
}
