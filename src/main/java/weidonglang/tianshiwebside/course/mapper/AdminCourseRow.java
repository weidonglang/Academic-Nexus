package weidonglang.tianshiwebside.course.mapper;

public record AdminCourseRow(
        Long courseId,
        String code,
        String name,
        Integer credit,
        String category
) {
}
