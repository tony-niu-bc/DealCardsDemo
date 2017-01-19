package com.wzhnsc.dealcardsdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class CardsContainer extends ViewGroup {
    // 容器的宽度
    public int mContainerWidth = 0;
    // 容器的高度
    public int mContainerHeight = 0;

    public enum VanishTopChildMode {
        // 子视图向左飞出消失
        VANISHING_TYPE_TO_LEFT,
        // 子视图向右飞出消失
        VANISHING_TYPE_TO_RIGHT
    }

    // 从顶至底每一层的子视图
    private List<CardsBase> mChildrenViewList = new ArrayList<>();

    // 在v4的支持包中提供了此类来帮助我们方便的编写自定义ViewGroup来处理拖动
    private final ViewDragHelper mDragHelper;

    // 获取最顶部子视图原始位置坐标
    private int mTopChildRawLeft = 0;
    private int mTopChildRawTop = 0;

    // 获取最顶部子视图原始宽高
    private int mTopChildRawWidth = 0;
    private int mTopChildRawHight = 0;

    // 子视图所居位置（暂时只支持居左和居中）
    private int mGravity = Gravity.START;

    // 子视图间的叠加偏移量(可通过属性改变)
    private int mOverlapOffsetDist = 100;

    // 判断是否滑动的阈值(单位是像素)
    private int mTouchSlop = 5;

    // GestureDetector的替代版，存在于v4包中，更兼容更好用的手势识别工具类
    // 不需要对touch事件写一堆代码判断手势了
    private GestureDetectorCompat mGestureDetectorCompat;

    // 记录按下时触点的坐标位置
    private Point mPressedPoint = new Point();

    public CardsContainer(Context context) {
        this(context, null);
    }

    public CardsContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardsContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.container);
        mOverlapOffsetDist = (int)a.getDimension(R.styleable.container_overlapOffsetDist, mOverlapOffsetDist);
        mGravity           = a.getInt(R.styleable.container_android_gravity, -1);
        a.recycle();

        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        // GestureDetector 类向外提供了两个接口 ，
        // OnGuestureListener 与 OnDoubleTabListener，还实现了一个内部类SimpleOnGestureListener。
        // true if the GestureDetector.OnGestureListener consumed the event, else false.
        // 当你想识别一两个手势，而嫌弃重写六个方法太冗余的时候，SimpleOnGestureListener 就可以满足你的洁癖!
        // OnDoubleTabListener 接口主要回调双击事件，有下面的几个动作：
        // 单击事件(onSingleTapConfirmed):        系统等待一段时间后没有收到第二次点击事件则判定为单击。
        // 双击事件(onDoubleTap):                 连续点两次为双击。
        // 双击间隔时间发生的动作(onDoubleTapEvent):
        // OnGestureListener有下面的几个动作：
        // 按下(onDown):        刚刚手指接触到触摸屏的那一刹那，就是触的那一下。只有你想要忽略整个手势动作时，在onDown中返回false。
        // 抛掷(onFling):       手指在触摸屏上迅速移动，并松开的动作。
        // 长按(onLongPress):   手指按在持续一段时间，并且没有松开。
        // 按住(onShowPress):   手指按在触摸屏上，它的时间范围在按下起效，在长按之前。
        // 抬起(onSingleTapUp): 手指离开触摸屏的那一刹那。
        // 滚动(onScroll):      手指在触摸屏上滑动。
        mGestureDetectorCompat = new GestureDetectorCompat(context, new SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                // 如果有拖动不再往下传递此事件
                return (Math.abs(dy) + Math.abs(dx)) > mTouchSlop;
            }
        });
        mGestureDetectorCompat.setIsLongpressEnabled(false);

        mDragHelper = ViewDragHelper.create(this,
                                            // helper.mTouchSlop = (int)(helper.mTouchSlop * (1 / sensitivity));
                                            // 传入越大，mTouchSlop的值就会越小
                                            10f,
                                            new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                // 只捕获拖动最顶部的子视图
                if (mChildrenViewList.indexOf(child) != 0) {
                    return false;
                }

                // 触点需按在子视图且不能为填充区域
                if ((mPressedPoint.x < (getLeft()  + getPaddingLeft()))
                 || (mPressedPoint.x > (getRight() - getPaddingRight()))
                 || (mPressedPoint.y < (getTop()   + getPaddingTop()))
                 || (mPressedPoint.y > (getBottom() - getPaddingBottom()))) {
                    return false;
                }

                // 使子视图保持静止状态
                ((CardsBase)child).setAtRest();

                return true;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                // 能拖动的只可能是最顶层的子视图
                // 上层子视图位置改变，下层的位置也需要随着改变，
                // 只是上下左右移动的幅度不得：超过其上一层视图原来的位置，低过它自身原来的位置
                float rateLeftRight = Math.abs(dx) / (float)mOverlapOffsetDist;
                float rateTopBottom = Math.abs(dy) / (float)mOverlapOffsetDist;

                rateLeftRight = rateLeftRight > 1 ? 1 : rateLeftRight;
                rateTopBottom = rateTopBottom > 1 ? 1 : rateTopBottom;

                for (int i = 1; i < mChildrenViewList.size(); i++) {
                    View childView = mChildrenViewList.get(i);

                    int itsOverlapOffsetDist = mOverlapOffsetDist * i;

                    float newLeft = mTopChildRawLeft + itsOverlapOffsetDist;
                    float newTop  = mTopChildRawTop  + itsOverlapOffsetDist;

                    newLeft = newLeft - (itsOverlapOffsetDist * rateLeftRight);
                    newTop  = newTop  - (itsOverlapOffsetDist * rateTopBottom);

                    Log.d("DealCardDemo",
                          "onViewPositionChanged - " +
                          "i = " + i +
                          "\nrateLeftRight = " + rateLeftRight +
                          "\nrateTopBottom = " + rateTopBottom +
                          "\nnewLeft = " + newLeft +
                          "\nnewTop = " + newTop +
                          "\nitsOverlapOffsetDist = " + itsOverlapOffsetDist);

                    // 每个子视图的宽高都是一样的，所以这里用了最顶层子视图的宽高
                    childView.layout((int)newLeft,
                                     (int)newTop,
                                     (int)newLeft + mTopChildRawWidth,
                                     (int)newTop  + mTopChildRawHight);
                }
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                // 当前松手时的位置点到原始位置点的距离
                final int distX = releasedChild.getLeft() - mTopChildRawLeft;
                final int distY = releasedChild.getTop()  - mTopChildRawTop;

                // 最顶层子视图的右边点
                final int topChildRawRight = mTopChildRawLeft + mTopChildRawWidth;

                int destX = mTopChildRawLeft;
                int destY = mTopChildRawTop;

                // 垂直上下拖动不消失
                if (Math.abs(distY) < (Math.abs(distX) * 3)) {
                    // 向右超出容器一半宽度，则向右滑出消失
                    if (distX > (mTopChildRawWidth / 2)) {
                        destX = mContainerWidth;
                        destY = distY * topChildRawRight / distX + mTopChildRawTop;
                    }
                    // 向左超出容器一半宽度，则向左滑出消失
                    else if (distX < -(mTopChildRawWidth / 2)) {
                        destX = -mTopChildRawWidth;
                        destY = distY * topChildRawRight / -distX + mTopChildRawTop;
                    }

                    if (destY > mContainerHeight) {
                        destY = mContainerHeight;
                    }
                    else if (destY < -releasedChild.getHeight()) {
                        destY = -releasedChild.getHeight();
                    }
                }

                // 回退原始位置后带有晃动效果
                if (destX == mTopChildRawLeft) {
                    ((CardsBase)releasedChild).animTo(mTopChildRawLeft, mTopChildRawTop);
                }
                // 移出消失
                else {
                    if (mDragHelper.smoothSlideViewTo(releasedChild, destX, destY)) {
                        ViewCompat.postInvalidateOnAnimation(CardsContainer.this);
                    }
                }
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return top;
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        makeChildrenViewList();
    }

    @Override
    public void computeScroll() {
        // ViewDragHelper::smoothSlideViewTo 函数实现动画移动到指定位置
        // 就需要在 View::computeScroll 方法中加上如下处理代码
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        // 动画结束
        else {
            synchronized(this) {
                if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                    sortChildren();
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();

        // 按下时保存坐标信息
        if (action == MotionEvent.ACTION_DOWN) {
            mPressedPoint.x = (int)ev.getX();
            mPressedPoint.y = (int)ev.getY();
        }

        return super.dispatchTouchEvent(ev);
    }

    // 在 onInterceptTouchEvent 和 onTouchEvent 里接管触摸方法
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mDragHelper.processTouchEvent(e);
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth  = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(resolveSizeAndState(maxWidth,  widthMeasureSpec,  MeasureSpec.UNSPECIFIED),
                             resolveSizeAndState(maxHeight, heightMeasureSpec, MeasureSpec.UNSPECIFIED));

        mContainerWidth  = getMeasuredWidth();
        mContainerHeight = getMeasuredHeight();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // 获取最顶层子视图原始宽度
        mTopChildRawWidth = mChildrenViewList.get(0).getMeasuredWidth();
        mTopChildRawHight = mChildrenViewList.get(0).getMeasuredHeight();

        adjustChildrenPosition();
    }

    // 形成子视图索引列表
    private void makeChildrenViewList() {
        mChildrenViewList.clear();

        // 布局文件中后添加的子视图在显示时是在最上面的，
        // 此处是以 Z 轴序自上而下取得子视图
        for (int i = getChildCount(); i >= 0; i--) {
            View childView = getChildAt(i);

            if (childView instanceof CardsBase) {
                ((CardsBase)childView).setParentView(this);

                childView.setTag(i + 1);

                mChildrenViewList.add((CardsBase)childView);
            }
        }
    }

    // 对View重新排序
    private void sortChildren() {
        synchronized(this) {
            if (mTopChildRawLeft == mChildrenViewList.get(0).getLeft()) {
                return;
            }

            // 调整子视图顺次
            for (int i = mChildrenViewList.size() - 1; i > 0; i--) {
                mChildrenViewList.get(i).bringToFront();
            }

            makeChildrenViewList();

            adjustChildrenPosition();
        }
    }

    // 调整子视图的位置
    public void adjustChildrenPosition() {
        int parentLeft = getPaddingLeft();
        int parentTop  = getPaddingTop();

        if (Gravity.CENTER == mGravity) {
            // 叠层偏移延伸出的宽度
            int overlapOffsetWidth = mOverlapOffsetDist * (mChildrenViewList.size() - 1);

            parentLeft += (mContainerWidth  - getPaddingRight()  - mTopChildRawWidth - overlapOffsetWidth) / 2;
            parentTop  += (mContainerHeight - getPaddingBottom() - mTopChildRawHight - overlapOffsetWidth) / 2;
        }

        for (int i = 0; i < mChildrenViewList.size(); i++) {
            View childView   = mChildrenViewList.get(i);

            Log.d("DealCardDemo",
                  "adjustChildrenPosition - " +
                  "\ni = " + i +
                  "\nparentLeft = " + parentLeft +
                  "\nparentTop = " + parentTop);

            // 每个子视图的宽高都是一样的，所以这里用了最顶层子视图的宽高
            childView.layout(parentLeft, parentTop, parentLeft + mTopChildRawWidth, parentTop + mTopChildRawHight);

            childView.offsetLeftAndRight(mOverlapOffsetDist * i);
            childView.offsetTopAndBottom(mOverlapOffsetDist * i);

            // 按比例缩小
//            final float scale = 1 - 0.06f * i;
//            childView.setScaleX(scale);
//            childView.setScaleY(scale);
        }

        // 获取最顶层子视图原始位置坐标
        mTopChildRawLeft = mChildrenViewList.get(0).getLeft();
        mTopChildRawTop  = mChildrenViewList.get(0).getTop();
    }

    // 外部控制最顶层子视图消失
    public void vanishTopChild(VanishTopChildMode vtcMode) {
        synchronized(this) {
            View topChild = mChildrenViewList.get(0);

            if (mDragHelper.smoothSlideViewTo(topChild,
                                              (VanishTopChildMode.VANISHING_TYPE_TO_LEFT == vtcMode) ? -mContainerWidth : mContainerWidth,
                                              mContainerHeight)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }
}