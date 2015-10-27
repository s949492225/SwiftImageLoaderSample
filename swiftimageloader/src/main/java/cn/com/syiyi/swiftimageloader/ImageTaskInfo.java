package cn.com.syiyi.swiftimageloader;

import android.widget.ImageView;

import java.io.File;

/**
 * Created by lintao.song on 2015/10/26.
 * 图片加载的任务的信息
 */
public class ImageTaskInfo {
    //图片的网址
    private String url;
    //要显示的view
    private ImageView view;
    //磁盘的的缓存路径
    private String diskCachePath;
    //下载图片的监听
    private onLoadImageListener listener;

    public ImageTaskInfo(String url, ImageView view, String diskCachePath, onLoadImageListener listener) {

        setUrl(url);
        setView(view);
        setDiskCachePath(diskCachePath);
        if (listener != null)
            this.listener = listener;

    }

    public onLoadImageListener getListener() {
        return listener;
    }

    public void setListener(onLoadImageListener listener) {
        this.listener = listener;
    }

    public String getDiskCachePath() {
        return diskCachePath;
    }

    public void setDiskCachePath(String diskCachePath) {

        this.diskCachePath = diskCachePath + File.separator + SwiftUtil.md5(getUrl());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.trim().equals(""))
            throw new RuntimeException("image's url can not be null");
        this.url = url;
    }

    public ImageView getView() {
        return view;
    }

    public void setView(ImageView view) {
        if (view == null)
            throw new RuntimeException("imageView can not be null");
        view.setTag(url);
        this.view = view;
    }

}
