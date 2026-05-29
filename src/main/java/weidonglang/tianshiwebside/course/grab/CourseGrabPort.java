package weidonglang.tianshiwebside.course.grab;

public interface CourseGrabPort {
    CourseGrabResult grab(CourseGrabCommand command);
}
