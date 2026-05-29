package weidonglang.tianshiwebside.course.grab;

public record CourseGrabCommand(
        String username,
        Long offeringId,
        String requestId
) {
}
