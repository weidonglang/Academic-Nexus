package weidonglang.tianshiwebside.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = normalize(request.getHeader(TraceIdHolder.TRACE_ID_HEADER));
        request.setAttribute(TraceIdHolder.TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader(TraceIdHolder.TRACE_ID_HEADER, traceId);
        TraceIdHolder.set(traceId);
        MDC.put("traceId", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            TraceIdHolder.clear();
        }
    }

    private String normalize(String incomingTraceId) {
        if (incomingTraceId == null || incomingTraceId.isBlank() || incomingTraceId.length() > 80) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return incomingTraceId.trim();
    }
}
