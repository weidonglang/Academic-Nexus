package weidonglang.tianshiwebside.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/login",
            "/dashboard",
            "/student/{*path}",
            "/registration/{*path}",
            "/course/{*path}",
            "/schedule/{*path}",
            "/classroom/{*path}",
            "/information/{*path}",
            "/grade/{*path}",
            "/exam/{*path}",
            "/evaluation",
            "/teacher/{*path}",
            "/admin/{*path}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
