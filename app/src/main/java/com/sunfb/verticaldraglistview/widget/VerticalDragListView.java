package com.sunfb.verticaldraglistview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

public class VerticalDragListView extends FrameLayout {
    //第0个自孩子
    private View mMenuView;
    //这个View的目的就是为了兼容 ListView和RecyclerView
    //VerticalDragListView中的第1个子View -> "前面"
    private View mDragListView;
    /**
     * ViewDragHelper说明：https://www.cnblogs.com/shu94/p/12757399.html
     */
    private ViewDragHelper viewDragHelper;
    //判断菜单是否打开
    private boolean mMenuIsOpen = false;
    //记录手指按下的位置
    private float mDownY;
    //"后面" 菜单的高度
    private int mMenuHeight;

    public VerticalDragListView(@NonNull Context context) {
        this(context, null);
    }

    public VerticalDragListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalDragListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        viewDragHelper = ViewDragHelper.create(this, callback);
    }
    /**
     * 在setContentView()之后调用，即就是在解析XML文件之后调用，
     * 在解析XML文件之后，然后获取VerticalDragListView中的子View
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new RuntimeException("VerticalDragListView布局有且只有2个子View");
        }
        mMenuView = getChildAt(0);
        mDragListView = getChildAt(1);
    }
    //只要是在onMeasure()方法之后，super.onMeasure()之后去获取高度都是可以的，我们也可以在onLayout()方法中获取高度
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // 表示：如果布局改变了，都会去重新测量，然后摆放，就会获取菜单高度
        if (changed) {
            View menuView = getChildAt(0);  //获取后面菜单的View，即就是 "第0个" 子view
            mMenuHeight = menuView.getMeasuredHeight();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mMenuIsOpen) {
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = ev.getY();
                viewDragHelper.processTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = ev.getY();
                if (moveY - mDownY > 0 && !canChildScrollUp()) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //把onTouchEvent触摸事件交给mDragHelper来处理
        viewDragHelper.processTouchEvent(event);
        return true;

    }
    //1. 拖动我们的子View，也就是VerticalDragListView里边包裹的子布局
    ViewDragHelper.Callback callback = new ViewDragHelper.Callback() {
        /**
         * 当你拖动子view，这个方法肯定是要实现的，而且必须返回true，表示你要捕获你拖动的子view，接下来它的位置信息、拖动速度等一系列参数才会被记录，并返回给你。
         * @param child
         * @param pointerId
         * @return
         */
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            //指定该子View是否可以拖动,就是child。 返回true的目的就是我让VerticalDragListView里边包裹的所有子View都返回true
            //1.1  只能前面的控件拖动，后面的控件不能拖动
            //只能是让  "前面"  控件拖动 ，而不让 "后面" 控件拖动
            return child == mDragListView;
        }


        /**
         * 前面提过，你捕获控件后，helper会返回给你一些数据，这个方法返回给你的就是控件水平位置信息。 重点理解left这个值。写来写去，就这个left值迷惑人！！！请慢慢品味下面这句话：以child父布局的左上角顶点为坐标原点，以右为x轴正方向，下为y轴正方向， left值等于child水平方向划过的像素数（从左往右，像素数为正，反之为负）与它自身的mLeft值的和。撇开数值的结果，通俗的来讲就是你这次移动之后，child的水平位置应该在哪里！为什么是应该呢，因为这个方法走完，紧接着系统会拿该方法返回值作为view新的x坐标（即mLeft值）。那么在系统告诉你view移动后，应该所处的位置与你最终返回的位置之间，你可以做边界判断。例如：子view初始位置mLeft = 0，如果我将view向左滑动20px，那么此方法left就会返回给我-20，而我又不想拿这个值作为子view的新的x坐标。那我返回0，就可以让子view水平还在原位置。以下两个例子是left值的计算方法：
         *
         * 　　例子1：子view视图的左顶点就在父布局的坐标原点处，当你手指从左往右滑动10个像素，left就等于像素数+10 加上view距离坐标原点横坐标0，结果就是10；
         *
         * 　　例子2：父布局paddingleft 为20像素，如果单位是dp，最终也是转为px计算的。子view的mleft = 20，当你手指从右往左滑动10个像素，left就等于像素数-10+20=10；
         *
         * 　　left理解通透之后，dx就好说了，还记得刚提到的可正可负的像素数吗，哈哈，dx就是它！综上，可以得出一个公式：left = view.getLeft()+dx.
         * @param child
         * @param left
         * @param dx
         * @return
         */
        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            //super默认值是0
            return super.clampViewPositionHorizontal(child, left, dx);
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            //使用场景：卡片布局
            //1.3  垂直拖动的范围只能是后面的View的高度
            //每次垂直拖动，移动的位置
            if (top < 0) {
                top = 0;
            }
            if (top > mMenuHeight) {
                top = mMenuHeight;
            }
            return top;
        }

        /**
         * getMeasuredHeight()与getHeight的区别
         * 实际上在当屏幕可以包裹内容的时候，他们的值相等，
         *
         * 只有当view超出屏幕后，才能看出他们的区别：
         *
         * getMeasuredHeight()是实际View的大小，与屏幕无关，而getHeight的大小此时则是屏幕的大小。
         *
         * 当超出屏幕后，getMeasuredHeight()等于getHeight()加上屏幕之外没有显示的大小
         * @param releasedChild
         * @param xvel
         * @param yvel
         */
        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (mDragListView != releasedChild) return;
            //1.4  手指松开的时候两者选其一，要么打开要么关闭
            if (mDragListView.getTop() > mMenuView.getMeasuredHeight() / 2) {
                //打开
                mMenuIsOpen = true;
                viewDragHelper.settleCapturedViewAt(0, mMenuView.getMeasuredHeight());
            } else {
                //关闭
                mMenuIsOpen = false;
                viewDragHelper.settleCapturedViewAt(0, 0);
            }
            invalidate();
        }


    };

    /**
     * 响应滚动
     * 这个是直接复制网上的，目的是你拖动下边的View的时候 可以按照你自己拖动的高度自动滚动
     * 手指拖动高度如果大于mMenuHeight/2，则让打开；
     * 手指拖动高度如果小于mMenuHeight/2，则让关闭；
     */
    @Override
    public void computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            invalidate();
        }
        super.computeScroll();
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     * <p>
     * 可以判断ListView、RecyclerView、ScrollView
     * 这个方法是SwipeRefreshLayout中的源码：用于判断该View是否滚动到了最顶部，还能不能向上滚动
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mDragListView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mDragListView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mDragListView, -1) || mDragListView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mDragListView, -1);
        }
    }
}
