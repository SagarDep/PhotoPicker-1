package com.nanchen.imagepicker.bean;

import java.io.Serializable;

/**
 * 照片实体
 *
 * Author: nanchen
 * Email: liushilin520@foxmail.com
 * Date: 2017-03-10  17:48
 */

public class Photo implements Serializable {

    private int id;
    private String path;  //路径
    private boolean isCamera;

    public Photo(String path) {
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isCamera() {
        return isCamera;
    }

    public void setIsCamera(boolean isCamera) {
        this.isCamera = isCamera;
    }
}
