package me.relex.widget.waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import me.relex.waveformview.R;


/**
 * Copyright (C), 2021-2099
 *
 * @author Cooper
 * History:
 * author - date - version - desc
 * Cooper 2022/9/28 16:39 1  支持截取的波形view
 */
public class WaveFormSelectionView extends WaveFormView {
    public WaveFormSelectionView(Context context) {
        super(context);
        init(context, null);
    }

    public WaveFormSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WaveFormSelectionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public WaveFormSelectionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Nullable
    private WaveFormSelectionViewListener mListener;

    public void setWaveFormSelectionViewListener(@Nullable WaveFormSelectionViewListener listener) {
        mListener = listener;
    }

    //共用
    private Paint mPaint = new Paint();
    private final Path mPath = new Path();

    //选择
    private int mSelectionCursorWidth = 2;
    private float mSelectionIndicatorSize = 50f;
    private int mSelectionColor = Color.YELLOW;
    private int mSelectionMaskColor = Color.parseColor("#70756B3A");
    private long mSelectionStartTime = 0;
    private long mSelectionEndTime = 0;
    private final RectF mHandlerRectLeft = new RectF();
    private final RectF mHandlerRectRight = new RectF();

    //播放指示
    private int mPlayCursorWidth = 1;
    private float mPlayIndicatorSize = mSelectionIndicatorSize / 2;
    private int mPlayColor = Color.parseColor("#ffffff");
    private final RectF mHandlerRectPlayer = new RectF();

    private void init(Context context, @Nullable AttributeSet attrs) {

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveFormSelectionView);
            mSelectionCursorWidth = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_selection_cursor_width, 2);
            mSelectionIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_selection_indicator_size, 50);
            mSelectionColor = typedArray.getColor(R.styleable.WaveFormSelectionView_wfs_selection_color, Color.YELLOW);
            mSelectionMaskColor = typedArray.getColor(R.styleable.WaveFormSelectionView_wfs_selection_mask_color, Color.parseColor("#70756B3A"));
            mSelectionStartTime = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_selection_start_time, 1000);
            mSelectionEndTime = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_selection_end_time, 15000);

            mPlayCursorWidth = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_play_cursor_width, 1);
            mPlayIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.WaveFormSelectionView_wfs_play_indicator_size, 25);
            mPlayColor = typedArray.getColor(R.styleable.WaveFormSelectionView_wfs_play_color, Color.parseColor("#ffffff"));
            typedArray.recycle();
        }

        mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mWaveFormInfo == null) {
            return;
        }
        drawHandler(canvas, mWaveFormInfo);

        drawPlayCursor(canvas, mWaveFormInfo);
    }

    private void drawHandler(Canvas canvas, @NonNull WaveFormInfo info) {

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        long halfHandler = WaveUtil.pixelsToTime(mSelectionIndicatorSize / 2, info.getSample_rate(),
                info.getSamples_per_pixel(), mScale);

        if (mSelectionStartTime > mTotalTime) {
            mSelectionStartTime = mTotalTime;
        }
        if (mSelectionEndTime > mTotalTime) {
            mSelectionEndTime = mTotalTime;
        }

        //left handler，大于起始时间就显示
        if (mSelectionStartTime + halfHandler >= mStartTime) {
            long l = mSelectionStartTime - mStartTime;
            int i = (int) (info.getSamplePerMs() * l * mScale);

            //遮罩
            mPaint.setColor(mSelectionMaskColor);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect(0, 0, i, height, mPaint);

            //竖线
            mPaint.setColor(mSelectionColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mSelectionCursorWidth);
            canvas.drawLine(i, 0, i, height, mPaint);

            //三角
            mPath.reset();
            mPath.setFillType(Path.FillType.EVEN_ODD);
            mPath.moveTo(i - mSelectionIndicatorSize / 2, height);
            mPath.lineTo(i + mSelectionIndicatorSize / 2, height);
            mPath.lineTo(i, height - mSelectionIndicatorSize);
            mPath.lineTo(i - mSelectionIndicatorSize / 2, height);
            mPath.close();
            mPaint.setStrokeWidth(1);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(mPath, mPaint);

            //触发范围
            mHandlerRectLeft.set(i - mSelectionIndicatorSize, 0, i + mSelectionIndicatorSize, height);
        }

        //right handler，小于起始时间，显示遮罩。大于起始时间则正常显示
        if (mStartTime >= mSelectionEndTime || mSelectionEndTime + halfHandler >= mStartTime) {
            long r = mSelectionEndTime - mStartTime;
            int i = (int) (info.getSamplePerMs() * r * mScale) - 1;
            if (i < 0) {
                i++;
            }

            //遮罩
            mPaint.setColor(mSelectionMaskColor);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRect(i, 0, width, height, mPaint);

            //竖线
            mPaint.setColor(mSelectionColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mSelectionCursorWidth);
            canvas.drawLine(i, 0, i, height, mPaint);

            //三角
            mPath.reset();
            mPath.setFillType(Path.FillType.EVEN_ODD);
            mPath.moveTo(i - mSelectionIndicatorSize / 2, height);
            mPath.lineTo(i + mSelectionIndicatorSize / 2, height);
            mPath.lineTo(i, height - mSelectionIndicatorSize);
            mPath.lineTo(i - mSelectionIndicatorSize / 2, height);
            mPath.close();
            mPaint.setStrokeWidth(1);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(mPath, mPaint);

            //触发范围
            mHandlerRectRight.set(i - mSelectionIndicatorSize, 0, i + mSelectionIndicatorSize, height);
        }
    }

    private float downX = 0f;
    private long downTime = 0;

    private boolean mHandlerDraggingLeft = false;
    private boolean mHandlerDraggingRight = false;

    private boolean mHandlerDraggingPlayer = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mWaveFormInfo != null) {
            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }

                    cancelFling();
                    cancelAnimation();

                    downX = event.getX();
                    float downY = event.getY();

                    //判断优先拉左边还是优先拉右边。当左边距离开始的距离小于2倍的mHandlerTriangleSize，则右边优先，防止在最左边卡死
                    if (WaveUtil.timeToPixels(mSelectionStartTime, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale) < mSelectionIndicatorSize * 2) {
                        if (mHandlerRectRight.contains(downX, downY)) {
                            mHandlerDraggingLeft = false;
                            mHandlerDraggingRight = true;
                            downTime = mStartTime + WaveUtil.pixelsToTime(downX, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                            return true;
                        } else if (mHandlerRectLeft.contains(downX, downY)) {
                            mHandlerDraggingLeft = true;
                            mHandlerDraggingRight = false;
                            downTime = mStartTime + WaveUtil.pixelsToTime(downX, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                            return true;
                        }
                    } else { //否则左边优先
                        if (mHandlerRectLeft.contains(downX, downY)) {
                            mHandlerDraggingLeft = true;
                            mHandlerDraggingRight = false;
                            downTime = mStartTime + WaveUtil.pixelsToTime(downX, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                            return true;
                        } else if (mHandlerRectRight.contains(downX, downY)) {
                            mHandlerDraggingLeft = false;
                            mHandlerDraggingRight = true;
                            downTime = mStartTime + WaveUtil.pixelsToTime(downX, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                            return true;
                        }
                    }
                    //最后判断播放指针
                    if (mHandlerRectPlayer.contains(downX, downY)) {
                        mHandlerDraggingPlayer = true;

                        downTime = currentPlayingTime;

                        return true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mHandlerDraggingLeft) {
                        float dx = event.getX() - downX;
                        long time = WaveUtil.pixelsToTime(dx, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                        mSelectionStartTime = downTime + time;
                        if (mSelectionStartTime > mSelectionEndTime) {
                            mSelectionStartTime = mSelectionEndTime;
                        }
                        if (mSelectionStartTime < 0) {
                            mSelectionStartTime = 0;
                        }
                        if (mListener != null) {
                            mListener.onHandlerSelectionChanged(mSelectionStartTime, mSelectionEndTime);
                        }
                        invalidate();
                        return true;
                    } else if (mHandlerDraggingRight) {
                        float dx = event.getX() - downX;
                        long time = WaveUtil.pixelsToTime(dx, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                        mSelectionEndTime = downTime + time;
                        if (mSelectionEndTime < mSelectionStartTime) {
                            mSelectionEndTime = mSelectionStartTime;
                        }
                        if (mSelectionEndTime > mTotalTime) {
                            mSelectionEndTime = mTotalTime;
                        }
                        if (mListener != null) {
                            mListener.onHandlerSelectionChanged(mSelectionStartTime, mSelectionEndTime);
                        }
                        invalidate();
                        return true;
                    } else if (mHandlerDraggingPlayer) {
                        float dx = event.getX() - downX;
                        long time = WaveUtil.pixelsToTime(dx, mWaveFormInfo.getSample_rate(), mWaveFormInfo.getSamples_per_pixel(), mScale);
                        currentPlayingTime = downTime + time;
                        if (currentPlayingTime < 0) {
                            currentPlayingTime = 0;
                        }
                        if (currentPlayingTime > mTotalTime) {
                            currentPlayingTime = mTotalTime;
                        }
                        if (mListener != null) {
                            mListener.onPlayTimeSelectionChanged(currentPlayingTime);
                        }
                        invalidate();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ViewParent p = getParent();
                    if (p != null) {
                        p.requestDisallowInterceptTouchEvent(false);
                    }
                    mHandlerDraggingLeft = false;
                    mHandlerDraggingRight = false;
                    mHandlerDraggingPlayer = false;
                    downTime = 0;
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setWave(WaveFormInfo info) {
        super.setWave(info);

        if (mWaveFormInfo != null && mListener != null) {
            if (mSelectionEndTime < mSelectionStartTime) {
                mSelectionEndTime = mSelectionStartTime;
            }
            if (mSelectionEndTime > mTotalTime) {
                mSelectionEndTime = mTotalTime;
            }
            mListener.onHandlerSelectionChanged(mSelectionStartTime, mSelectionEndTime);
        }
    }

    private long currentPlayingTime = 0;

    private void drawPlayCursor(Canvas canvas, @NonNull WaveFormInfo info) {
        if (currentPlayingTime > 0) {
            if (currentPlayingTime > mTotalTime) {
                currentPlayingTime = mTotalTime;
            }

            int height = getMeasuredHeight();
            int p = WaveUtil.timeToPixels(currentPlayingTime - mStartTime, info.getSample_rate(), info.getSamples_per_pixel(), mScale);

            //竖线
            mPaint.setColor(mPlayColor);
            mPaint.setStrokeWidth(mPlayCursorWidth);
            canvas.drawLine(p, height / 4f, p, height / 4f * 3f, mPaint);

            //三角
            height = (int) (height / 4f * 3f);
            mPath.reset();
            mPath.setFillType(Path.FillType.EVEN_ODD);
            mPath.moveTo(p - mPlayIndicatorSize / 2, height);
            mPath.lineTo(p + mPlayIndicatorSize / 2, height);
            mPath.lineTo(p, height - mPlayIndicatorSize);
            mPath.lineTo(p - mPlayIndicatorSize / 2, height);
            mPath.close();
            mPaint.setStrokeWidth(1);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawPath(mPath, mPaint);

            //触发范围
            mHandlerRectPlayer.set(p - mPlayIndicatorSize, 0, p + mPlayIndicatorSize, height);
        }
    }

    /**
     * 更新播放位置
     *
     * @param millisecond 毫秒，大于0才显示
     */
    public void updatePlayCursorTime(long millisecond) {
        if (mWaveFormInfo == null) return;

        currentPlayingTime = millisecond;
        if (currentPlayingTime > mTotalTime) {
            currentPlayingTime = mTotalTime;
        }
        if (currentPlayingTime < 0) {
            currentPlayingTime = 0;
        }

        long time = WaveUtil.pixelsToTime(getMeasuredWidth(), mWaveFormInfo.getSample_rate(),
                mWaveFormInfo.getSamples_per_pixel(), mScale);
        long half = time / 2;

        if (currentPlayingTime >= mStartTime && currentPlayingTime <= (mStartTime + half)) { //大于开始时间，小于屏幕一半，开始时间不变
            invalidate();
        } else if (currentPlayingTime >= (mTotalTime - half)) { //剩余时间不够右侧半屏幕，开始时间设置为最大值：mTotalTime - time
            setStartTime(mTotalTime - time);
        } else { //否则播放时间居中
            setStartTime(Math.max(currentPlayingTime - half, 0));
        }
    }

    /**
     * 获取当前播放位置
     *
     * @return 位置
     */
    public long getCurrentPlayingTime() {
        return currentPlayingTime;
    }
}
