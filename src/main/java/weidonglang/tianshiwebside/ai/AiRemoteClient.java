package weidonglang.tianshiwebside.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AiRemoteClient {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String baseUrl;

    public AiRemoteClient(@Value("${app.ai-service.base-url:http://localhost:8090}") String baseUrl) {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        this.baseUrl = stripTrailingSlash(baseUrl);
    }

    public Optional<AiAssistantResponse> ask(String question, List<AiSourceDocument> documents) {
        try {
            Map<String, Object> payload = Map.of(
                    "question", question,
                    "documents", documents
            );
            String response = post("/internal/ai/rag/answer", payload, Duration.ofSeconds(90));
            return Optional.of(objectMapper.readValue(response, AiAssistantResponse.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<AiChatResponse> chat(String message) {
        try {
            String response = post("/internal/ai/chat", Map.of("message", message), Duration.ofSeconds(90));
            return Optional.of(objectMapper.readValue(response, AiChatResponse.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<LoadTestAnalysisResponse> analyzeLoadTest(Object report) {
        try {
            String response = post("/internal/ai/load-test/analyze", Map.of("report", report), Duration.ofSeconds(180));
            return Optional.of(objectMapper.readValue(response, LoadTestAnalysisResponse.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public AiServiceStatusResponse status() {
        long start = System.nanoTime();
        try {
            String response = get("/internal/ai/status");
            Map<String, Object> raw = objectMapper.readValue(response, new TypeReference<>() {
            });
            return new AiServiceStatusResponse(
                    true,
                    booleanValue(raw.get("ollamaEnabled")),
                    booleanValue(raw.get("ollamaReachable")),
                    String.valueOf(raw.getOrDefault("chatModel", "")),
                    String.valueOf(raw.getOrDefault("sqlModel", "")),
                    booleanValue(raw.get("ollamaReachable")) ? "AI 模式" : "本地兜底模式",
                    elapsedMillis(start),
                    String.valueOf(raw.getOrDefault("lastError", "")),
                    Instant.now()
            );
        } catch (Exception ex) {
            return new AiServiceStatusResponse(
                    false,
                    false,
                    false,
                    "",
                    "",
                    "主系统本地兜底模式",
                    elapsedMillis(start),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    Instant.now()
            );
        }
    }

    public Optional<NaturalSqlGenerateResponse> generateSql(String question, List<SqlSchemaService.TableSchema> schemas) {
        try {
            Map<String, Object> payload = Map.of(
                    "question", question,
                    "schemas", schemas
            );
            String response = post("/internal/ai/sql/generate", payload, Duration.ofSeconds(90));
            Map<String, Object> raw = objectMapper.readValue(response, new TypeReference<>() {
            });
            String sql = String.valueOf(raw.getOrDefault("sql", ""));
            String explanation = String.valueOf(raw.getOrDefault("explanation", ""));
            @SuppressWarnings("unchecked")
            List<String> warnings = raw.get("warnings") instanceof List<?> list
                    ? list.stream().map(String::valueOf).toList()
                    : List.of();
            return Optional.of(new NaturalSqlGenerateResponse(
                    sql,
                    explanation,
                    warnings,
                    schemas.stream().map(SqlSchemaService.TableSchema::tableName).toList(),
                    "ai-service"
            ));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String post(String path, Object payload) throws Exception {
        return post(path, payload, Duration.ofSeconds(20));
    }

    private String post(String path, Object payload, Duration timeout) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("ai-service status " + response.statusCode());
        }
        return response.body();
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("ai-service status " + response.statusCode());
        }
        return response.body();
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8090";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
