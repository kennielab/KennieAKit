package com.kennie.indexbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * project : KennieAKit
 * class_name :  LKLetterIndexBar
 * author : Kennie
 * date : 2022/1/16 19:16
 * desc : 字母侧边栏
 */
public class LKLetterIndexBar extends View {private static final String TAG = LKLetterIndexBar.class.getSimpleName();

//    public static final String[] DEFAULT_LETTERS = {"↑", "★", "#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
//            , "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    public static final String[] DEFAULT_LETTERS = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
            , "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private String[] mLetterArray = DEFAULT_LETTERS; // 字母表

    private int selectIndex = -1; // 当前选中的字母位置
    private int mOldPosition;
    private int mNewPosition;

    private Paint mLettersPaint = new Paint(); // 字母列表画笔
    private Paint mTextPaint = new Paint(); // 提示字母画笔
    private Paint mWavePaint = new Paint(); // 波浪效果画笔


    private float mIndexTextSize; // 索引文字大小
    private int mIndexTextColor; // 索引文字颜色

    private float mLargeTextSize;

    private int mWaveColor;
    private int mTextColorChoose;
    private int mWidth; // 字符所在区域宽度
    private int mHeight; // 字符所在区域高度
    private int mItemHeight; // 单个字母的高度
    private int mPadding;

    // 计算波浪贝塞尔曲线的角弧长值
    private static final double ANGLE = Math.PI * 45 / 180;
    private static final double ANGLE_R = Math.PI * 90 / 180;

    // 圆形路径
    private Path mBallPath = new Path();

    // 手指滑动的Y点作为中心点
    private int mCenterY; //中心点Y

    // 贝塞尔曲线的分布半径
    private int mRadius;

    // 圆形半径
    private int mBallRadius;
    // 用于过渡效果计算
    ValueAnimator mRatioAnimator;

    // 用于绘制贝塞尔曲线的比率
    private float mRatio;

    // 选中字体的坐标
    private float mPosX, mPosY;

    // 圆形中心点X
    private float mBallCentreX;

    /**
     * 是否触摸字母(用来处理滚动反复刷新字母问题)
     */
    private boolean isDown = false;

    /**
     * 是否显示字母提示(默认不显示)
     */
    private boolean isTipOverlay = false;
    private TextView overlayTextView; // 覆盖字母提示


    private OnLetterIndexChangeListener mListener;


    public LKLetterIndexBar(Context context) {
        this(context, null);
    }

    public LKLetterIndexBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LKLetterIndexBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        //mLetters = context.getResources().getStringArray(R.array.Letters);

        mIndexTextSize = context.getResources().getDimensionPixelSize(R.dimen.textSize_sidebar);
        mIndexTextColor = Color.GRAY;

        mWaveColor = Color.parseColor("#be2580D5");

        mTextColorChoose = context.getResources().getColor(android.R.color.black);
        mLargeTextSize = context.getResources().getDimensionPixelSize(R.dimen.large_textSize_sidebar);
        mPadding = context.getResources().getDimensionPixelSize(R.dimen.textSize_sidebar_padding);
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.LKLetterIndexBar);
            mIndexTextSize = typedArray.getFloat(R.styleable.LKLetterIndexBar_indexTextSize, mIndexTextSize);
            mIndexTextColor = typedArray.getColor(R.styleable.LKLetterIndexBar_indexTextColor, mIndexTextColor);

            mTextColorChoose = typedArray.getColor(R.styleable.LKLetterIndexBar_sidebarChooseTextColor, mTextColorChoose);
            mLargeTextSize = typedArray.getFloat(R.styleable.LKLetterIndexBar_sidebarLargeTextSize, mLargeTextSize);
            mWaveColor = typedArray.getColor(R.styleable.LKLetterIndexBar_sidebarBackgroundColor, mWaveColor);
            mRadius = typedArray.getInt(R.styleable.LKLetterIndexBar_sidebarRadius, context.getResources().getDimensionPixelSize(R.dimen.radius_sidebar));
            mBallRadius = typedArray.getInt(R.styleable.LKLetterIndexBar_sidebarBallRadius, context.getResources().getDimensionPixelSize(R.dimen.ball_radius_sidebar));
            typedArray.recycle();
        }

        mWavePaint = new Paint();
        mWavePaint.setAntiAlias(true);
        mWavePaint.setStyle(Paint.Style.FILL);
        mWavePaint.setColor(mWaveColor);

        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mTextColorChoose);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(mLargeTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mItemHeight = (mHeight - mPadding) / mLetterArray.length;
        mPosX = mWidth - 1.6f * mIndexTextSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制背景
        drawBackground(canvas);
        // 绘制字母列表
        drawLetters(canvas);
        // 绘制圆
        if (isTipOverlay()) {
            drawBallPath(canvas);
        }

        // 绘制选中的字体
        drawChooseText(canvas);

    }

    private void drawBackground(Canvas canvas) {
        RectF rectF = new RectF();
        rectF.left = mPosX - mIndexTextSize;
        rectF.right = mPosX + mIndexTextSize;
        rectF.top = mIndexTextSize / 2;
        rectF.bottom = mHeight - mIndexTextSize / 2;

        // 绘制背景
        mLettersPaint.reset();
        mLettersPaint.setStyle(Paint.Style.FILL);
        mLettersPaint.setColor(Color.parseColor("#F9F9F9"));
        mLettersPaint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, mIndexTextSize, mIndexTextSize, mLettersPaint);

        // 绘制背景边框
        mLettersPaint.reset();
        mLettersPaint.setStyle(Paint.Style.STROKE);
        mLettersPaint.setColor(mIndexTextColor);
        mLettersPaint.setAntiAlias(true);
        canvas.drawRoundRect(rectF, mIndexTextSize, mIndexTextSize, mLettersPaint);
    }

    private void drawLetters(Canvas canvas) {

        for (int i = 0; i < mLetterArray.length; i++) {
            mLettersPaint.reset();
            mLettersPaint.setColor(mIndexTextColor);
            mLettersPaint.setAntiAlias(true);
            mLettersPaint.setTextSize(mIndexTextSize);
            mLettersPaint.setTextAlign(Paint.Align.CENTER);

            Paint.FontMetrics fontMetrics = mLettersPaint.getFontMetrics();
            float baseline = Math.abs(-fontMetrics.bottom - fontMetrics.top);

            float posY = mItemHeight * i + baseline / 2 + mPadding;

            if (i == selectIndex) {
                mPosY = posY;
            } else {
                canvas.drawText(mLetterArray[i], mPosX, posY, mLettersPaint);
            }
        }

    }

    private void drawChooseText(Canvas canvas) {
        if (selectIndex != -1) {
            // 绘制右侧选中字符
            mLettersPaint.reset();
            mLettersPaint.setColor(Color.parseColor("#07C160"));
            mLettersPaint.setTextSize(mIndexTextSize);
            mLettersPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(mLetterArray[selectIndex], mPosX, mPosY, mLettersPaint);
        }
    }


    private void drawBallPath(Canvas canvas) {
        //x轴的移动路径
        mBallCentreX = (mWidth + mBallRadius) - (2.0f * mRadius + 2.0f * mBallRadius) * mRatio;
        mBallPath.reset();
        mBallPath.addCircle(mBallCentreX, mCenterY, mBallRadius, Path.Direction.CW);
        mBallPath.close();
        canvas.drawPath(mBallPath, mWavePaint);

        if (selectIndex != -1) {
            // 绘制提示字符
            if (mRatio >= 0.9f) {
                String target = mLetterArray[selectIndex];
                Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
                float baseline = Math.abs(-fontMetrics.bottom - fontMetrics.top);
                float x = mBallCentreX;
                float y = mCenterY + baseline / 2;
                canvas.drawText(target, x, y, mTextPaint);
            }
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float y = event.getY();
        final float x = event.getX();

        mOldPosition = selectIndex;
        mNewPosition = (int) (y / mHeight * mLetterArray.length);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (x < mWidth - 2 * mRadius) {
                    return false;
                }
                mCenterY = (int) y;
                if (null != mListener) {
                    mListener.onTouched(true);
                }
                if (isTipOverlay()) {
                    startAnimator(mRatio, 1.0f);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // 开始触摸
                mCenterY = (int) y;
                // 手指滑动
                if (mOldPosition != mNewPosition) {
                    if (mNewPosition >= 0 && mNewPosition < mLetterArray.length) {
                        selectIndex = mNewPosition;
                        if (null != mListener && !TextUtils.isEmpty(mLetterArray[mNewPosition])) {
                            // 显示提示字母View
                            if (!isTipOverlay()) {
                                if (overlayTextView != null) {
                                    overlayTextView.setVisibility(VISIBLE);
                                    overlayTextView.setText(mLetterArray[mNewPosition]);
                                }
                            }

                            mListener.onLetterChanged(mLetterArray[mNewPosition], mNewPosition);
                        }
                    }
                }
                invalidate();
                //改变标记状态
                isDown = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 手指抬起
                if (null != mListener) {
                    mListener.onTouched(false);
                }
                // 关闭波浪效果
                if (isTipOverlay()) {
                    startAnimator(mRatio, 0f);
                } else {
                    // 隐藏提示字母View
                    if (overlayTextView != null) {
                        overlayTextView.setVisibility(GONE);
                    }
                }
                selectIndex = -1;
                if (null != mListener) {
                    if (mNewPosition >= 0 && mNewPosition < mLetterArray.length) {
                        //mListener.onLetterClosed(mNewPosition, mLetterArray[mNewPosition]);
                    }
                    //改变标记状态
                    isDown = false;
                }

                break;
            default:
                break;
        }
        return true;
    }


    private void startAnimator(float... value) {
        if (mRatioAnimator == null) {
            mRatioAnimator = new ValueAnimator();
        }
        mRatioAnimator.cancel();
        mRatioAnimator.setFloatValues(value);
        mRatioAnimator.addUpdateListener(value1 -> {
            mRatio = (float) value1.getAnimatedValue();
            //球弹到位的时候，并且点击的位置变了，即点击的时候显示当前选择位置
            if (mRatio == 1f && mOldPosition != mNewPosition) {
                if (mNewPosition >= 0 && mNewPosition < mLetterArray.length) {
                    selectIndex = mNewPosition;
                    if (mListener != null) {
                        mListener.onLetterChanged(mLetterArray[mNewPosition], mNewPosition);
                    }
                }
            }
            invalidate();
        });
        mRatioAnimator.start();
    }


    /**
     * @param listener
     */
    public void setOnLetterChangeListener(OnLetterIndexChangeListener listener) {
        this.mListener = listener;
    }


    /**
     * 设置页面滑动更新字母
     *
     * @param letter 当前字母
     */
    public void onItemScrollUpdateLetter(String letter) {
        //手指没触摸才调用
        if (!isDown) {
            Log.i(TAG, "onItemScrollUpdateLetter:" + letter);
            for (int i = 0; i < mLetterArray.length; i++) {
                if (mLetterArray[i].equals(letter) && selectIndex != i) {
                    selectIndex = i;
                    invalidate();
                }
            }
        }

    }

    public boolean isTipOverlay() {
        return isTipOverlay;
    }

    public void setTipOverlay(boolean tipOverlay) {
        isTipOverlay = tipOverlay;
    }


    public LKLetterIndexBar setOverlayTextView(TextView overlay) {
        this.overlayTextView = overlay;
        return this;
    }


    /**
     * 自定义侧边字母
     *
     * @param letters 字母集合
     */
    public void setLetterArray(String[] letters) {
        this.mLetterArray = letters;
        requestLayout();
        invalidate();
    }

//    public static class Builder {
//
//        private LKLetterIndexBar letterIndexBar;
//        private Context context;
//
//        public Builder(Context context) {
//            this.letterIndexBar = new LKLetterIndexBar(context);
//            this.context = context;
//        }
//
//        /**
//         * 设置索引文字大小
//         *
//         * @param indexTextSize 索引文字大小
//         * @return
//         */
//        public Builder setIndexTextSize(float indexTextSize) {
//            letterIndexBar.mIndexTextSize = indexTextSize;
//            return this;
//        }
//
//        /**
//         * 设置索引文字大小
//         *
//         * @param dimenId
//         * @return
//         */
//        public Builder setIndexTextSize(int dimenId) {
//            letterIndexBar.mIndexTextSize = this.context.getResources().getDimensionPixelSize(dimenId);
//            return this;
//        }
//
//        public LKLetterIndexBar build() {
//            return this.letterIndexBar;
//        }
//    }

}