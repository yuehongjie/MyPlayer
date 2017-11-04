package com.yu.myplayer;

import java.util.Formatter;

/**
 * Created by Administrator on 2017-11-4.
 *
 */

public class MediaUtils {

    public static String formatTime(long timeMs) {
        if (timeMs <= 0) {
            return "00:00";
        }

        long totalSeconds = timeMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = totalSeconds / 60 % 60;
        long hours = totalSeconds / 3600;

        Formatter formatter = new Formatter();
        return hours > 0
                ? formatter.format("%d:%02d:%02d", new Object[]{hours, minutes, seconds}).toString()
                : formatter.format("%02d:%02d", new Object[]{minutes, seconds}).toString();
    }
}
