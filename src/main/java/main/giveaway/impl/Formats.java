package main.giveaway.impl;

import java.time.format.DateTimeFormatter;

public interface Formats {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    String ISO_TIME_REGEX = "^\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2}$"; //2021.11.16 16:00

    String TIME_REGEX = "(\\d{4}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2})|(\\d{1,2}[smhdсмдч]|\\s)+";
}