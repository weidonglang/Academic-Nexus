package weidonglang.tianshiwebside.course.grab;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;

@Service
@ConditionalOnProperty(name = "course-grab.remote.enabled", havingValue = "true")
public class RemoteCourseGrabClient implements CourseGrabPort {
    @Override
    public CourseGrabResult grab(CourseGrabCommand command) {
        throw new BusinessException(
                ErrorCode.INTERNAL_ERROR,
                "Remote course grab service is enabled but no remote client implementation has been configured"
        );
    }
}
