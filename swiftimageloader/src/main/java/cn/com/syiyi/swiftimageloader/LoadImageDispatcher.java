package cn.com.syiyi.swiftimageloader;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;

/**
 * Created by lintao.song on 2015/10/27.
 * 显示图片的的分发器
 */
public class LoadImageDispatcher implements Handler.Callback {
    //内存缓存，使用最近最少使用的页面置换算法，请百度，非常符合listview的情况，以网址md5后的值为key
    private LruCache<String, Bitmap> cache;
    //用户显示图片的handler，防止线程安全问题
    private Handler mHandler;

    public LoadImageDispatcher() {
        mHandler = new Handler(this);
        //设置内存缓存为运行时内存的8分之一
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        cache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    /**
     * @param url 图片的网址
     * @return 内存缓存中是否含有此图片
     */
    public synchronized boolean isLruCacheContain(String url) {
        Bitmap bitmap = cache.get(SwiftUtil.md5(url));
        return bitmap != null;
    }

    /**
     * 将图片添加入内存缓存
     *
     * @param url    图片的网址
     * @param bitmap 需要加入缓存的图片
     */
    public synchronized void addLurCache(String url, Bitmap bitmap) {
        cache.put(SwiftUtil.md5(url), bitmap);
    }

    //发送显示图片的消息
    public synchronized void sendMessage2ShowImage(ImageTaskInfo info) {
        Message msg = mHandler.obtainMessage();
        msg.what = 1;
        msg.obj = info;
        mHandler.sendMessage(msg);
    }

    //开始加载的监听
    public synchronized void setMessage2start(ImageTaskInfo info) {
        Message msg = mHandler.obtainMessage();
        msg.what = 0;
        msg.obj = info;
        mHandler.sendMessage(msg);
    }

    /**
     * @param msg 显示图片你的消息
     * @return 是否一次性
     */
    @Override
    public boolean handleMessage(Message msg) {
        ImageTaskInfo info = (ImageTaskInfo) msg.obj;
        onLoadImageListener listener = info.getListener();
        switch (msg.what) {
            case 0:
                if (listener != null) {
                    listener.onStart();
                }
                break;
            case 1:
                showImage((ImageTaskInfo) msg.obj);

                break;
        }
        return false;
    }

    /**
     * 通过对比view中设置的tag值来比较图片网址是否一致来防止图片错误,图片加载完成的监听
     *
     * @param info 下载任务的信息
     */
    private void showImage(ImageTaskInfo info) {
        if (info.getView().getTag().toString().trim().equals(info.getUrl().trim())) {
            onLoadImageListener listener = info.getListener();
            Bitmap bitmap = cache.get(SwiftUtil.md5(info.getUrl()));
            info.getView().setImageBitmap(bitmap);
            if (listener != null) {
                listener.onComplete(bitmap);
            }
        }
    }

}
