package com.anderhurtado.spigot.mobmoney.util;

import com.anderhurtado.spigot.mobmoney.MobMoney;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private TimeUtils(){}

    private static final Pattern PATTERN = Pattern.compile("(\\d+)([hmsd])");
    public static long convertToTime(String time) {
        Matcher m = PATTERN.matcher(time);
        long totalMillis=0;
        while (m.find()){
            final int duration=Integer.parseInt(m.group(1));
            final TimeUnit interval=toTimeUnit(m.group(2));
            if(interval == null) throw new RuntimeException("Invalid format");
            final long l=interval.toMillis(duration);
            totalMillis=totalMillis+l;
        }
        return totalMillis;
    }

    public static String convertToString(long time) {
        StringBuilder sb = new StringBuilder();

        TimeUnit[] values = TimeUnit.values();
        for(int i=values.length-1; i>=0; i--) {
            if(time / values[i].time >= 1) {
                sb.append((int)Math.floor((float) time / values[i].time));
                sb.append(values[i].timeDisplay);
                time %= values[i].time;
            }
        }
        String result = sb.toString();
        if(result.isEmpty()) result = "0s";
        return result;
    }

    private static TimeUnit toTimeUnit(String c){
        switch (c) {
            case "s": return TimeUnit.SECONDS;
            case "m": return TimeUnit.MINUTES;
            case "h": return TimeUnit.HOURS;
            case "d": return TimeUnit.DAYS;
            default: return null;
        }
    }

    private static String toString(TimeUnit tu) {
        switch (tu) {
            case SECONDS: return "s";
            case MINUTES: return "m";
            case HOURS: return "h";
            case DAYS: return "d";
            default: return null;
        }
    }

    private enum TimeUnit {
        SECONDS(1_000,"s"), MINUTES(60_000,"m"), HOURS(3600_000,"h"), DAYS(86_400_000,"d");

        public long time;
        public String timeDisplay;

        TimeUnit(long time, String timeDisplay) {
            this.time = time;
            this.timeDisplay = MobMoney.msg.getOrDefault("Time."+name().toLowerCase(), timeDisplay);
        }

        private long toMillis(int duration) {
            return duration * time;
        }

    }

}
