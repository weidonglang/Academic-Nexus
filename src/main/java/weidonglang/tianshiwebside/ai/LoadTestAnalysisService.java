package weidonglang.tianshiwebside.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoadTestAnalysisService {
    private final ObjectMapper objectMapper;
    private final AiRemoteClient remoteClient;
    private final AiCallLogService callLogService;
    private final Path reportDir;

    public LoadTestAnalysisService(
            AiRemoteClient remoteClient,
            AiCallLogService callLogService,
            @Value("${load-test.report-dir:reports}") String reportDir
    ) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.remoteClient = remoteClient;
        this.callLogService = callLogService;
        this.reportDir = Path.of(reportDir).toAbsolutePath().normalize();
    }

    public LoadTestAnalysisResponse analyze(String jsonName, Principal principal) {
        long start = System.nanoTime();
        try {
            JsonNode report = readReport(jsonName);
            LoadTestAnalysisResponse response = remoteClient.analyzeLoadTest(report)
                    .orElseGet(() -> localAnalyze(report));
            callLogService.record(principal, "LOAD_TEST_ANALYSIS", jsonName, response.serviceMode(), elapsedMillis(start), true, null);
            return response;
        } catch (RuntimeException ex) {
            callLogService.record(principal, "LOAD_TEST_ANALYSIS", jsonName, "local-analysis", elapsedMillis(start), false, ex.getMessage());
            throw ex;
        }
    }

    private JsonNode readReport(String jsonName) {
        if (jsonName == null || !jsonName.endsWith(".json") || jsonName.contains("/") || jsonName.contains("\\")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报告文件名不合法");
        }
        Path file = reportDir.resolve(jsonName).normalize();
        if (!file.startsWith(reportDir) || !Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "压测报告不存在");
        }
        try {
            return objectMapper.readTree(file.toFile());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "压测报告读取失败");
        }
    }

    private LoadTestAnalysisResponse localAnalyze(JsonNode report) {
        JsonNode summary = report.path("summary");
        JsonNode redis = report.path("redis");
        int requestCount = summary.path("requestCount").asInt(0);
        int successCount = summary.path("byStatus").path("SUCCESS").asInt(0);
        int fullCount = summary.path("byStatus").path("FULL").asInt(0);
        double avgLatency = summary.path("avgLatency").asDouble(0);
        double p95 = summary.path("p95").asDouble(0);
        double throughput = summary.path("throughput").asDouble(0);
        boolean redisReachable = redis.path("reachable").asBoolean(false);
        double successRate = requestCount == 0 ? 0 : successCount * 100.0 / requestCount;
        List<String> bottlenecks = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        if (!redisReachable) {
            bottlenecks.add("Redis 未连接或不可用，抢课请求会更多依赖数据库兜底。");
            suggestions.add("压测前先预热 Redis 库存，并确认 Redis PING 正常。");
        }
        if (p95 > 1000) {
            bottlenecks.add("P95 响应时间偏高，说明高并发下存在尾部延迟。");
            suggestions.add("检查数据库连接池、Redis 连接和抢课写入事务耗时。");
        }
        if (fullCount > 0) {
            bottlenecks.add("存在满员返回，说明容量保护已参与请求裁决。");
        }
        if (successRate < 80 && fullCount == 0) {
            bottlenecks.add("成功率偏低且不是容量满导致，需排查接口错误或账号准备问题。");
        }
        if (bottlenecks.isEmpty()) {
            bottlenecks.add("未发现明显瓶颈，吞吐和延迟处于可接受范围。");
        }
        if (suggestions.isEmpty()) {
            suggestions.add("继续观察 Redis 库存、数据库已选人数和教学班容量是否一致。");
        }
        String risk = (!redisReachable || p95 > 1000 || successRate < 80) ? "中风险" : "低风险";
        String conclusion = "本次压测共 " + requestCount + " 次请求，成功 " + successCount
                + " 次，满员 " + fullCount + " 次，吞吐 " + String.format("%.2f", throughput)
                + "/s，平均响应 " + String.format("%.1f", avgLatency)
                + "ms，P95 " + String.format("%.1f", p95) + "ms。";
        return new LoadTestAnalysisResponse(conclusion, bottlenecks, suggestions, risk, "local-analysis");
    }

    private long elapsedMillis(long startNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
