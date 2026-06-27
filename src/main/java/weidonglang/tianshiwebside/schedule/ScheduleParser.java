package weidonglang.tianshiwebside.schedule;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class ScheduleParser {
    private static final Map<String, Integer> DAY_MAP = Map.ofEntries(
            Map.entry("周一", 1), Map.entry("星期一", 1),
            Map.entry("周二", 2), Map.entry("星期二", 2),
            Map.entry("周三", 3), Map.entry("星期三", 3),
            Map.entry("周四", 4), Map.entry("星期四", 4),
            Map.entry("周五", 5), Map.entry("星期五", 5),
            Map.entry("周六", 6), Map.entry("星期六", 6),
            Map.entry("周日", 7), Map.entry("星期日", 7),
            Map.entry("周天", 7), Map.entry("星期天", 7)
    );
    private static final Pattern SLOT_PATTERN = Pattern.compile("(?:第)?\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})\\s*(?:节)?");

    public ParsedSchedule parse(String scheduleText) {
        String text = scheduleText == null ? "" : scheduleText.trim();
        Integer day = null;
        for (var entry : DAY_MAP.entrySet()) {
            if (text.contains(entry.getKey())) {
                day = entry.getValue();
                break;
            }
        }
        var matcher = SLOT_PATTERN.matcher(text);
        if (day == null || !matcher.find()) {
            return new ParsedSchedule(0, "UNKNOWN", false, "时间格式异常");
        }
        return new ParsedSchedule(day, matcher.group(1) + "-" + matcher.group(2), true, "");
    }

    public record ParsedSchedule(int dayOfWeek, String slot, boolean valid, String message) {
    }
}
