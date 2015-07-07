package ch.ethz.smartheating.schedule;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.ethz.smartheating.R;

/**
 * Created by schmisam on 22/06/15.
 */

public class ScheduleView extends View {

    private final Context mContext;
    private Calendar mToday;
    private Paint mTimeTextPaint;
    private float mTimeTextWidth;
    private float mTimeTextHeight;
    private Paint mHeaderTextPaint;
    private float mHeaderTextHeight;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private PointF mCurrentOrigin = new PointF(0f, 0f);
    private Paint mHeaderBackgroundPaint;
    private float mWidthPerDay;
    private Paint mDayBackgroundPaint;
    private Paint mHourSeparatorPaint;
    private float mHeaderMarginBottom;
    private Paint mTodayBackgroundPaint;
    private Paint mTodayHeaderTextPaint;
    private Paint mEntryBackgroundPaint;
    private float mTimeColumnWidth;
    private List<EntryRect> mEntryRects;
    private TextPaint mEntryTextPaint;
    private Paint mHeaderColumnBackgroundPaint;
    private Scroller mStickyScroller;
    private float mDistanceY = 0;
    private List<String> mDayLabels = Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    // Attributes and their default values.
    private int mHourHeight = 50;
    private int mColumnGap = 10;
    private int mFirstDayOfWeek = Calendar.MONDAY;
    private int mTextSize = 12;
    private int mHeaderColumnPadding = 10;
    private int mHeaderColumnTextColor = Color.BLACK;
    private int mHeaderRowPadding = 10;
    private int mHeaderRowBackgroundColor = Color.LTGRAY;
    private int mDayBackgroundColor = Color.rgb(245, 245, 245);
    private int mHourSeparatorColor = Color.rgb(200, 200, 200);
    private int mTodayBackgroundColor = Color.rgb(239, 247, 254);
    private int mHourSeparatorHeight = 2;
    private int mTodayHeaderTextColor = Color.rgb(39, 137, 228);
    private int mEntryTextSize = 12;
    private int mEntryTextColor = Color.BLACK;
    private int mEntryPadding = 8;
    private int mHeaderColumnBackgroundColor = Color.WHITE;
    private int mDefaultEventColor;
    private int mOverlappingEventGap = 0;
    private int mEntryMarginVertical = 0;
    private float mXScrollingSpeed = 1f;
    private boolean mRefreshEntries = false;

    // Listeners.
    private ScheduleEntryClickListener mEntryClickListener, mEntryLongPressListener;
    private EmptyViewClickListener mEmptyViewClickListener, mEmptyViewLongPressListener;
    private GetCurrentScheduleListener mGetCurrentScheduleListener;

    private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            mScroller.forceFinished(true);
            mStickyScroller.forceFinished(true);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mDistanceY = distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mScroller.forceFinished(true);
            mStickyScroller.forceFinished(true);

            mScroller.fling(0, (int) mCurrentOrigin.y, 0, (int) velocityY, 0, 0, (int) -(mHourHeight * 24 + mHeaderTextHeight + mHeaderRowPadding * 2 - getHeight()), 0);

            ViewCompat.postInvalidateOnAnimation(ScheduleView.this);
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // If the tap was on an entry, trigger the callback.
            if (mEntryRects != null && mEntryClickListener != null) {
                List<EntryRect> reversedEntryRects = new ArrayList<EntryRect>(mEntryRects);
                Collections.reverse(reversedEntryRects);
                EntryRect lastEntry = reversedEntryRects.get(reversedEntryRects.size() - 1);
                for (EntryRect entry : reversedEntryRects) {
                    if (entry.rectF != null && e.getX() > entry.rectF.left && e.getX() < entry.rectF.right && e.getY() > entry.rectF.top && e.getY() < entry.rectF.bottom) {
                        Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());
                        if (selectedTime != null) {
                            mEntryClickListener.onScheduleEntryClick(entry.entry, entry.rectF, lastEntry.entry, selectedTime.get(Calendar.HOUR_OF_DAY));
                            playSoundEffect(SoundEffectConstants.CLICK);
                            return super.onSingleTapConfirmed(e);
                        }
                    }
                    lastEntry = entry;
                }
            }

            // If the tap was on an empty space, trigger the callback.
            if (mEmptyViewClickListener != null && e.getX() > mTimeColumnWidth && e.getY() > (mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());
                if (selectedTime != null) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    mEmptyViewClickListener.onEmptyViewClicked(selectedTime);
                }
            }

            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);

            if (mEntryLongPressListener != null && mEntryRects != null) {
                List<EntryRect> reversedEntryRects = mEntryRects;
                Collections.reverse(reversedEntryRects);
                for (EntryRect entry : reversedEntryRects) {
                    if (entry.rectF != null && e.getX() > entry.rectF.left && e.getX() < entry.rectF.right && e.getY() > entry.rectF.top && e.getY() < entry.rectF.bottom) {
                        mEntryLongPressListener.onScheduleEntryLongPress(entry.entry, entry.rectF);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return;
                    }
                }
            }

            // If the tap was on an empty space, trigger the callback.
            if (mEmptyViewLongPressListener != null && e.getX() > mTimeColumnWidth && e.getY() > (mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)) {
                Calendar selectedTime = getTimeFromPoint(e.getX(), e.getY());
                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    mEmptyViewLongPressListener.onEmptyViewLongPress(selectedTime);
                }
            }
        }
    };

    public ScheduleView(Context context) {
        this(context, null);
    }

    public ScheduleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScheduleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Hold references.
        mContext = context;

        // Get the attribute values (if any).
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScheduleView, 0, 0);
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.ScheduleView_firstDayOfWeek, mFirstDayOfWeek);
            mHourHeight = a.getDimensionPixelSize(R.styleable.ScheduleView_hourHeight, mHourHeight);
            mTextSize = a.getDimensionPixelSize(R.styleable.ScheduleView_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize, context.getResources().getDisplayMetrics()));
            mHeaderColumnPadding = a.getDimensionPixelSize(R.styleable.ScheduleView_headerColumnPadding, mHeaderColumnPadding);
            mColumnGap = a.getDimensionPixelSize(R.styleable.ScheduleView_columnGap, mColumnGap);
            mHeaderColumnTextColor = a.getColor(R.styleable.ScheduleView_headerColumnTextColor, mHeaderColumnTextColor);
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.ScheduleView_headerRowPadding, mHeaderRowPadding);
            mHeaderRowBackgroundColor = a.getColor(R.styleable.ScheduleView_headerRowBackgroundColor, mHeaderRowBackgroundColor);
            mDayBackgroundColor = a.getColor(R.styleable.ScheduleView_dayBackgroundColor, mDayBackgroundColor);
            mHourSeparatorColor = a.getColor(R.styleable.ScheduleView_hourSeparatorColor, mHourSeparatorColor);
            mTodayBackgroundColor = a.getColor(R.styleable.ScheduleView_todayBackgroundColor, mTodayBackgroundColor);
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.ScheduleView_hourSeparatorHeight, mHourSeparatorHeight);
            mTodayHeaderTextColor = a.getColor(R.styleable.ScheduleView_todayHeaderTextColor, mTodayHeaderTextColor);
            mEntryTextSize = a.getDimensionPixelSize(R.styleable.ScheduleView_entryTextSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEntryTextSize, context.getResources().getDisplayMetrics()));
            mEntryTextColor = a.getColor(R.styleable.ScheduleView_entryTextColor, mEntryTextColor);
            mEntryPadding = a.getDimensionPixelSize(R.styleable.ScheduleView_hourSeparatorHeight, mEntryPadding);
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.ScheduleView_headerColumnBackground, mHeaderColumnBackgroundColor);
            mOverlappingEventGap = a.getDimensionPixelSize(R.styleable.ScheduleView_overlappingEventGap, mOverlappingEventGap);
            mEntryMarginVertical = a.getDimensionPixelSize(R.styleable.ScheduleView_entryMarginVertical, mEntryMarginVertical);
            mXScrollingSpeed = a.getFloat(R.styleable.ScheduleView_xScrollingSpeed, mXScrollingSpeed);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        // Get the date today.
        mToday = Calendar.getInstance();
        mToday.set(Calendar.HOUR_OF_DAY, 0);
        mToday.set(Calendar.MINUTE, 0);
        mToday.set(Calendar.SECOND, 0);

        // Scrolling initialization.
        mGestureDetector = new GestureDetectorCompat(mContext, mGestureListener);
        mScroller = new OverScroller(mContext);
        mStickyScroller = new Scroller(mContext);

        // Measure settings for time column.
        mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTimeTextPaint.setTextSize(mTextSize);
        mTimeTextPaint.setColor(mHeaderColumnTextColor);
        Rect rect = new Rect();
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length(), rect);
        mTimeTextWidth = mTimeTextPaint.measureText("00 PM");
        mTimeTextHeight = rect.height();
        mHeaderMarginBottom = mTimeTextHeight / 2;

        // Measure settings for header row.
        mHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHeaderTextPaint.setColor(mHeaderColumnTextColor);
        mHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mHeaderTextPaint.setTextSize(mTextSize);
        mHeaderTextPaint.getTextBounds("00 PM", 0, "00 PM".length(), rect);
        mHeaderTextHeight = rect.height();
        mHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Prepare header background paint.
        mHeaderBackgroundPaint = new Paint();
        mHeaderBackgroundPaint.setColor(mHeaderRowBackgroundColor);

        // Prepare day background color paint.
        mDayBackgroundPaint = new Paint();
        mDayBackgroundPaint.setColor(mDayBackgroundColor);

        // Prepare hour separator color paint.
        mHourSeparatorPaint = new Paint();
        mHourSeparatorPaint.setStyle(Paint.Style.STROKE);
        mHourSeparatorPaint.setStrokeWidth(mHourSeparatorHeight);
        mHourSeparatorPaint.setColor(mHourSeparatorColor);

        // Prepare today background color paint.
        mTodayBackgroundPaint = new Paint();
        mTodayBackgroundPaint.setColor(mTodayBackgroundColor);

        // Prepare today header text color paint.
        mTodayHeaderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTodayHeaderTextPaint.setTextAlign(Paint.Align.CENTER);
        mTodayHeaderTextPaint.setTextSize(mTextSize);
        mTodayHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTodayHeaderTextPaint.setColor(mTodayHeaderTextColor);

        // Prepare entry background color.
        mEntryBackgroundPaint = new Paint();
        mEntryBackgroundPaint.setColor(Color.rgb(174, 208, 238));

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint = new Paint();
        mHeaderColumnBackgroundPaint.setColor(mHeaderColumnBackgroundColor);

        // Prepare entry text size and color.
        mEntryTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        mEntryTextPaint.setStyle(Paint.Style.FILL);
        mEntryTextPaint.setColor(mEntryTextColor);
        mEntryTextPaint.setTextSize(mEntryTextSize);

        // Set default entry color.
        mDefaultEventColor = Color.parseColor("#9fc6e7");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the header row.
        drawHeaderRowAndEvents(canvas);

        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas);

        // Hide everything in the first cell (top left corner).
        canvas.drawRect(0, 0, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint);

        // Hide anything that is in the bottom margin of the header row.
        canvas.drawRect(mTimeColumnWidth, mHeaderTextHeight + mHeaderRowPadding * 2, getWidth(), mHeaderRowPadding * 2 + mHeaderTextHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - mHourSeparatorHeight / 2, mHeaderColumnBackgroundPaint);
    }

    private void drawTimeColumnAndAxes(Canvas canvas) {
        // Do not let the view go above/below the limit due to scrolling. Set the max and min limit of the scroll.
        if (mCurrentOrigin.y - mDistanceY > 0) mCurrentOrigin.y = 0;
        else if (mCurrentOrigin.y - mDistanceY < -(mHourHeight * 24 + mHeaderTextHeight + mHeaderRowPadding * 2 - getHeight()))
            mCurrentOrigin.y = -(mHourHeight * 24 + mHeaderTextHeight + mHeaderRowPadding * 2 - getHeight());
        else mCurrentOrigin.y -= mDistanceY;

        // Draw the background color for the header column.
        canvas.drawRect(0, mHeaderTextHeight + mHeaderRowPadding * 2, mTimeColumnWidth, getHeight(), mHeaderColumnBackgroundPaint);

        for (int i = 0; i < 24; i++) {
            float top = mHeaderTextHeight + mHeaderRowPadding * 2 + mCurrentOrigin.y + mHourHeight * i + mHeaderMarginBottom;

            String time = i + ":00";

            if (top < getHeight())
                canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint);
        }
    }

    private void drawHeaderRowAndEvents(Canvas canvas) {
        // Calculate the available width for each day.
        mTimeColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2;
        mWidthPerDay = getWidth() - mTimeColumnWidth - mColumnGap * 6;
        mWidthPerDay = mWidthPerDay / 7;

        float startPixel = mTimeColumnWidth;

        // Prepare to iterate for each hour to draw the hour lines.
        int lineCount = (int) ((getHeight() - mHeaderTextHeight - mHeaderRowPadding * 2 -
                mHeaderMarginBottom) / mHourHeight) + 1;
        lineCount *= 7;
        float[] hourLines = new float[lineCount * 4];

        // Clear the cache for entry rectangles.
        if (mEntryRects != null) {
            for (EntryRect entryRect : mEntryRects) {
                entryRect.rectF = null;
            }
        }

        // Get new schedule entries if necessary.
        if (mEntryRects == null || mRefreshEntries) {
            if (mEntryRects == null) {
                mEntryRects = new ArrayList<EntryRect>();
            }

            List<ScheduleEntry> entries = mGetCurrentScheduleListener.getCurrentSchedule();

            sortEntries(entries);

            for (ScheduleEntry entry : entries) {
                mEntryRects.add(new EntryRect(entry, null));
            }
        }

        for (EntryRect entry : mEntryRects) {
            entry.width = 1f;
            entry.left = 0;
            entry.top = entry.entry.getStartTime() * 60;
            entry.bottom = entry.entry.getEndTime() * 60;
        }

        // Iterate through each day.
        for (int dayNumber = mFirstDayOfWeek; dayNumber < mFirstDayOfWeek + 7; dayNumber++) {

            // Check if the day is today.
            boolean sameDay = ((dayNumber - 1) % 7 + 1) == mToday.get(Calendar.DAY_OF_WEEK);

            canvas.drawRect(startPixel, mHeaderTextHeight + mHeaderRowPadding * 2 + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel + mWidthPerDay, getHeight(), sameDay ? mTodayBackgroundPaint : mDayBackgroundPaint);

            // Prepare the separator lines for hours.
            int i = 0;
            for (int hourNumber = 0; hourNumber < 24; hourNumber++) {
                float top = mHeaderTextHeight + mHeaderRowPadding * 2 + mCurrentOrigin.y + mHourHeight * hourNumber + mTimeTextHeight / 2 + mHeaderMarginBottom;
                if (top > mHeaderTextHeight + mHeaderRowPadding * 2 + mTimeTextHeight / 2 + mHeaderMarginBottom - mHourSeparatorHeight && top < getHeight()) {
                    hourLines[i * 4] = startPixel;
                    hourLines[i * 4 + 1] = top;
                    hourLines[i * 4 + 2] = startPixel + mWidthPerDay;
                    hourLines[i * 4 + 3] = top;
                    i++;
                }
            }

            // Draw the lines for hours.
            canvas.drawLines(hourLines, mHourSeparatorPaint);

            // Draw the events.
            drawEvents(((dayNumber - 1) % 7 + 1), startPixel, canvas);

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap;
        }

        // Draw the header background.
        canvas.drawRect(0, 0, getWidth(), mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint);

        // Reset to the left to draw the day labels.
        startPixel = mTimeColumnWidth;

        // Iterate through each day.
        for (int dayNumber = mFirstDayOfWeek; dayNumber < mFirstDayOfWeek + 7; dayNumber++) {

            // Check if the day is today.
            boolean sameDay = ((dayNumber - 1) % 7 + 1) == mToday.get(Calendar.DAY_OF_WEEK);

            // Draw the day labels.
            canvas.drawText(mDayLabels.get((dayNumber - 1) % 7), startPixel + mWidthPerDay / 2, mHeaderTextHeight + mHeaderRowPadding, sameDay ? mTodayHeaderTextPaint : mHeaderTextPaint);

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap;
        }
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param day            The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     */
    private void drawEvents(int day, float startFromPixel, Canvas canvas) {
        if (mEntryRects != null && mEntryRects.size() > 0) {
            for (int i = 0; i < mEntryRects.size(); i++) {
                if (mEntryRects.get(i).entry.getDay() == day) {

                    // Calculate top.
                    float top = mHourHeight * 24 * mEntryRects.get(i).top / 1440 + mCurrentOrigin.y + mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2 + mEntryMarginVertical;
                    float originalTop = top;
                    if (top < mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2)
                        top = mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2;

                    // Calculate bottom.
                    float bottom = mEntryRects.get(i).bottom;
                    bottom = mHourHeight * 24 * bottom / 1440 + mCurrentOrigin.y + mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2 - mEntryMarginVertical;

                    // Calculate left and right.
                    float left = startFromPixel + mEntryRects.get(i).left * mWidthPerDay;
                    if (left < startFromPixel)
                        left += mOverlappingEventGap;
                    float originalLeft = left;
                    float right = left + mEntryRects.get(i).width * mWidthPerDay;
                    if (right < startFromPixel + mWidthPerDay)
                        right -= mOverlappingEventGap;
                    if (left < mTimeColumnWidth) left = mTimeColumnWidth;

                    // Draw the event and the event name on top of it.
                    RectF eventRectF = new RectF(left, top, right, bottom);
                    if (bottom > mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom + mTimeTextHeight / 2 && left < right &&
                            eventRectF.right > mTimeColumnWidth &&
                            eventRectF.left < getWidth() &&
                            eventRectF.bottom > mHeaderTextHeight + mHeaderRowPadding * 2 + mTimeTextHeight / 2 + mHeaderMarginBottom &&
                            eventRectF.top < getHeight() &&
                            left < right
                            ) {
                        mEntryRects.get(i).rectF = eventRectF;
                        mEntryBackgroundPaint.setColor(mEntryRects.get(i).entry.getColor() == 0 ? mDefaultEventColor : mEntryRects.get(i).entry.getColor());
                        canvas.drawRect(mEntryRects.get(i).rectF, mEntryBackgroundPaint);
                        drawText(String.valueOf(mEntryRects.get(i).entry.getTemperature()) + "°", mEntryRects.get(i).rectF, canvas, originalTop, originalLeft);
                    } else
                        mEntryRects.get(i).rectF = null;
                }
            }
        }
    }


    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param text         The text to draw.
     * @param rect         The rectangle on which the text is to be drawn.
     * @param canvas       The canvas to draw upon.
     * @param originalTop  The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private void drawText(String text, RectF rect, Canvas canvas, float originalTop, float originalLeft) {
        if (rect.right - rect.left - mEntryPadding * 2 < 0) return;

        // Get text dimensions
        StaticLayout textLayout = new StaticLayout(text, mEntryTextPaint, (int) (rect.right - originalLeft - mEntryPadding * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

        // Crop height
        int availableHeight = (int) (rect.bottom - originalTop - mEntryPadding * 2);
        int lineHeight = textLayout.getHeight() / textLayout.getLineCount();
        if (lineHeight < availableHeight && textLayout.getHeight() > rect.height() - mEntryPadding * 2) {
            int lineCount = textLayout.getLineCount();
            int availableLineCount = (int) Math.floor(lineCount * availableHeight / textLayout.getHeight());
            float widthAvailable = (rect.right - originalLeft - mEntryPadding * 2) * availableLineCount;
            textLayout = new StaticLayout(TextUtils.ellipsize(text, mEntryTextPaint, widthAvailable, TextUtils.TruncateAt.END), mEntryTextPaint, (int) (rect.right - originalLeft - mEntryPadding * 2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } else if (lineHeight >= availableHeight) {
            int width = (int) (rect.right - originalLeft - mEntryPadding * 2);
            textLayout = new StaticLayout(TextUtils.ellipsize(text, mEntryTextPaint, width, TextUtils.TruncateAt.END), mEntryTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);
        }

        // Draw text
        canvas.save();
        canvas.translate(originalLeft + mEntryPadding, originalTop + mEntryPadding);
        textLayout.draw(canvas);
        canvas.restore();
    }


    /**
     * A class to hold reference to the entries and their visual representation. An EntryRect is
     * actually the rectangle that is drawn on the calendar for a given entry.
     */
    private class EntryRect {
        public ScheduleEntry entry;
        public RectF rectF;
        public float left;
        public float width;
        public float top;
        public float bottom;

        /**
         * Create a new instance of entry rect.
         *
         * @param entry The ScheduleEntry which this rectangle represents.
         * @param rectF The rectangle.
         */
        public EntryRect(ScheduleEntry entry, RectF rectF) {
            this.entry = entry;
            this.rectF = rectF;
        }
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting the listeners.
    //
    /////////////////////////////////////////////////////////////////

    public void setOnScheduleEntryClickListener(ScheduleEntryClickListener listener) {
        this.mEntryClickListener = listener;
        this.mEntryLongPressListener = listener;
    }

    public void setEmptyViewClickListener(EmptyViewClickListener listener) {
        this.mEmptyViewClickListener = listener;
        this.mEmptyViewLongPressListener = listener;
    }

    public void setGetCurrentScheduleListener(GetCurrentScheduleListener listener) {
        this.mGetCurrentScheduleListener = listener;
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            if (Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) < mWidthPerDay + mColumnGap && Math.abs(mScroller.getFinalX() - mScroller.getStartX()) != 0) {
                mScroller.forceFinished(true);
                float leftDays = Math.round(mCurrentOrigin.x / (mWidthPerDay + mColumnGap));
                if (mScroller.getFinalX() < mScroller.getCurrX())
                    leftDays--;
                else
                    leftDays++;
                int nearestOrigin = (int) (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap));
                mStickyScroller.startScroll((int) mCurrentOrigin.x, 0, -nearestOrigin, 0);
                ViewCompat.postInvalidateOnAnimation(ScheduleView.this);
            } else {
                mCurrentOrigin.y = mScroller.getCurrY();
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }

        if (mStickyScroller.computeScrollOffset()) {
            mCurrentOrigin.x = mStickyScroller.getCurrX();
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Refreshes the view and loads the events again.
     */
    public void notifyDatasetChanged() {
        mRefreshEntries = true;
        invalidate();
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     *
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    public void goToHour(double hour) {
        int verticalOffset = (int) (mHourHeight * hour);
        if (hour < 0)
            verticalOffset = 0;
        else if (hour > 24)
            verticalOffset = mHourHeight * 24;

        if (verticalOffset > mHourHeight * 24 - getHeight() + mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom)
            verticalOffset = (int) (mHourHeight * 24 - getHeight() + mHeaderTextHeight + mHeaderRowPadding * 2 + mHeaderMarginBottom);

        mCurrentOrigin.y = -verticalOffset + mTimeTextHeight / 2;
        invalidate();
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Interfaces.
    //
    /////////////////////////////////////////////////////////////////

    public interface ScheduleEntryClickListener {
        public void onScheduleEntryClick(ScheduleEntry entry, RectF eventRect, ScheduleEntry nextEntry, int hourClicked);

        public void onScheduleEntryLongPress(ScheduleEntry entry, RectF eventRect);
    }

    public interface EmptyViewClickListener {
        public void onEmptyViewClicked(Calendar time); // TODO: Maybe add day as well?

        public void onEmptyViewLongPress(Calendar time);
    }

    public interface GetCurrentScheduleListener {
        public List<ScheduleEntry> getCurrentSchedule();
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Helper functions.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Sorts the entries in ascending order.
     *
     * @param entries The entries to be sorted.
     */
    private void sortEntries(List<ScheduleEntry> entries) {
        Collections.sort(entries, new Comparator<ScheduleEntry>() {
            @Override
            public int compare(ScheduleEntry entry1, ScheduleEntry entry2) {
                int start1 = entry1.getDay();
                int start2 = entry2.getDay();
                int comparator = start1 > start2 ? 1 : (start1 < start2 ? -1 : 0);
                if (comparator == 0) {
                    int end1 = entry1.getStartTime();
                    int end2 = entry2.getStartTime();
                    comparator = end1 > end2 ? 1 : -1;
                }
                return comparator;
            }
        });
    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private Calendar getTimeFromPoint(float x, float y) {
        float startPixel = mTimeColumnWidth;
        for (int dayNumber = 1; dayNumber <= 7; dayNumber++) {
            if (x > startPixel && x < startPixel + mWidthPerDay) {
                Calendar day = (Calendar) mToday.clone();
                day.set(Calendar.DAY_OF_WEEK, dayNumber);
                float pixelsFromZero = y - mCurrentOrigin.y - mHeaderTextHeight
                        - mHeaderRowPadding * 2 - mTimeTextHeight / 2 - mHeaderMarginBottom;
                int hour = (int) (pixelsFromZero / mHourHeight);
                int minute = (int) (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight);
                minute = (minute / 15) * 15;
                day.set(Calendar.HOUR, hour);
                day.set(Calendar.MINUTE, minute);
                return day;
            }
            startPixel += mWidthPerDay + mColumnGap;
        }
        return null;
    }
}


