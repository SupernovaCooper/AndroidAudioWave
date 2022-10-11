package me.relex.widget.waveform;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import me.relex.waveformview.R;
import me.relex.widget.waveform.gesture.DragDetector;
import me.relex.widget.waveform.gesture.OnDragGestureListener;


public class WaveFormThumbView extends View implements OnDragGestureListener {
    private Paint mWaveFormPaint;
    private Paint mWaveFormHighLightPaint;
    @Nullable
    protected WaveFormInfo mBean;
    protected long mTotalTime = 0;
    protected float mThumbScale;
    private long mThumbStartTime = 0;
    private long mThumbDuration = 0;
    private DragDetector mDragDetector;
    private int mThumbStartTimePixel;
    private int mThumbEndTimePixel;

    private OnDragThumbListener mOnDragThumbListener;

    public WaveFormThumbView(Context context) {
        super(context);
        init(context, null);
    }

    public WaveFormThumbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaveFormThumbView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WaveFormThumbView(Context context, AttributeSet attrs, int defStyleAttr,
                             int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        int waveformColor = Color.BLACK;
        int highlightColor = Color.GRAY;

        if (attrs != null) {
            TypedArray typedArray =
                    context.obtainStyledAttributes(attrs, R.styleable.WaveFormThumbView);
            waveformColor = typedArray.getColor(R.styleable.WaveFormThumbView_wf_waveform_normal_color,
                    Color.BLACK);
            highlightColor =
                    typedArray.getColor(R.styleable.WaveFormThumbView_wf_waveform_highlight_color,
                            Color.GRAY);
            typedArray.recycle();
        }

        mWaveFormPaint = new Paint();
        mWaveFormPaint.setColor(waveformColor);
        mWaveFormPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWaveFormPaint.setStrokeWidth(0);

        mWaveFormHighLightPaint = new Paint();
        mWaveFormHighLightPaint.setColor(highlightColor);
        mWaveFormHighLightPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mWaveFormHighLightPaint.setStrokeWidth(0);

        mDragDetector = new DragDetector(context, this);
    }

    public void setWave(WaveFormInfo bean) {
        if (bean == null) {
            return;
        }

        mBean = bean;
        initWave(bean);
        invalidate();
    }

    public void initWave(@NonNull WaveFormInfo bean) {
        int sampleRate = bean.getSample_rate();
        int samplesPerPixel = bean.getSamples_per_pixel();
        int length = bean.getLength();
        mTotalTime = WaveUtil.dataPixelsToTime(length, sampleRate, samplesPerPixel);
        computerMinScaleFactor();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        computerMinScaleFactor();
    }

    private void computerMinScaleFactor() {
        int width = getMeasuredWidth();

        if (mBean == null || width <= 0) {
            return;
        }

        mThumbScale = (float) width / mBean.getLength();
        configScalePaint(mThumbScale);
    }

    private void configScalePaint(float scale) {
        float smokeWidth = (int) Math.ceil(scale);
        smokeWidth = smokeWidth < 0 ? 0 : smokeWidth;
        mWaveFormPaint.setStrokeWidth(smokeWidth);
        mWaveFormHighLightPaint.setStrokeWidth(smokeWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }
            break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                ViewParent parent = getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
            }
            break;
        }

        return mDragDetector.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBean == null) {
            return;
        }
        drawWave(canvas, mBean);
    }

    private Paint paint = null;

    private void drawWave(Canvas canvas, @NonNull WaveFormInfo bean) {

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int dataPixel = 0; //计算出来最近的数组指针
        float axisX = 0;
        int finalAxisX = -1;

        while (axisX < width) {
            if (dataPixel < 0 || dataPixel >= bean.getLength()) {
                break;
            }

            int nearestAxisX = (int) axisX;
            if (nearestAxisX != finalAxisX) {
                finalAxisX = nearestAxisX;

                float pixel = bean.getPixelData(dataPixel);

                paint = dataPixel >= mThumbStartTimePixel && dataPixel <= mThumbEndTimePixel
                        ? mWaveFormHighLightPaint : mWaveFormPaint;

                canvas.drawLine(finalAxisX, height / 2f - 1, finalAxisX, height / 2f - (pixel / Short.MAX_VALUE) * height / 2, paint);
                canvas.drawLine(finalAxisX, height / 2f + 1, finalAxisX, height / 2f + (pixel / Short.MAX_VALUE) * height / 2, paint);
            }

            axisX += mThumbScale;
            dataPixel++;
        }
    }

    public void updateThumbTime(long thumbStartTime, long thumbEndTime) {
        if (mBean == null) {
            return;
        }
        mThumbStartTime = thumbStartTime;
        mThumbDuration = thumbEndTime - thumbStartTime;
        int sampleRate = mBean.getSample_rate();
        int samplesPerPixel = mBean.getSamples_per_pixel();
        mThumbStartTimePixel =
                WaveUtil.timeToPixels(thumbStartTime, sampleRate, samplesPerPixel, 1f);
        mThumbEndTimePixel =
                WaveUtil.timeToPixels(thumbEndTime, sampleRate, samplesPerPixel, 1f);
        float thumbRectLeft = mThumbStartTimePixel * mThumbScale;
        float thumbRectRight = mThumbEndTimePixel * mThumbScale;
        mDragDetector.setEnableRect(thumbRectLeft, 0, thumbRectRight, getHeight());

        invalidate();
    }

    @Override
    public void onDrag(float dx, float dy) {
        if (mBean == null) {
            return;
        }

        int sampleRate = mBean.getSample_rate();
        int samplesPerPixel = mBean.getSamples_per_pixel();

        mThumbStartTime += WaveUtil.pixelsToTime(dx, sampleRate, samplesPerPixel, mThumbScale);

        // 右边界
        if (mThumbStartTime + mThumbDuration > mTotalTime) {
            mThumbStartTime = mTotalTime - mThumbDuration;
        }

        // 左边界
        if (mThumbStartTime < 0) {
            mThumbStartTime = 0;
        }

        if (mOnDragThumbListener != null) {
            mOnDragThumbListener.onDrag(mThumbStartTime);
        }
    }

    public void setOnDragThumbListener(OnDragThumbListener onDragThumbListener) {
        mOnDragThumbListener = onDragThumbListener;
    }

    public interface OnDragThumbListener {
        void onDrag(long startTime);
    }
}
