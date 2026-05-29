package weidonglang.tianshiwebside.common.api;

import java.util.List;

/**
 * 通用分页返回对象。
 *
 * 管理端用户列表、教师成绩录入、教学班管理等数据量较大的页面都使用分页返回，
 * 避免一次性把上万条压测账号或选课记录全部传给浏览器导致页面卡顿。
 */
public record PageResponse<T>(
        List<T> records,
        int page,
        int size,
        long total
) {
}
