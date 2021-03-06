package com.backdoor.engine;

import com.backdoor.engine.lang.Worker;
import com.backdoor.engine.lang.WorkerFactory;
import com.backdoor.engine.lang.WorkerInterface;
import com.backdoor.engine.misc.Action;
import com.backdoor.engine.misc.ActionType;
import com.backdoor.engine.misc.Ampm;
import com.backdoor.engine.misc.ContactOutput;
import com.backdoor.engine.misc.ContactsInterface;
import com.backdoor.engine.misc.Long;
import com.backdoor.engine.misc.TimeUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Recognizer {

    private String[] times;
    private WorkerInterface worker;
    private ContactsInterface contactsInterface;
    public static Locale locale = Locale.getDefault();

    private Recognizer(String[] times, String locale, ContactsInterface contactsInterface) {
        this.times = times;
        this.contactsInterface = contactsInterface;
        this.updateLocale(locale);
    }

    public void updateLocale(String locale) {
        System.out.println("updateLocale: " + locale);
        worker = WorkerFactory.getWorker(locale);
    }

    public void setLocale(Locale locale) {
        Recognizer.locale = locale;
    }

    public Model parse(String string) {
        String keyStr = string.toLowerCase().trim();
        keyStr = worker.replaceNumbers(keyStr);
        System.out.println("parse: " + keyStr + ", worker " + worker);
        if (worker.hasShowAction(keyStr)) {
            String local = keyStr + "";
            Action action = worker.getShowAction(local);
            if (action != null) {
                boolean hasNext = worker.hasNextModifier(local);
                long date;
                Long multi = new Long();
                worker.getMultiplier(local, multi);
                if (hasNext) {
                    date = System.currentTimeMillis() + multi.get();
                } else {
                    Long dt = new Long();
                    worker.getDate(local, dt);
                    date = dt.get();
                }
                Model model = new Model();
                model.setAction(action);
                model.setType(ActionType.SHOW);
                model.setDateTime(TimeUtil.getGmtFromDateTime(date));
                return model;
            }
        }
        if (worker.hasNote(keyStr)) {
            return getNote(keyStr);
        }
        if (worker.hasGroup(keyStr)) {
            keyStr = worker.clearGroup(keyStr);
            return getGroup(keyStr);
        }
        if (worker.hasEvent(keyStr)) {
            Model model = getEvent(keyStr);
            if (model != null) {
                return model;
            }
        }
        if (worker.hasAction(keyStr)) {
            return getAction(keyStr);
        }
        if (worker.hasEmptyTrash(keyStr)) {
            return getEmptyTrash();
        }
        if (worker.hasDisableReminders(keyStr)) {
            return getDisableAction();
        }
        if (worker.hasAnswer(keyStr)) {
            return getAnswer(keyStr);
        }

        Action type = Action.DATE;
        String number = null;
        boolean hasAction = false;
        if (worker.hasCall(keyStr)) {
            keyStr = worker.clearCall(keyStr);
            hasAction = true;
            type = Action.CALL;
        }
        if (worker.hasSender(keyStr)) {
            keyStr = worker.clearSender(keyStr);
            Action actionType = worker.getMessageType(keyStr);
            if (actionType != null) {
                hasAction = true;
                keyStr = worker.clearMessageType(keyStr);
                type = actionType;
            }
        }
        boolean repeating;
        boolean isEveryDay = false;
        boolean hasWeekday = false;
        List<Integer> weekdays = new ArrayList<>();
        long repeat = 0;
        if (repeating = worker.hasRepeat(keyStr)) {
            isEveryDay = worker.hasEveryDay(keyStr);
            keyStr = worker.clearRepeat(keyStr);

            System.out.println("parse: has repeat -> " + keyStr + ", isEvery " + isEveryDay);
            repeat = worker.getDaysRepeat(keyStr);
            if (repeat != 0) {
                keyStr = worker.clearDaysRepeat(keyStr);
            }

            if (isEveryDay) {
                weekdays.clear();
                for (int i = 0; i < 7; i++) {
                    weekdays.add(1);
                }
                if (type == Action.CALL) {
                    type = Action.WEEK_CALL;
                } else if (type == Action.MESSAGE) {
                    type = Action.WEEK_SMS;
                } else {
                    type = Action.WEEK;
                }
            }
        }
        boolean isCalendar;
        if (isCalendar = worker.hasCalendar(keyStr)) {
            keyStr = worker.clearCalendar(keyStr);
        }
        boolean today;
        if (today = worker.hasToday(keyStr)) {
            keyStr = worker.clearToday(keyStr);
        }
        boolean afterTomorrow;
        if (afterTomorrow = worker.hasAfterTomorrow(keyStr)) {
            keyStr = worker.clearAfterTomorrow(keyStr);
        }
        boolean tomorrow;
        if (tomorrow = worker.hasTomorrow(keyStr)) {
            keyStr = worker.clearTomorrow(keyStr);
        }
        Ampm ampm = worker.getAmpm(keyStr);
        if (ampm != null) {
            keyStr = worker.clearAmpm(keyStr);
        }
        if (!isEveryDay) {
            weekdays = worker.getWeekDays(keyStr);
            for (int day : weekdays) {
                if (day == 1) {
                    hasWeekday = true;
                    break;
                }
            }
            keyStr = worker.clearWeekDays(keyStr);
            if (hasWeekday) {
                if (type == Action.CALL) {
                    type = Action.WEEK_CALL;
                } else if (type == Action.MESSAGE) {
                    type = Action.WEEK_SMS;
                } else {
                    type = Action.WEEK;
                }
            }
        }

        boolean hasTimer;
        Long afterTime = new Long();
        if (hasTimer = worker.isTimer(keyStr)) {
            keyStr = worker.cleanTimer(keyStr);
            keyStr = worker.getMultiplier(keyStr, afterTime);
        }
        System.out.println("parse: " + afterTime.get() + ", input " + keyStr);
        Long date = new Long();
        keyStr = worker.getDate(keyStr, date);

        System.out.println("parse: after date " + keyStr);
        long time = worker.getTime(keyStr, ampm, times);
        if (time != 0) {
            keyStr = worker.clearTime(keyStr);
        }
        System.out.println("parse: " + keyStr + ", time " + time + ", date " + date);
        if (today) {
            System.out.println("parse: today");
            time = getTodayTime(time);
        } else if (afterTomorrow) {
            System.out.println("parse: after tomorrow");
            time = getAfterTomorrowTime(time);
        } else if (tomorrow) {
            System.out.println("parse: tomorrow");
            time = getTomorrowTime(time);
        } else if (isEveryDay) {
            System.out.println("parse: everyday");
            time = getDayTime(time, weekdays);
        } else if (hasWeekday && !repeating) {
            System.out.println("parse: on weekday");
            time = getDayTime(time, weekdays);
        } else if (repeating) {
            System.out.println("parse: repeating");
            time = getRepeatingTime(time, hasWeekday);
        } else if (hasTimer) {
            System.out.println("parse: timer");
            time = System.currentTimeMillis() + afterTime.get();
        } else if (date.get() != 0 || time != 0) {
            System.out.println("parse: date/time");
            time = getDateTime(date.get(), time);
        } else {
            return null;
        }

        String message = null;
        if (hasAction && (type == Action.MESSAGE || type == Action.MAIL)) {
            message = worker.getMessage(keyStr);
            keyStr = worker.clearMessage(keyStr);
            if (message != null) {
                keyStr = keyStr.replace(message, "");
            }
        }
        if (hasAction && contactsInterface != null) {
            ContactOutput output = contactsInterface.findNumber(keyStr);
            if (output == null) {
                return null;
            }
            keyStr = output.getOutput();
            number = output.getNumber();
            if (type == Action.MAIL) {
                output = contactsInterface.findEmail(keyStr);
                number = output.getNumber();
                keyStr = output.getOutput();
            }
            if (number == null) {
                return null;
            }
        }

        String task = StringUtils.capitalize(StringUtils.normalizeSpace(keyStr));
        if (hasAction) {
            task = StringUtils.capitalize(message);
            if ((type == Action.MESSAGE || type == Action.MAIL) && task == null) {
                return null;
            }
        }
        Model model = new Model();
        model.setType(ActionType.REMINDER);
        model.setSummary(task);
        model.setDateTime(TimeUtil.getGmtFromDateTime(time));
        model.setWeekdays(weekdays);
        model.setRepeatInterval(repeat);
        model.setTarget(number);
        model.setHasCalendar(isCalendar);
        model.setAction(type);
        return model;
    }

    private Model getAnswer(String keyStr) {
        Model model = new Model();
        model.setType(ActionType.ANSWER);
        model.setAction(worker.getAnswer(keyStr));
        return model;
    }

    private long getDateTime(long date, long time) {
        if (date == 0) {
            date = System.currentTimeMillis();
        }
        if (time == 0) {
            time = date;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.setTimeInMillis(calendar.getTimeInMillis() + Worker.DAY);
        }
        return calendar.getTimeInMillis();
    }

    private long getRepeatingTime(long time, boolean hasWeekday) {
        Calendar calendar = Calendar.getInstance();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        calendar.setTimeInMillis(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (!hasWeekday) {
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.setTimeInMillis(calendar.getTimeInMillis() + Worker.DAY);
            }
        }
        return calendar.getTimeInMillis();
    }

    private long getDayTime(long time, List<Integer> weekdays) {
        Calendar calendar = Calendar.getInstance();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        calendar.setTimeInMillis(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int count = Worker.getNumberOfSelectedWeekdays(weekdays);
        if (count == 1) {
            while (true) {
                int mDay = calendar.get(Calendar.DAY_OF_WEEK);
                if (weekdays.get(mDay - 1) == 1 && calendar.getTimeInMillis() > System.currentTimeMillis()) {
                    break;
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        return calendar.getTimeInMillis();
    }

    private long getTomorrowTime(long time) {
        return getTime(time, 1);
    }

    private long getAfterTomorrowTime(long time) {
        return getTime(time, 2);
    }

    private long getTodayTime(long time) {
        return getTime(time, 0);
    }

    private long getTime(long time, int days) {
        Calendar calendar = Calendar.getInstance();
        if (time == 0) {
            time = System.currentTimeMillis();
        }
        calendar.setTimeInMillis(time);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(System.currentTimeMillis() + (Worker.DAY * days));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private Model getDisableAction() {
        Model model = new Model();
        model.setType(ActionType.ACTION);
        model.setAction(Action.DISABLE);
        return model;
    }

    private Model getEmptyTrash() {
        Model model = new Model();
        model.setType(ActionType.ACTION);
        model.setAction(Action.TRASH);
        return model;
    }

    private Model getGroup(String keyStr) {
        keyStr = StringUtils.capitalize(worker.clearNote(keyStr));
        Model model = new Model();
        model.setSummary(keyStr);
        model.setType(ActionType.GROUP);
        return model;
    }

    private Model getEvent(String keyStr) {
        Action event = worker.getEvent(keyStr);
        if (event == Action.NO_EVENT) {
            return null;
        }
        Model model = new Model();
        model.setType(ActionType.ACTION);
        model.setAction(event);
        return model;
    }

    private Model getAction(String keyStr) {
        Model model = new Model();
        model.setType(ActionType.ACTION);
        model.setAction(worker.getAction(keyStr));
        return model;
    }

    private Model getNote(String keyStr) {
        keyStr = StringUtils.capitalize(worker.clearNote(keyStr));
        Model model = new Model();
        model.setSummary(keyStr);
        model.setType(ActionType.NOTE);
        return model;
    }

    public void setContactHelper(ContactsInterface contactsInterface) {
        this.contactsInterface = contactsInterface;
    }

    public static class Builder {

        public Builder() {
        }

        public TimeBuilder setLocale(String locale) {
            return new TimeBuilder(locale);
        }

        public class TimeBuilder {

            private String locale;

            TimeBuilder(String locale) {
                this.locale = locale;
            }

            public ExtraBuilder setTimes(String[] times) {
                return new ExtraBuilder(locale, times);
            }
        }

        public class ExtraBuilder {

            private ContactsInterface contactsInterface;
            private String[] times;
            private String locale;

            ExtraBuilder(String locale, String[] times) {
                this.locale = locale;
                this.times = times;
            }

            public ExtraBuilder setContactsInterface(ContactsInterface contactsInterface) {
                this.contactsInterface = contactsInterface;
                return this;
            }

            public Recognizer build() {
                return new Recognizer(times, locale, contactsInterface);
            }
        }
    }
}
