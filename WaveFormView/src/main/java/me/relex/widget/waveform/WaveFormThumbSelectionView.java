package me.relex.widget.waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import me.relex.waveformview.R;


/**
 * Copyright (C), 2021-2099
 *
 * @author Cooper
 * History:
 * author - date - version - desc
 * Cooper 2022/9/28 17:08 1  可以展示选择框的预览view
 */
public class WaveFormThumbSelectionView extends WaveFormThumbView {
    public WaveFormThumbSelectionView(Context context) {
        super(context);
        init(context, null);
    }

    public WaveFormThumbSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaveFormThumbSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public WaveFormThumbSelectionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private int container_color = Color.YELLOW;

    private void init(Context context, @Nullable AttributeSet attrs) {
        int container_size = 2;

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveFormThumbSelectionView);
            container_color =
                    typedArray.getColor(R.styleable.WaveFormThumbSelectionView_wfs_container_color, Color.YELLOW);
            container_size = typedArray.getDimensionPixelSize(R.styleable.WaveFormThumbSelectionView_wfs_container_size, 2);
            typedArray.recycle();
        }

        handlerPaint = new Paint();
        handlerPaint.setColor(container_color);
        handlerPaint.setStyle(Paint.Style.STROKE);
        handlerPaint.setStrokeWidth(container_size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBean == null) {
            return;
        }
        drawHandler(canvas, mBean);
    }

    private long mHandlerStartTime = 0;
    private long mHandlerEndTime = 0;
    private Paint handlerPaint = new Paint();

    private void drawHandler(Canvas canvas, @NonNull WaveFormInfo info) {
        int height = getMeasuredHeight();

        int startX = (int) (info.getSamplePerMs() * mHandlerStartTime * mThumbScale);
        int endX = (int) (info.getSamplePerMs() * mHandlerEndTime * mThumbScale);
        canvas.drawRect(startX, handlerPaint.getStrokeWidth() / 2, endX, height - handlerPaint.getStrokeWidth() / 2, handlerPaint);
    }

    /**
     * 更新选择框的起始结束时间
     *
     * @param startMillisecond 开始时间，毫秒
     * @param endMillisecond   结束时间，毫秒
     */
    public void updateSelectionTime(long startMillisecond, long endMillisecond) {
        mHandlerStartTime = startMillisecond;
        mHandlerEndTime = endMillisecond;
        invalidate();
    }
}
