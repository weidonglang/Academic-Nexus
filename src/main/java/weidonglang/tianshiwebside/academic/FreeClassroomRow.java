package weidonglang.tianshiwebside.academic;

public record FreeClassroomRow(
        String campus,
        String building,
        String room,
        Integer capacity,
        String roomType,
        String availableSlot
) {
}
