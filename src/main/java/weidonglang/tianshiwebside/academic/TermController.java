package weidonglang.tianshiwebside.academic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;

@RestController
public class TermController {
    private final TermService termService;

    public TermController(TermService termService) {
        this.termService = termService;
    }

    @GetMapping("/api/academic/current-term")
    public ApiResponse<TermService.CurrentTerm> currentTerm() {
        return ApiResponse.success(termService.currentTerm());
    }
}
