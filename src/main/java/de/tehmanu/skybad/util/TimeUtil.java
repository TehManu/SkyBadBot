package de.tehmanu.skybad.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author TehManu
 * @since 16.10.2024
 */
public class TimeUtil {

    public static String formatTime(final int time) {
        String formated = "";
        int hours = time / 60;
        int minutes = time % 60;
        if (hours > 0) {
            formated += hours + "h ";
        }
        if (hours > 0 && minutes > 0 || hours == 0) {
            formated += minutes + "min";
        }
        return formated;
    }

    public static boolean isLoginTimeValid() {
        LocalTime start = LocalTime.parse("23:30:01");
        LocalTime now = LocalTime.now();
        LocalTime end = LocalTime.parse("00:00:00");
        return !now.isBefore(start) && !now.isAfter(end);
    }

    public static Date convertToDate(String time) {
        LocalTime localTime = LocalTime.parse(time);
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.now(), localTime);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
