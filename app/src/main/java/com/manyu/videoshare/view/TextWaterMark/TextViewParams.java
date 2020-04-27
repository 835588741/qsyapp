package com.manyu.videoshare.view.TextWaterMark;

/**
 * Created by cretin on 15/12/21
 * 用于记录每个TextView的状态
 */
public class TextViewParams {
    int width;
    int height;
    long tag;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }
}