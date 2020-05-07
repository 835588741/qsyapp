package com.manyu.videoshare.view.TextWaterMark;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.manyu.videoshare.util.CalcUtil;
import com.manyu.videoshare.view.scraw.ScrawlBoardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 水印布局
 */
public class WaterMarkLayout extends RelativeLayout {

    // 水印组件列表
    private List<WaterMark> waterMarkList = new ArrayList<>();

    private ScrawlBoardView scrawlBoardView;
    // 当前的水印
    private WaterMark currentWaterMark;

    private boolean markTouchFlag = false;
    private boolean buttonTouchFlag = false;

    // 第一次触摸按下
    private float firstTouchX = 0;
    private float firstTouchY = 0;

    private float moveX = 0;
    private float moveY = 0;
    private float currentMarkCenterX;
    private float currentMarkCenterY;
    private boolean isAbleMove;

    private OnMarkerClickListener onMarkerClickListener;

    public WaterMarkLayout(Context context) {
        this(context, null);
    }

    public WaterMarkLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        scrawlBoardView = new ScrawlBoardView(context);
        addView(scrawlBoardView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public ScrawlBoardView getScrawlBoardView() {
        return scrawlBoardView;
    }

    public void addWaterMark(WaterMark waterMark, int x, int y) {
        // 重置X Y数据
        waterMark.setX(x);
        waterMark.setY(y);
        // 设置一个唯一ID 后面删除时需要用到 PS:只有触摸生效时才能获得真正的currentWaterMark，直接点击删除按钮可能删除的是最后一次触摸的其他水印
        waterMark.setWaterMarkId(System.currentTimeMillis());

        // 存入容器
        waterMarkList.add(waterMark);

        // 设置水印的拖拉按钮的触摸事件监听
        waterMark.setOnTouchListener(onTouchListener);
        waterMark.getBtnControl().setOnTouchListener(onTouchWaterButtonListener);
        waterMark.getBtnDelete().setOnClickListener(new OnClickDelete(waterMark.getWaterMarkId()));

        // 添加本布局中
        addView(waterMark);

        currentWaterMark = waterMark;
    }

    public void addWaterMark(final WaterMark waterMark) {
        // 重载
        addWaterMark(waterMark, 0, 0);
        post(new Runnable() {
            @Override
            public void run() {
                int x = (getWidth() - waterMark.getWidth()) / 2;
                int y = (getHeight() - waterMark.getHeight()) / 2;
                waterMark.setX(x);
                waterMark.setY(y);
            }
        });
    }

    public void setWaterMarkShadowColor(String colorStr) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkShadowColor(colorStr);
    }

    public void setWaterMarkShadowAlpha(int alpha) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkShadowAlpha(alpha);
    }

    public void setWaterMarkColor(String colorStr) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkColor(colorStr);
    }

    public void setWaterMarkAlpha(int alpha) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkAlpha(alpha);
    }

    // 设置字体颜色
    public void setWaterMarkTextColor(String colorStr) {
        if (currentWaterMark != null) {
            currentWaterMark.setWaterMarkTextColor(colorStr);
        }
    }

    // 设置透明度
    public void setWaterMarkTextAlpha(int alpha) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkTextAlpha(alpha);
    }

    // 设置边框字体颜色
    public void setWaterMarkBorderColor(String colorStr) {
        if (currentWaterMark != null) {
            currentWaterMark.setWaterMarkBorderColor(colorStr);
        }
    }

    // 设置边框透明度
    public void setWaterMarkBorderAlpha(int alpha) {
        if (currentWaterMark != null)
            currentWaterMark.setWaterMarkBorderAlpha(alpha);
    }

    // 处理点击水印按钮事件
    OnTouchListener onTouchWaterButtonListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            // 只做flag记录，不做任何处理
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                currentWaterMark = (WaterMark) view.getParent().getParent();
                buttonTouchFlag = true;
            }
            return false;
        }
    };

    // 处理水印的点击触摸事件
    OnTouchListener onTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            // 只做flag记录，不做任何处理
            if ((motionEvent.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                currentWaterMark = (WaterMark) view;
                markTouchFlag = true;
            }
            return false;
        }
    };

    private void showLog(String tex) {
        Log.e("xushiyong", tex);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scrawlBoardView.isEnabled()) {
            return scrawlBoardView.onTouchEvent(event);
        }
        // 获取拖动事件的发生位置
        final float moveX = event.getX();
        final float moveY = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                if (!isAbleMove) {
                    isAbleMove = CalcUtil.spacing(firstTouchX, firstTouchY, event.getX(), event.getY()) > 20;
                }
                if (isAbleMove) {
                    if (buttonTouchFlag) {
                        resizeMark(moveX, moveY);
                    } else if (markTouchFlag) {
                        dragMark(moveX, moveY);
                    }
                    this.moveX = event.getX();
                    this.moveY = event.getY();
                }
                break;
            case MotionEvent.ACTION_DOWN:
                startTouchMark(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                endTouchMark();
                break;
        }
        return true;
    }

    /**
     * 开始触摸
     */
    private void startTouchMark(float x, float y) {
        isAbleMove = false;
        firstTouchX = x;
        firstTouchY = y;
        this.moveX = firstTouchX;
        this.moveY = firstTouchY;
        if (currentWaterMark != null) {
            currentMarkCenterX = currentWaterMark.getX() + currentWaterMark.getWidth() / 2;
            currentMarkCenterY = currentWaterMark.getY() + currentWaterMark.getHeight() / 2;
            // 设置基准尺寸为按下时，按下坐标点和水印中心点的距离
            currentWaterMark.setBaseSize((float) CalcUtil.spacing(currentMarkCenterX, currentMarkCenterY, moveX, moveY));

            for (WaterMark waterMark : waterMarkList) {
                waterMark.setControlBtnVisible(waterMark == currentWaterMark);
            }
        }

    }

    /**
     * 结束触摸
     */
    private void endTouchMark() {
        if (currentWaterMark == null) {
            buttonTouchFlag = false;
            markTouchFlag = false;
            return;
        }

        if (!isAbleMove && (markTouchFlag || buttonTouchFlag)) {
            // 表示有过点击水印的行为，显示水印的控制按钮，同时回调页面做其他处理
            for (WaterMark waterMark : waterMarkList) {
                int flag = waterMark == currentWaterMark ? View.VISIBLE : View.GONE;
                waterMark.setVisibility(flag);
            }
            if (onMarkerClickListener != null) {
                onMarkerClickListener.onMarkerClick(currentWaterMark);
            }
        }

        if (!isAbleMove && !markTouchFlag && !buttonTouchFlag) {
            // 表示点击空白区域，隐藏所有的控制按钮
            for (WaterMark waterMark : waterMarkList) {
                waterMark.setControlBtnVisible(false);
            }
            currentWaterMark = null;
            if (onMarkerClickListener != null) {
                onMarkerClickListener.onMarkerClick(null);
            }
        }

        buttonTouchFlag = false;
        markTouchFlag = false;
    }

    /**
     * 缩放&旋转
     */
    private void resizeMark(float x, float y) {
        if (currentWaterMark == null) {
            return;
        }
        // 水印中心点 和 当前压按点的距离
        final float newDist = (float) CalcUtil.spacing(currentMarkCenterX, currentMarkCenterY, x, y);
        if (newDist == 0) {
            return;
        }
        // 缩放
        currentWaterMark.setScale(newDist);
        postRotation();
    }

    private void postRotation() {
        post(new Runnable() {
            @Override
            public void run() {
                int width = currentWaterMark.getWidth();
                int height = currentWaterMark.getHeight();
                // 计算角度
                float diffAngle = CalcUtil.degree(width, height);
                float currentAngle = CalcUtil.angleBetweenPoints(moveX, moveY, currentMarkCenterX, currentMarkCenterY) - diffAngle;
                currentWaterMark.setRotation(currentAngle);

                postResetPosition();
            }
        });
    }

    private void postResetPosition() {
        post(new Runnable() {
            @Override
            public void run() {
                int width = currentWaterMark.getWidth();
                int height = currentWaterMark.getHeight();
                // 重新设置位置
                currentWaterMark.setX(currentMarkCenterX - width / 2f);
                currentWaterMark.setY(currentMarkCenterY - height / 2f);
            }
        });
    }

    /**
     * 拖动水印
     */
    private void dragMark(float x, float y) {
        if (currentWaterMark == null) {
            return;
        }
        currentWaterMark.setX(currentWaterMark.getX() + (x - this.moveX));
        currentWaterMark.setY(currentWaterMark.getY() + (y - this.moveY));
    }

    /**
     * 删除水印
     *
     * @param waterMarkID
     */
    private void deleteWaterMark(long waterMarkID) {
        for (int i = 0; i < waterMarkList.size(); i++) {
            WaterMark wm = waterMarkList.get(i);
            if (wm.getWaterMarkId() == waterMarkID) {
                waterMarkList.remove(i);
                removeView(wm);
                // 删除的同时，去除当前水印的标记
                currentWaterMark = null;
                if (onMarkerClickListener == null) {
                    onMarkerClickListener.onMarkerClick(null);
                }
                break;
            }
        }
    }

    public boolean hasMark() {
        return waterMarkList.size() != 0 || scrawlBoardView.pathCount() > 0;
    }

    public void hideAllController() {
        for (WaterMark waterMark : waterMarkList) {
            waterMark.setControlBtnVisible(false);
        }
        currentWaterMark = null;
    }

    public void showAllMark() {
        for (WaterMark waterMark : waterMarkList) {
            waterMark.setVisibility(View.VISIBLE);
        }
    }

    public void setOnMarkerClickListener(OnMarkerClickListener onMarkerClickListener) {
        this.onMarkerClickListener = onMarkerClickListener;
    }

    // 点击删除水印的自定义事件 PS:为了接收一个水印ID，便于删除对应的水印
    private class OnClickDelete implements OnClickListener {

        long waterMarkId;

        public OnClickDelete(long waterMarkId) {
            this.waterMarkId = waterMarkId;
        }

        @Override
        public void onClick(View view) {
            // 删除水印
            deleteWaterMark(waterMarkId);
        }
    }

    public interface OnMarkerClickListener {
        void onMarkerClick(WaterMark mark);
    }
}
