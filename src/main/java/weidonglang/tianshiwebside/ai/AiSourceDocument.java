package weidonglang.tianshiwebside.ai;

public record AiSourceDocument(
        String id,
        String title,
        String type,
        String content,
        double score
) {
}
