package weidonglang.tianshiwebside.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import weidonglang.tianshiwebside.common.api.ApiResponse;

@Controller
public class SpaErrorController implements ErrorController {
    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Object rawUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String uri = rawUri == null ? "" : rawUri.toString();
        if (!uri.startsWith("/api") && !uri.startsWith("/actuator")) {
            return "forward:/index.html";
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("404", "Resource not found", null));
    }
}
