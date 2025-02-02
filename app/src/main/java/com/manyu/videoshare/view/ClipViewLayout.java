package com.manyu.videoshare.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import com.manyu.videoshare.R;

import java.io.IOException;


/**
 * 博客地址：http://blog.csdn.net/gdutxiaoxu
 *
 * @author xujun
 * @time 19-4-26
 */
public class ClipViewLayout extends RelativeLayout {

    private static final String TAG = "ClipViewLayout";

    //裁剪原图
    private ImageView mImageView;
    //裁剪框
    private ClipView mClipView;
    //裁剪框水平方向间距，xml布局文件中指定
    private float mHorizontalPadding;
    //裁剪框垂直方向间距，计算得出
    private float mVerticalPadding;
    //图片缩放、移动操作矩阵
    private Matrix mMatrix = new Matrix();
    //图片原来已经缩放、移动过的操作矩阵
    private Matrix mSavedMatrix = new Matrix();
    //动作标志：无
    private static final int NONE = 0;
    //动作标志：拖动
    private static final int DRAG = 1;
    //动作标志：缩放
    private static final int ZOOM = 2;
    //初始化动作标志
    private int mode = NONE;
    //记录起始坐标
    private PointF mStart = new PointF();
    //记录缩放时两指中间点坐标
    private PointF mMid = new PointF();
    private float mOldDist = 1f;
    //用于存放矩阵的9个值
    private final float[] mMatrixValues = new float[9];
    //最小缩放比例
    private float mMinScale;
    //最大缩放比例
    private float mMaxScale = 4;

    private int mPreViewW;
    private int mWidthPixels;
    private int mHeightPixels;


    public ClipViewLayout(Context context) {
        this(context, null);
    }

    public ClipViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //初始化控件自定义的属性
    public void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ClipViewLayout);

        //获取剪切框距离左右的边距, 默认为50dp
        mHorizontalPadding = array.getDimensionPixelSize(R.styleable.ClipViewLayout_horizontalPadding,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics()));
        //获取裁剪框边框宽度，默认1dp
        int clipBorderWidth = array.getDimensionPixelSize(R.styleable.ClipViewLayout_clipBorderWidth,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        //裁剪框类型(圆或者矩形)
        int clipType = array.getInt(R.styleable.ClipViewLayout_clipType, 2);

        //回收
        array.recycle();


        mClipView = new ClipView(context);
        mClipView.setClipType(clipType);

        //设置剪切框边框
        mClipView.setClipBorderWidth(clipBorderWidth);
        //设置剪切框水平间距
        mClipView.setmHorizontalPadding(mHorizontalPadding);
        mImageView = new ImageView(context);
        //相对布局布局参数
        android.view.ViewGroup.LayoutParams lp = new LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mImageView, lp);
        this.addView(mClipView, lp);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mWidthPixels = displayMetrics.widthPixels;
        mHeightPixels = displayMetrics.heightPixels;
        mPreViewW = (int) (mWidthPixels - mHorizontalPadding * 2);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onTouchEvent: ACTION_DOWN");
                mSavedMatrix.set(mMatrix);
                //设置开始点位置
                mStart.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //开始放下时候两手指间的距离
                mOldDist = spacing(event);
                if (mOldDist > 10f) {
                    mSavedMatrix.set(mMatrix);
                    midPoint(mMid, event);
                    mode = ZOOM;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: mode =" + mode);
                if (mode == DRAG) { //拖动
                    mMatrix.set(mSavedMatrix);
                    float dx = event.getX() - mStart.x;
                    float dy = event.getY() - mStart.y;

                    mVerticalPadding = mClipView.getClipRect().top;
                    mMatrix.postTranslate(dx, dy);
                    //检查边界
                    checkBorder();
                } else if (mode == ZOOM) { //缩放
                    //缩放后两手指间的距离
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        //手势缩放比例
                        float scale = newDist / mOldDist;
                        if (scale < 1) { //缩小
                            if (getScale() > mMinScale) {
                                mMatrix.set(mSavedMatrix);
                                mVerticalPadding = mClipView.getClipRect().top;
                                mMatrix.postScale(scale, scale, mMid.x, mMid.y);
                                //缩放到最小范围下面去了，则返回到最小范围大小
                                while (getScale() < mMinScale) {
                                    //返回到最小范围的放大比例
                                    scale = 1 + 0.01F;
                                    mMatrix.postScale(scale, scale, mMid.x, mMid.y);
                                }
                            }
                            //边界检查
                            checkBorder();
                        } else { //放大
                            if (getScale() <= mMaxScale) {
                                mMatrix.set(mSavedMatrix);
                                mVerticalPadding = mClipView.getClipRect().top;
                                mMatrix.postScale(scale, scale, mMid.x, mMid.y);
                            }
                        }
                    }
                }
                mImageView.setImageMatrix(mMatrix);
                break;
        }
        return true;
    }

    /**
     * 根据当前图片的Matrix获得图片的范围
     */
    private RectF getMatrixRectF(Matrix matrix) {
        RectF rect = new RectF();
        Drawable d = mImageView.getDrawable();
        if (null != d) {
            rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }

    /**
     * 边界检测
     */
    private void checkBorder() {
        RectF rect = getMatrixRectF(mMatrix);
        float deltaX = 0;
        float deltaY = 0;
        int width = mImageView.getWidth();
        int height = mImageView.getHeight();
        // 如果宽或高大于屏幕，则控制范围 ; 这里的0.001是因为精度丢失会产生问题，但是误差一般很小，所以我们直接加了一个0.01
        if (rect.width() + 0.01 >= width - 2 * mHorizontalPadding) {
            // 图片矩阵的最左边 > 裁剪边框的左边
            if (rect.left > mHorizontalPadding) {
                deltaX = -rect.left + mHorizontalPadding;
            }
            // 图片矩阵的最右边 < 裁剪边框的右边
            if (rect.right < width - mHorizontalPadding) {
                deltaX = width - mHorizontalPadding - rect.right;
            }
        }
        if (rect.height() + 0.01 >= height - 2 * mVerticalPadding) {
            // 图片矩阵的 top > 裁剪边框的 top
            if (rect.top > mVerticalPadding) {
                deltaY = -rect.top + mVerticalPadding;
            }
            // 图片矩阵的 bottom < 裁剪边框的 bottom
            if (rect.bottom < height - mVerticalPadding) {
                deltaY = height - mVerticalPadding - rect.bottom;
            }
        }

        Log.i(TAG, "checkBorder: deltaX=" + deltaX + " deltaY = " + deltaY);

        mMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 获得当前的缩放比例
     */
    public final float getScale() {
        mMatrix.getValues(mMatrixValues);
        return mMatrixValues[Matrix.MSCALE_X];
    }

    public void setClipType(int clipType) {
        if (mClipView != null) {
            mClipView.setClipType(clipType);
        }
    }


    /**
     * 多点触控时，计算最先放下的两指距离
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 多点触控时，计算最先放下的两指中心坐标
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }


}
