package weidonglang.tianshiwebside;

import org.junit.jupiter.api.Test;
import weidonglang.tianshiwebside.schedule.ScheduleParser;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleParsingRegressionTests {
    private final ScheduleParser parser = new ScheduleParser();

    @Test
    void parsesCommonChineseScheduleFormats() {
        assertThat(parser.parse("周一 1-2节").dayOfWeek()).isEqualTo(1);
        assertThat(parser.parse("星期三 第3-4节 单周").slot()).isEqualTo("3-4");
        assertThat(parser.parse("周五 9-10").dayOfWeek()).isEqualTo(5);
    }

    @Test
    void unknownScheduleDoesNotDefaultToMondayMorning() {
        ScheduleParser.ParsedSchedule parsed = parser.parse("时间待定");

        assertThat(parsed.valid()).isFalse();
        assertThat(parsed.dayOfWeek()).isZero();
        assertThat(parsed.slot()).isEqualTo("UNKNOWN");
        assertThat(parsed.message()).contains("时间格式异常");
    }
}
