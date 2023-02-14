package main.giveaway.impl;

public interface Seconds {

    static long getSeconds(String time) {
        String[] splitTime = time.split("\\s+");
        long seconds = 0;

        for (String s : splitTime) {
            long localTime = Long.parseLong(s.substring(0, s.length() - 1));
            String symbol = s.substring(s.length() - 1);

            switch (symbol) {
                case "m", "м" -> seconds += localTime * 60;
                case "h", "ч" -> seconds += localTime * 3600;
                case "d", "д" -> seconds += localTime * 86400;
                case "s", "с" -> seconds += localTime;
            }
        }

        return seconds;
    }
}
