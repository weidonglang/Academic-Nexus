package weidonglang.tianshiwebside.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.academic.TermService;

import java.security.Principal;
import java.time.Duration;

/**
 * 首页仪表盘接口。
 *
 * 登录后前端 Dashboard 页面会调用这里，聚合当前学期课程数量、待评价数量、考试安排、
 * 已获学分和近期事件。它的作用是把多个业务模块的摘要信息集中到首页展示。
 */
@RestController
public class DashboardController {
    private final DashboardMapper dashboardMapper;
    private final QueryCacheService queryCacheService;
    private final TermService termService;

    public DashboardController(DashboardMapper dashboardMapper, QueryCacheService queryCacheService, TermService termService) {
        this.dashboardMapper = dashboardMapper;
        this.queryCacheService = queryCacheService;
        this.termService = termService;
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
        String currentTerm = termService.resolveTerm(null);
        return ApiResponse.success(queryCacheService.get(
                "query:dashboard:" + username + ":" + currentTerm,
                Duration.ofSeconds(20),
                new TypeReference<DashboardOverview>() {
                },
                () -> new DashboardOverview(
                        dashboardMapper.countTermOfferings(currentTerm),
                        dashboardMapper.countPendingEvaluations(username),
                        dashboardMapper.countUpcomingExams(username),
                        dashboardMapper.sumEarnedCredits(username),
                        dashboardMapper.findRecentEvents(username)
                )
        ));
    }
}
