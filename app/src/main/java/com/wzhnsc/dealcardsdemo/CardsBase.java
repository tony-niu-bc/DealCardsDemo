package com.wzhnsc.dealcardsdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

public class CardsBase extends FrameLayout {
    public CardsContainer mParentView;

    // Spring 通过可设置的摩擦力(Friction)和张力(tension)实现了胡克定律，通过代码模拟了物理场景。
    private Spring mSpringX;
    private Spring mSpringY;

    public CardsBase(Context context) {
        this(context, null);
    }

    public CardsBase(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardsBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSpring();
    }

    private void initSpring() {
        SpringConfig springConfig  = SpringConfig.fromBouncinessAndSpeed(15, 20);
        SpringSystem mSpringSystem = SpringSystem.create();

        mSpringX = mSpringSystem.createSpring().setSpringConfig(springConfig);
        mSpringY = mSpringSystem.createSpring().setSpringConfig(springConfig);

        // 每个 Spring 内部都维护着一个 SpringListener 数组
        // onSpringActivate       在首次开始运动时候调用。
        // onSpringUpdate         在 advance 后调用，表示状态更新。
        //                        SpringSystem 会遍历由其管理的所有 Spring 实例，对它们进行 advance 。
        // onSpringAtRest         在进入 rest 状态后调用。
        // onSpringEndStateChange 则略有不同，
        //                        仅在 setEndValue 中被调用，且该 Spring 需要在运动中且新的 endValue 不等于原 endValue 。
        mSpringX.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                CardsBase.this.offsetLeftAndRight(((int)spring.getCurrentValue()) - getLeft());
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                mParentView.adjustChildrenPosition();
            }
        });

        mSpringY.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                CardsBase.this.offsetTopAndBottom(((int)spring.getCurrentValue()) - getTop());
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                mParentView.adjustChildrenPosition();
            }
        });
    }

    // 移动到指定位置后带有晃动效果
    public void animTo(int xPos, int yPos) {
        // 设置当前spring位置
        mSpringX.setCurrentValue(getLeft());
        mSpringY.setCurrentValue(getTop());

        mSpringX.setEndValue(xPos);
        mSpringY.setEndValue(yPos);
    }

    public void setParentView(CardsContainer parentView) {
        mParentView = parentView;
    }

    public void setAtRest() {
        mSpringX.setAtRest();
        mSpringY.setAtRest();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 触点需按在本视图且不能为填充区域时
            if (((int)ev.getX() > (getLeft()  + getPaddingLeft()))
             && ((int)ev.getX() < (getRight() - getPaddingRight()))
             && ((int)ev.getY() > (getTop()    + getPaddingTop()))
             && ((int)ev.getY() < (getBottom() - getPaddingBottom()))) {
                // 让其父容器的父容器不要拦截触摸事件
                mParentView.getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        return super.dispatchTouchEvent(ev);
    }
}
