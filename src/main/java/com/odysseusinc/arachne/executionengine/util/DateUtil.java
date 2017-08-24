package com.odysseusinc.arachne.executionengine.util;

import java.util.Date;

public class DateUtil {

    public static String defaultFormat(String format, Date date) {

        return date != null ? String.format(format, date) : "";
    }
}
