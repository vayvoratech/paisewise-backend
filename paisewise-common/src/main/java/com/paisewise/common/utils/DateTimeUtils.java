package com.paisewise.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimeUtils {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public static LocalDateTime nowIST() {
        return LocalDateTime.now(IST_ZONE);
    }

    public static ZonedDateTime currentZonedDateTimeIST() {
        return ZonedDateTime.now(IST_ZONE);
    }
}