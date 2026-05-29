package weidonglang.tianshiwebside.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;

import java.security.Principal;

/**
 * 首页仪表盘接口。
 *
 * 登录后前端 Dashboard 页面会调用这里，聚合当前学期课程数量、待评价数量、考试安排、
 * 已获学分和近期事件。它的作用是把多个业务模块的摘要信息集中到首页展示。
 */
@RestController
public class DashboardController {
    private static final String CURRENT_TERM = "2025-2026-2";

    private final DashboardMapper dashboardMapper;

    public DashboardController(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    /**
     * 查询当前登录用户首页概览。
     *
     * Principal 来自 Spring Security，保证只读取当前账号相关的数据，
     * 学生、教师、管理员进入首页时都可以复用这一接口。
     */
    @GetMapping("/api/dashboard/me")
    public ApiResponse<DashboardOverview> overview(Principal principal) {
        String username = principal.getName();
        DashboardOverview overview = new DashboardOverview(
                dashboardMapper.countTermOfferings(CURRENT_TERM),
                dashboardMapper.countPendingEvaluations(username),
                dashboardMapper.countUpcomingExams(username),
                dashboardMapper.sumEarnedCredits(username),
                dashboardMapper.findRecentEvents(username)
        );
        return ApiResponse.success(overview);
    }
}
