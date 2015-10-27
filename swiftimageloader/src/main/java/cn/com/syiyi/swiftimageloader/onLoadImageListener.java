package cn.com.syiyi.swiftimageloader;

import android.graphics.Bitmap;

/**
 * Created by lintao.song on 2015/10/27.
 */
public interface onLoadImageListener {
    void onStart();
    void onComplete(Bitmap bitmap);
}
