package com.example.liferpg.logic;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Date helpers. All "day keys" are ISO local dates (yyyy-MM-dd). */
public final class DateUtils {
    private DateUtils() { }

    public static String todayKey() {
        return LocalDate.now().toString();
    }

    public static String keyForMillis(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    public static long startOfDayMillis(int year, int month1to12, int day) {
        return LocalDate.of(year, month1to12, day).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    public static long daysBetween(String fromKey, String toKey) {
        return ChronoUnit.DAYS.between(LocalDate.parse(fromKey), LocalDate.parse(toKey));
    }

    public static String plusDays(String key, int days) {
        return LocalDate.parse(key).plusDays(days).toString();
    }

    public static String mondayOfWeek(String key) {
        return LocalDate.parse(key).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
    }

    public static String weekdayShort(String key) {
        return LocalDate.parse(key).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }

    public static String formatDay(long millis) {
        return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    public static String formatDateTime(long millis) {
        return DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())
                .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }
}
