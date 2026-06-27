package weidonglang.tianshiwebside.ai;

public record AiChatResponse(
        String answer,
        String serviceMode,
        String modelName,
        boolean searchUsed,
        java.util.List<AiSearchDtos.SearchResult> searchSources,
        String searchMessage
) {
}
