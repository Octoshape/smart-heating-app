package ch.ethz.smartheating.model;


import android.graphics.Color;

import ch.ethz.smartheating.utilities.Utility;

/**
 * A {@link ScheduleEntry} object consists of a temperature, a start time, an end time and the day
 * the entry is scheduled for. The color of the entry is given by the temperature.
 */
public class ScheduleEntry {
    private int mStartTime;
    private int mEndTime;
    private int mDay;
    private double mTemperature;
    private int mColor;

    /**
     * Initializes the entry for ScheduleView.
     *
     * @param temperature Temperature of the entry.
     * @param startTime   Hour (in 24-hour format) when the entry starts.
     * @param endTime     Hour (in 24-hour format) when the entry ends.
     * @param day         Day when the entry occurs.
     */
    public ScheduleEntry(double temperature, int startTime, int endTime, int day) {
        this.mStartTime = startTime;
        this.mEndTime = endTime;
        this.mDay = day;
        this.mTemperature = temperature;
        if (temperature == Utility.NO_HEATING_TEMPERATURE) {
            this.mColor = Color.LTGRAY;
        } else {
            this.mColor = Utility.getColorForTemperature(temperature);
        }
    }

    public int getDay() {
        return mDay;
    }

    public void setDay(int day) {
        this.mDay = day;
    }

    public int getStartTime() {
        return mStartTime;
    }

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
}