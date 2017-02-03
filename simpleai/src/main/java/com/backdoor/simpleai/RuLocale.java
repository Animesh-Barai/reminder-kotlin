package com.backdoor.simpleai;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class RuLocale extends Worker implements WorkerInterface {

    @Override
    protected String[] getWeekdays() {
        return new String[]{"воскресен", "понедельн", "вторн", "среду?", "червер", "пятниц", "суббот"};
    }

    @Override
    public boolean hasCalendar(String input) {
        return input.matches(".*календарь.*");
    }

    @Override
    public String clearCalendar(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (string.matches(".*календарь.*")) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public List<Integer> getWeekDays(String input) {
        int[] array = {0, 0, 0, 0, 0, 0, 0};
        String[] parts = input.split("\\s");
        String[] weekDays = getWeekdays();
        for (String part : parts) {
            for (int i = 0; i < weekDays.length; i++) {
                String day = weekDays[i];
                if (part.matches(".*" + day + ".*")) {
                    array[i] = 1;
                    break;
                }
            }
        }
        List<Integer> list = new ArrayList<>();
        for (int anArray : array) list.add(anArray);
        return list;
    }

    @Override
    public String clearWeekDays(String input) {
        String[] parts = input.split("\\s");
        String[] weekDays = getWeekdays();
        for (String part : parts) {
            for (String day : weekDays) {
                if (part.matches(".*" + day + ".*")) {
                    input = input.replace(part, "");
                    break;
                }
            }
        }
        parts = input.split("\\s");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.matches("в")) sb.append(" ").append(part);
        }
        return sb.toString().trim();
    }

    @Override
    public long getDaysRepeat(String input) {
        String[] parts = input.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (hasDays(part)) {
                int integer;
                try {
                    integer = Integer.parseInt(parts[i - 1]);
                } catch (NumberFormatException e) {
                    integer = 1;
                }
                return integer * DAY;
            }
        }
        return 0;
    }

    @Override
    public String clearDaysRepeat(String input) {
        String[] parts = input.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (hasDays(part)) {
                try {
                    Integer.parseInt(parts[i - 1]);
                    input = input.replace(parts[i - 1], "");
                } catch (NumberFormatException e) {
                }
                input = input.replace(part, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public boolean hasRepeat(String input) {
        return input.matches(".*кажд.*");
    }

    @Override
    public String clearRepeat(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (string.matches(".*кажд.*")) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public boolean hasTomorrow(String input) {
        return input.matches(".*завтра.*");
    }

    @Override
    public String clearTomorrow(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (string.matches(".*завтра.*")) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public String getMessage(String input) {
        String[] parts = input.split("\\s");
        StringBuilder sb = new StringBuilder();
        boolean isStart = false;
        for (String part : parts) {
            if (isStart) sb.append(" ").append(part);
            if (part.matches("текст(ом)?"))
                isStart = true;
        }
        return sb.toString().trim();
    }

    @Override
    public String clearMessage(String input) {
        String[] parts = input.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.matches("текст(ом)?")) {
                try {
                    if (parts[i -1].matches("с")) input = input.replace(parts[i - 1], "");
                } catch (IndexOutOfBoundsException e) {}
                input = input.replace(part, "");
            }
        }
        return input.trim();
    }

    @Override
    public Action getMessageType(String input) {
        if (input.matches(".*сообщение.*")) return Action.MESSAGE;
        if (input.matches(".*письмо?.*")) return Action.MAIL;
        return null;
    }

    @Override
    public String clearMessageType(String input) {
        String[] parts = input.split("\\s");
        for (String part : parts) {
            Action type = getMessageType(part);
            if (type != null) {
                input = input.replace(part, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public Ampm getAmpm(String input) {
        if (input.matches(".*утр(а|ом)?.*")) return Ampm.MORNING;
        if (input.matches(".*вечер.*")) return Ampm.EVENING;
        if (input.matches(".*днем.*")) return Ampm.NOON;
        if (input.matches(".*ночью.*")) return Ampm.NIGHT;
        return null;
    }

    @Override
    public String clearAmpm(String input) {
        String[] parts = input.split("\\s");
        for (String part : parts) {
            if (getAmpm(part) != null) {
                input = input.replace(part, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    protected Date getShortTime(String input) {
        Pattern pattern = Pattern.compile("([01]?\\d|2[0-3])( |:)?(([0-5]?\\d?)?)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String time = matcher.group().trim();
            for (SimpleDateFormat format : Worker.dateTaskFormats) {
                Date date;
                try {
                    date = format.parse(time);
                    if (date != null) return date;
                } catch (NullPointerException | ParseException e) {
                }
            }
        }
        return null;
    }

    @Override
    public String clearTime(String input) {
        String[] parts = input.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (hasHours(part) != -1) {
                int index = hasHours(part);
                input = input.replace(part, "");
                try {
                    Integer.parseInt(parts[i - index]);
                    input = input.replace(parts[i - index], "");
                } catch (NumberFormatException e) {
                }
            }
            if (hasMinutes(part) != -1) {
                int index = hasMinutes(part);
                try {
                    Integer.parseInt(parts[i - index]);
                    input = input.replace(parts[i - index], "");
                } catch (NumberFormatException e) {
                }
                input = input.replace(part, "");
            }
        }
        Pattern pattern = Pattern.compile("([01]?\\d|2[0-3])( |:)(([0-5]?\\d?)?)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String time = matcher.group().trim();
            input = input.replace(time, "");
        }
        parts = input.split("\\s");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.matches("в")) sb.append(" ").append(part);
        }
        return sb.toString().trim();
    }

    @Override
    protected int getMonth(String input) {
        int res = -1;
        if (input.contains("январь") || input.contains("января")) res = 0;
        if (input.contains("февраль") || input.contains("февраля")) res = 1;
        if (input.contains("март") || input.contains("марта")) res = 2;
        if (input.contains("апрель") || input.contains("апреля")) res = 3;
        if (input.contains("май") || input.contains("мая")) res = 4;
        if (input.contains("июнь") || input.contains("июня")) res = 5;
        if (input.contains("июль") || input.contains("июля")) res = 6;
        if (input.contains("август") || input.contains("августа")) res = 7;
        if (input.contains("сентябрь") || input.contains("сентября")) res = 8;
        if (input.contains("октябрь") || input.contains("октября")) res = 9;
        if (input.contains("ноябрь") || input.contains("ноября")) res = 10;
        if (input.contains("декабрь") || input.contains("декабря")) res = 11;
        return res;
    }

    @Override
    public boolean hasCall(String input) {
        return input.matches(".*звонить.*");
    }

    @Override
    public String clearCall(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (string.matches(".*звонить.*")) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public boolean isTimer(String input) {
        return input.matches(".*через.*");
    }

    @Override
    public String cleanTimer(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (isTimer(string)) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public boolean hasSender(String input) {
        return input.matches(".*отправ.*");
    }

    @Override
    public String clearSender(String input) {
        String[] parts = input.split("\\s");
        for (String string : parts) {
            if (string.matches(".*отправ.*")) {
                input = input.replace(string, "");
                break;
            }
        }
        return input.trim();
    }

    @Override
    public boolean hasNote(String input) {
        return input.startsWith("заметка");
    }

    @Override
    public String clearNote(String input) {
        input = input.replace("заметка", "");
        return input.trim();
    }

    @Override
    public boolean hasAction(String input) {
        return input.startsWith("открыть") || input.matches(".*помощь.*") ||
                input.matches(".*настро.*") || input.matches(".*громкость.*")
                || input.matches(".*сообщить.*");
    }

    @Override
    public Action getAction(String input) {
        if (input.matches(".*помощь.*"))
            return Action.HELP;
        else if (input.matches(".*громкость.*"))
            return Action.VOLUME;
        else if (input.matches(".*настройки.*"))
            return Action.SETTINGS;
        else if (input.matches(".*сообщить.*"))
            return Action.REPORT;
        else return Action.APP;
    }

    @Override
    public boolean hasEvent(String input) {
        return input.startsWith("добавить") || input.matches("ново?е?ы?й?.*");
    }

    @Override
    public Action getEvent(String input) {
        if (input.matches(".*день рождения.*"))
            return Action.BIRTHDAY;
        else return Action.REMINDER;
    }

    @Override
    public boolean hasEmptyTrash(String input) {
        return input.matches(".*очисти(ть)? корзин.*");
    }

    @Override
    public boolean hasDisableReminders(String input) {
        return input.matches(".*выключи (все)? ?напоминания.*");
    }

    @Override
    public boolean hasGroup(String input) {
        return input.matches(".*добавь группу.*");
    }

    @Override
    public String clearGroup(String input) {
        StringBuilder sb = new StringBuilder();
        String[] parts = input.split(" ");
        boolean st = false;
        for (String s : parts) {
            if (s.matches(".*групп.*")) {
                st = true;
                continue;
            }
            if (st) {
                sb.append(s);
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    protected int hasHours(String input) {
        if (input.matches(".*час.*")) return 1;
        else if (input.matches("\\d+")) return 0;
        return -1;
    }

    @Override
    protected int hasMinutes(String input) {
        if (input.matches(".*минуту?.*")) return 1;
        else if (input.matches("\\d+")) return 0;
        return -1;
    }

    @Override
    protected boolean hasSeconds(String input) {
        return input.matches(".*секунд.*");
    }

    @Override
    protected boolean hasDays(String input) {
        return input.matches(".*дня.*") || input.matches(".*дней.*") || input.matches(".*день.*");
    }

    @Override
    protected boolean hasWeeks(String input) {
        return input.matches(".*недел.*");
    }

    @Override
    protected int findNumber(String input) {
        int number = -1;
        if (input.matches("ноль")) number = 0;
        if (input.matches("один") || input.matches("одну") || input.matches("одна")) number = 1;
        if (input.matches("два") || input.matches("две")) number = 2;
        if (input.matches("три")) number = 3;
        if (input.matches("четыре")) number = 4;
        if (input.matches("пять")) number = 5;
        if (input.matches("шесть")) number = 6;
        if (input.matches("семь")) number = 7;
        if (input.matches("восемь")) number = 8;
        if (input.matches("девять")) number = 9;
        if (input.matches("десять")) number = 10;
        if (input.matches("одиннадцать")) number = 11;
        if (input.matches("двенадцать")) number = 12;
        if (input.matches("тринадцать")) number = 13;
        if (input.matches("четырнадцать")) number = 14;
        if (input.matches("пятнадцать")) number = 15;
        if (input.matches("шестнадцать")) number = 16;
        if (input.matches("семнадцать")) number = 17;
        if (input.matches("восемнадцать")) number = 18;
        if (input.matches("девятнадцать")) number = 19;
        if (input.matches("двадцать")) number = 20;
        if (input.matches("тридцать")) number = 30;
        if (input.matches("сорок")) number = 40;
        if (input.matches("пятьдесят")) number = 50;
        if (input.matches("шестьдесят")) number = 60;
        if (input.matches("семьдесят")) number = 70;
        if (input.matches("восемьдесят")) number = 80;
        if (input.matches("девяносто")) number = 90;

        if (input.matches("первого")) number = 1;
        if (input.matches("второго")) number = 2;
        if (input.matches("третьего")) number = 3;
        if (input.matches("четвертого")) number = 4;
        if (input.matches("пятого")) number = 5;
        if (input.matches("шестого")) number = 6;
        if (input.matches("седьмого")) number = 7;
        if (input.matches("восьмого")) number = 8;
        if (input.matches("девятого")) number = 9;
        if (input.matches("десятого")) number = 10;
        if (input.matches("одиннадцатого")) number = 11;
        if (input.matches("двенадцатого")) number = 12;
        if (input.matches("тринадцатого")) number = 13;
        if (input.matches("четырнадцатого")) number = 14;
        if (input.matches("пятнадцатого")) number = 15;
        if (input.matches("шестнадцатого")) number = 16;
        if (input.matches("семнадцатого")) number = 17;
        if (input.matches("восемнадцатого")) number = 18;
        if (input.matches("девятнадцатого")) number = 19;
        if (input.matches("двадцатого")) number = 20;
        if (input.matches("тридцатого")) number = 30;
        if (input.matches("сорокового")) number = 40;
        if (input.matches("пятидесятого")) number = 50;
        if (input.matches("шестидесятого")) number = 60;
        if (input.matches("семидесятого")) number = 70;
        if (input.matches("восьмидесятого")) number = 80;
        if (input.matches("девяностого")) number = 90;
        return number;
    }
}
