package ch.ethz.smartheating.schedule;


import java.util.Calendar;

import ch.ethz.smartheating.Utility;

/**
 * Created by schmisam on 22/06/15.
 */
public class ScheduleEntry {
    private long mId;
    private int mStartTime;
    private int mEndTime;
    private int mDay;
    private double mTemperature;
    private int mColor;

    public ScheduleEntry() {

    }

    /**
     * Initializes the entry for ScheduleView.
     *
     * @param id          The id of the entry.
     * @param temperature Temperature of the entry.
     * @param startTime   Hour (in 24-hour format) when the entry starts.
     * @param endTime     Hour (in 24-hour format) when the entry ends.
     */
    public ScheduleEntry(long id, double temperature, int startTime, int endTime, int day) {
        this.mId = id;
        this.mStartTime = startTime;
        this.mEndTime = endTime;
        this.mDay = day;
        this.mTemperature = temperature;
        this.mColor = Utility.getColorForTemperature(temperature);
    }

    public int getDay() {
        return mDay;
    }

    public int getStartTime () { return mStartTime; }

    public void setStartTime(int startTime) {
        this.mStartTime = startTime;
    }

    public int getEndTime() {
        return mEndTime;
    }

    public void setEndTime(int endTime) {
        this.mEndTime = endTime;
    }

    public double getTemperature() {
        return mTemperature;
    }

    public void setTemperature(double temperature) {
        this.mTemperature = temperature;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }
}