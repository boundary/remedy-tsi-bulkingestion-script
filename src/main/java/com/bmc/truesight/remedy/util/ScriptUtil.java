package com.bmc.truesight.remedy.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ScriptUtil {

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    public static String dateToString(Date date) {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return DATE_FORMAT.format(date);
    }
}
