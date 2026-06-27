package weidonglang.tianshiwebside.ai;

import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.governance.ContentModerationService;

import java.security.Principal;

@Service
public class AiChatService {
    private final AiRemoteClient remoteClient;
    private final AiCallLogService callLogService;
    private final AiModelRegistryService modelRegistryService;
    private final AiSearchService searchService;
    private final ContentModerationService moderationService;

    public AiChatService(
            AiRemoteClient remoteClient,
            AiCallLogService callLogService,
            AiModelRegistryService modelRegistryService,
            AiSearchService searchService,
            ContentModerationService moderationService
    ) {
        this.remoteClient = remoteClient;
        this.callLogService = callLogService;
        this.modelRegistryService = modelRegistryService;
        this.searchService = searchService;
        this.moderationService = moderationService;
    }

    public AiChatResponse chat(String message, Principal principal) {
        return chat(message, principal, null, null);
    }

    public AiChatResponse chat(String message, Principal principal, Long modelId, Long sessionId) {
        long start = System.nanoTime();
        moderationService.checkConfigured("AI_INPUT", message, operator(principal));
        AiModelRecord selectedModel = modelRegistryService.requireEnabledChatModel(modelId);
        AiSearchDtos.SearchTestResponse search = maybeSearch(message, principal);
        return remoteClient.chat(message, selectedModel.id(), selectedModel.modelName(), sessionId)
                .map(response -> {
                    String actualModelName = response.actualModelName() == null || response.actualModelName().isBlank()
                            ? response.modelName()
                            : response.actualModelName();
                    String modelName = actualModelName == null || actualModelName.isBlank()
                            ? selectedModel.modelName()
                            : actualModelName;
                    String answer = appendSearchNotice(response.answer(), search);
                    moderationService.checkConfigured("AI_OUTPUT", answer, operator(principal));
                    String warning = firstNonBlank(response.fallbackReason(), search.message());
                    callLogService.record(principal, "CHAT", message, modelName, response.serviceMode(),
                            elapsedMillis(start), true, warning, sessionId, selectedModel.id(),
                            selectedModel.modelName(), modelName, response.fallbackReason());
                    return new AiChatResponse(
                            answer,
                            response.serviceMode(),
                            modelName,
                            search.searchUsed(),
                            search.results(),
                            search.message(),
                            selectedModel.id(),
                            selectedModel.modelName(),
                            modelName,
                            response.fallback(),
                            response.fallbackReason()
                    );
                })
                .orElseGet(() -> {
                    String answer = "AI 聊天服务暂不可用，当前为本地兜底模式。这个聊天入口不作为教务依据；涉及教务规则请使用智能教务助手。";
                    String moderatedAnswer = appendSearchNotice(answer, search);
                    moderationService.checkConfigured("AI_OUTPUT", moderatedAnswer, operator(principal));
                    String fallbackReason = "ai-service unavailable";
                    callLogService.record(principal, "CHAT_FALLBACK", message, selectedModel.modelName(), "local-fallback",
                            elapsedMillis(start), true, fallbackReason + "; " + search.message(), sessionId, selectedModel.id(),
                            selectedModel.modelName(), selectedModel.modelName(), fallbackReason);
                    return new AiChatResponse(moderatedAnswer, "local-fallback", selectedModel.modelName(),
                            search.searchUsed(), search.results(), search.message(), selectedModel.id(),
                            selectedModel.modelName(), selectedModel.modelName(), true, fallbackReason);
                });
    }

    private AiSearchDtos.SearchTestResponse maybeSearch(String message, Principal principal) {
        if (!searchService.shouldSearch(message)) {
            return new AiSearchDtos.SearchTestResponse(true, false, "", "未触发联网搜索", java.util.List.of(), null);
        }
        return searchService.search(principal == null ? "anonymous" : principal.getName(), "CHAT", message);
    }

    private String appendSearchNotice(String answer, AiSearchDtos.SearchTestResponse search) {
        if (!search.searchUsed() || search.results().isEmpty()) {
            return answer;
        }
        StringBuilder builder = new StringBuilder(answer).append("\n\n联网搜索参考：");
        for (int i = 0; i < Math.min(3, search.results().size()); i++) {
            AiSearchDtos.SearchResult result = search.results().get(i);
            builder.append("\n").append(i + 1).append(". ").append(result.title()).append(" - ").append(result.link());
        }
        return builder.toString();
    }

    private long elapsedMillis(long startNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private String operator(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
