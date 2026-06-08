package weidonglang.tianshiwebside.ai;

import org.springframework.stereotype.Service;

import java.security.Principal;

@Service
public class AiChatService {
    private final AiRemoteClient remoteClient;
    private final AiCallLogService callLogService;

    public AiChatService(AiRemoteClient remoteClient, AiCallLogService callLogService) {
        this.remoteClient = remoteClient;
        this.callLogService = callLogService;
    }

    public AiChatResponse chat(String message, Principal principal) {
        long start = System.nanoTime();
        return remoteClient.chat(message)
                .map(response -> {
                    callLogService.record(principal, "CHAT", message, response.serviceMode(), elapsedMillis(start), true, null);
                    return response;
                })
                .orElseGet(() -> {
                    String answer = "AI 聊天服务暂不可用，当前为本地兜底模式。这个聊天入口不作为教务依据；涉及教务规则请使用智能教务助手。";
                    callLogService.record(principal, "CHAT_FALLBACK", message, "local-fallback", elapsedMillis(start), true, "ai-service unavailable");
                    return new AiChatResponse(answer, "local-fallback");
                });
    }

    private long elapsedMillis(long startNanos) {
        return java.time.Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
