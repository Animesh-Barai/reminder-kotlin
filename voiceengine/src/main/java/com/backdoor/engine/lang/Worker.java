package com.backdoor.engine.lang;

import com.backdoor.engine.Recognizer;
import com.backdoor.engine.misc.Ampm;
import com.backdoor.engine.misc.Long;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class Worker implements WorkerInterface {

    /**
     * Millisecond constants.
     */
    protected final static long SECOND = 1000;
    protected final static long MINUTE = 60 * SECOND;
    protected final static long HOUR = MINUTE * 60;
    protected final static long HALF_DAY = HOUR * 12;
    public final static long DAY = HALF_DAY * 2;

    protected final static String WHITESPACES = "\\s+";
    protected final static String WHITESPACE = "\\s";

    protected SimpleDateFormat[] getHourFormats() {
        return new SimpleDateFormat[] {
                new SimpleDateFormat("HH mm", Recognizer.locale),
                new SimpleDateFormat("HH:mm", Recognizer.locale)
        };
    }

    protected SimpleDateFormat getHourFormat() {
        return new SimpleDateFormat("HH:mm",Recognizer.locale);
    }

    public static int getNumberOfSelectedWeekdays(List<Integer> days) {
        int count = 0;
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i) == 1) {
                count++;
            }
        }
        return count;
    }

    static int getSelectedWeekday(List<Integer> days) {
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i) == 1) {
                return i;
            }
        }
        return -1;
    }

    boolean isCorrectTime(int hourOfDay, int minuteOfHour) {
        return hourOfDay < 24 && minuteOfHour < 60;
    }

    boolean isLeapYear(int year) {
        return (year % 4 == 0) && year % 100 != 0 ||
                (year % 4 == 0) && (year % 100 == 0) && (year % 400 == 0);
    }

    protected abstract String[] getWeekdays();

    protected abstract float findNumber(String input);

    protected abstract int hasHours(String input);

    protected abstract int hasMinutes(String input);

    protected abstract boolean hasSeconds(String input);

    protected abstract boolean hasDays(String input);

    protected abstract boolean hasWeeks(String input);

    protected abstract boolean hasMonth(String input);

    @Override
    public boolean hasEveryDay(String input) {
        return false;
    }

    protected abstract int getMonth(String input);

    @Override
    public String getMultiplier(String input, Long res) {
        System.out.println("getMultiplier: " + input);
        String[] parts = input.split("\\s");
        for (int i = 0; i < parts.length; i++) {
            String string = parts[i];
            try {
                float number = 0;
                try {
                    number = Float.parseFloat(string);
                    if (number != -1) {
                        parts[i] = "";
                    }
                    for (int j = i; j < parts.length; j++) {
                        float multi = getMulti(parts[j]);
                        if (multi != -1) {
                            number = number * multi;
                            parts[j] = "";
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    float multi = getMulti(string);
                    if (multi != -1) {
                        number += multi;
                        parts[i] = "";
                    } else {
                        continue;
                    }
                }
                if (number > 0) {
                    res.set(res.get() + (long) number);
                }
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException ignored) {
            }
        }
        String out = clipStrings(parts);
        System.out.println("getMultiplier: out -> " + out + ", res -> " + res.get());
        return out;
    }

    private float getMulti(String input) {
        if (hasSeconds(input)) {
            return SECOND;
        } else if (hasMinutes(input) != -1) {
            return MINUTE;
        } else if (hasHours(input) != -1) {
            return HOUR;
        } else if (hasDays(input)) {
            return DAY;
        } else if (hasWeeks(input)) {
            return 7 * DAY;
        } else if (hasMonth(input)) {
            return 30 * DAY;
        }
        return -1;
    }

    @Override
    public String replaceNumbers(String input) {
        String[] parts = input.split("\\s+");
        float allNumber = 0;
        int beginIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            float number = findNumber(parts[i]);
            if (number != -1) {
                allNumber += number;
                parts[i] = "";
                if (beginIndex == -1) {
                    beginIndex = i;
                }
            } else {
                number = findFloat(parts[i]);
                if (number != -1) {
                    allNumber += number;
                    if (beginIndex == -1) {
                        beginIndex = i;
                    }
                }
            }
        }
        System.out.println("replaceNumbers: parts -> " + Arrays.toString(parts));
        if (beginIndex != -1 && allNumber != 0) {
            String[] newP = new String[parts.length + 1];
            for (int i = 0; i < parts.length; i++) {
                if (i > beginIndex) {
                    newP[i + 1] = parts[i];
                } else if (i == beginIndex) {
                    newP[beginIndex] = String.valueOf(allNumber);
                    newP[i + 1] = parts[i];
                } else {
                    newP[i] = parts[i];
                }
            }
            parts = newP;
        }
        System.out.println("replaceNumbers: after parts -> " + Arrays.toString(parts));
        String out = clipStrings(parts);
        out = clearFloats(out);
        System.out.println("replaceNumbers: out -> " + out);
        return out;
    }

    protected abstract String clearFloats(String input);

    protected String clipStrings(String[] parts) {
        StringBuilder out = new StringBuilder();
        for (String s : parts) out.append(" ").append(s);
        return out.toString().trim().replaceAll("\\s{2,}", " ");
    }

    protected abstract float findFloat(String input);

    @Override
    public long getTime(String input, Ampm ampm, String[] times) {
        System.out.println("getTime: " + ampm + ", input " + input);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        String[] parts = input.split("\\s+");
        float h = -1;
        float m = -1;
        float reserveHour = 0;
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            int hoursIndex = hasHours(part);
            int minutesIndex = hasMinutes(part);
            if (hoursIndex != -1) {
                float integer;
                boolean hourSuccess = false;
                try {
                    integer = Float.parseFloat(parts[i - hoursIndex]);
                    hourSuccess = true;
                    parts[i - hoursIndex] = "";
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    integer = 1;
                }
                if (ampm == Ampm.EVENING) {
                    integer += 12;
                }
                h = integer;
                if (hourSuccess) {
                    try {
                        m = Integer.parseInt(parts[i + 1]);
                        parts[i + 1] = "";
                    } catch (Exception ignored) {
                    }
                }
            }
            if (minutesIndex != -1) {
                float integer;
                try {
                    integer = Float.parseFloat(parts[i - minutesIndex]);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    integer = 0;
                }
                m = integer;
            }
            try {
                reserveHour = Float.parseFloat(parts[i]);
            } catch (NumberFormatException ignored) {
            }
        }
        Date date = getShortTime(input);
        if (date != null) {
            calendar.setTime(date);
            if (ampm == Ampm.EVENING) {
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                calendar.set(Calendar.HOUR_OF_DAY, hour < 12 ? hour + 12 : hour);
            }
            return calendar.getTimeInMillis();
        }
        if (h != -1) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, (int) h);
            if (m != -1) {
                calendar.set(Calendar.MINUTE, (int) m);
            } else {
                calendar.set(Calendar.MINUTE, 0);
            }
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }
        if (calendar.getTimeInMillis() == 0 && reserveHour != 0) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, (int) reserveHour);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (ampm == Ampm.EVENING) {
                calendar.add(Calendar.HOUR_OF_DAY, 12);
            }
        }
        if (calendar.getTimeInMillis() == 0 && ampm != null) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            try {
                SimpleDateFormat hourFormat = getHourFormat();
                if (ampm == Ampm.MORNING) {
                    calendar.setTime(hourFormat.parse(times[0]));
                }
                if (ampm == Ampm.NOON) {
                    calendar.setTime(hourFormat.parse(times[1]));
                }
                if (ampm == Ampm.EVENING) {
                    calendar.setTime(hourFormat.parse(times[2]));
                }
                if (ampm == Ampm.NIGHT) {
                    calendar.setTime(hourFormat.parse(times[3]));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return calendar.getTimeInMillis();
    }

    protected abstract Date getShortTime(String input);

    @Override
    public String clearToday(String input) {
        String[] parts = input.split("\\s");
        for (String s : parts) {
            if (hasToday(s)) {
                input = input.replace(s, "");
            }
        }
        return input.trim();
    }

    @Override
    public String clearAfterTomorrow(String input) {
        if (hasAfterTomorrow(input)) {
            input = input.replace(getAfterTomorrow(), "");
        }
        return input;
    }

    protected abstract String getAfterTomorrow();
}