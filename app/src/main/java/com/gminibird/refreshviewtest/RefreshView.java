package com.gminibird.refreshviewtest;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

public class RefreshView extends View {

    private final int LEFT = 0;
    private final int RIGHT = 1;
    private final int CENTER = 2;
    //dp
    private final int WIDTH = 60;               //wrap_content 默认宽度(dp)
    private final int HEIGHT = 40;              //wrap_content 默认高度(dp)
    private final int ORIGIN_MAX_RADIUS = 7;    //圆的最大半径
    private final int ORIGIN_MIN_RADIUS = 5;    //圆的最小半径
    //px
    private int mContentWidth;                  //view内容宽度
    private int mContentHeight;                 //view内容高度
    private int mGap;                           //相邻圆的圆心间隔
    private int mWidth;                         //wrap_content 默认宽度(px)
    private int mHeight;                        //wrap_content 默认高度(px)
    private int mMaxRadius;                     //圆最大半径
    private int mMinRadius;                     //圆最小半径

    public static final int STATE_ORIGIN = 0;
    public static final int STATE_PREPARED = 1;
    private int mOriginState = STATE_ORIGIN;
    private boolean isStateInitialized = false;

    //圆属性装载器
    private List<Circle> mCircles = new ArrayList<>();
    private Paint mPaint;
    private Animator mAnimator;

    public RefreshView(Context context) {
        super(context);
        init();
    }

    public RefreshView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 设置圆球初始状态
     * {@link #STATE_ORIGIN}为原始状态（三个小球重合）,
     * {@link #STATE_PREPARED}为准备好可以刷新的状态，三个小球间距最大
     */
    public void setOriginState(int state) {
        if (state == 0) {
            mOriginState = STATE_ORIGIN;
        } else {
            mOriginState = STATE_PREPARED;
        }
    }

    private void init() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float density = metrics.density;
        mMaxRadius = (int) (density * ORIGIN_MAX_RADIUS);
        mMinRadius = (int) (density * ORIGIN_MIN_RADIUS);
        mWidth = (int) (density * WIDTH);
        mHeight = (int) (density * HEIGHT);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        initAnimator();
    }

    private void initAnimator() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setRepeatCount(-1);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                prepareToStart();  //确保View达到可以刷新的状态
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                for (Circle circle : mCircles) {
                    updateCircle(circle, mCircles.indexOf(circle), animation.getAnimatedFraction());
                }
                postInvalidate();
            }
        });
        mAnimator = animator;
    }

    public void prepareToStart() {
        Circle circleLeft = mCircles.get(LEFT);
        circleLeft.x = mMinRadius;
        circleLeft.r = mMinRadius;
        Circle circleCenter = mCircles.get(CENTER);
        circleCenter.x = mContentWidth / 2;
        circleCenter.r = mMaxRadius;
        Circle circleRight = mCircles.get(RIGHT);
        circleRight.x = mContentWidth - mMinRadius;
        circleRight.r = mMinRadius;
        postInvalidate();
    }

    private void updateCircle(Circle circle, int index, float fraction) {
        float progress = fraction;  //真实进度
        float virtualFraction;      //每个小球内部的虚拟进度
        switch (index) {
            case LEFT:
                if (fraction < 5f / 6f) {
                    progress = progress + 1f / 6f;
                } else {
                    progress = progress - 5f / 6f;
                }
                break;
            case CENTER:
                if (fraction < 0.5f) {
                    progress = progress + 0.5f;
                } else {
                    progress = progress - 0.5f;
                }
                break;
            case RIGHT:
                if (fraction < 1f / 6f) {
                    progress += 5f / 6f;
                } else {
                    progress -= 1f / 6f;
                }
                break;
        }
        if (progress <= 1f / 6f) {
            virtualFraction = progress * 6;
            appear(circle, virtualFraction);
            return;
        }
        if (progress >= 5f / 6f) {
            virtualFraction = (progress - 5f / 6f) * 6;
            disappear(circle, virtualFraction);
            return;
        }
        virtualFraction = (progress - 1f / 6f) * 3f / 2f;
        move(circle, virtualFraction);
    }

    private void appear(Circle circle, float fraction) {
        circle.r = (int) (mMinRadius * fraction);
        circle.x = mMinRadius;
    }

    private void disappear(Circle circle, float fraction) {
        circle.r = (int) (mMinRadius * (1 - fraction));
    }

    private void move(Circle circle, float fraction) {
        int difference = mMaxRadius - mMinRadius;
        if (fraction < 0.5) {
            circle.r = (int) (mMinRadius + difference * fraction * 2);
        } else {
            circle.r = (int) (mMaxRadius - difference * (fraction - 0.5) * 2);
        }
        circle.x = (int) (mMinRadius + mGap * 2 * fraction);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        initContentAttr(getMeasuredWidth(), getMeasuredHeight());
        resetCircles();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (Circle circle : mCircles) {
            mPaint.setColor(circle.color);
            canvas.drawCircle(circle.x + getPaddingLeft(), circle.y + getPaddingTop(), circle.r, mPaint);
        }
    }

    public void start() {
        if (mAnimator != null && !mAnimator.isRunning()) {
            mAnimator.start();
        }
    }

    public void stop() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
            resetCircles();
        }
    }

    public void drag(float fraction) {
        if (mOriginState == STATE_PREPARED) {
            return;
        }
        if (mAnimator != null && mAnimator.isRunning()) {
            return;
        }
        if (fraction > 1) {
            return;
        }
        mCircles.get(LEFT).x = (int) (mMinRadius + mGap * (1f - fraction));
        mCircles.get(RIGHT).x = (int) (mContentWidth / 2 + mGap * fraction);
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(mWidth, heightSize);
        } else if (widthMeasureSpec == MeasureSpec.EXACTLY && heightMeasureSpec == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, mHeight);
        } else if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize);
        } else {
            setMeasuredDimension(mWidth, mHeight);
        }
    }

    private void initContentAttr(int width, int height) {
        mContentWidth = width - getPaddingLeft() - getPaddingRight();
        mContentHeight = height - getPaddingTop() - getPaddingBottom();
    }

    private void resetCircles() {
        if (mCircles.isEmpty()) {
            int x = mContentWidth / 2;
            int y = mContentHeight / 2;
            mGap = x - mMinRadius;   //初始化相邻圆心间的最大间距
            Circle circleLeft = new Circle(x, y, mMinRadius, 0xffff7f0a);
            Circle circleCenter = new Circle(x, y, mMaxRadius, Color.RED);
            Circle circleRight = new Circle(x, y, mMinRadius, Color.GREEN);
            mCircles.add(LEFT, circleLeft);
            mCircles.add(RIGHT, circleRight);
            mCircles.add(CENTER, circleCenter);
        }
        if (mOriginState == STATE_ORIGIN) {
            int x = mContentWidth / 2;
            int y = mContentHeight / 2;
            for (int i = 0; i < mCircles.size(); i++) {
                Circle circle = mCircles.get(i);
                circle.x = x;
                circle.y = y;
                if (i == CENTER) {
                    circle.r = mMaxRadius;
                } else {
                    circle.r = mMinRadius;
                }
            }
        } else {
            prepareToStart();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    class Circle {
        int x;
        int y;
        int r;
        int color;

        public Circle(int x, int y, int r, int color) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.color = color;
        }
    }
}