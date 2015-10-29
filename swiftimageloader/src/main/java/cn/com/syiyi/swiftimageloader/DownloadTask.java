package cn.com.syiyi.swiftimageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;

/**
 * Created by lintao.song on 2015/10/26.
 * 下载任务的具体的执行类
 */
public class DownloadTask implements Runnable {
    //下载任务的信息
    private ImageTaskInfo info;
    //下载任务的信号量，此参数控制线程池内正在下载的任务数以及线程池内部的队列数
    private Semaphore mTaskSemaphore;
    //下载完成后显示图片的分发器
    private LoadImageDispatcher mLoadImageDispatcher;

    public DownloadTask(ImageTaskInfo info, Semaphore mTaskSemaphore, LoadImageDispatcher mLoadImageDispatcher) {
        this.info = info;
        this.mTaskSemaphore = mTaskSemaphore;
        this.mLoadImageDispatcher = mLoadImageDispatcher;
    }

    /**
     * 此方法中信号量有一定的数量，只要信号量的计数器减少一个，就从下载任务队列中取出一个任务
     * 否则下载的分发器就会因为信号量而阻塞在哪里，直到计数器减少，
     * 通过此种方式可以在获取任务队列时选择FIFO/LIFO或者是别的方式。
     */
    @Override
    public void run() {
        try {

            downLoad();
        } catch (Exception e) {
        } finally {
            mTaskSemaphore.release();
        }
    }

    /**
     * 下载
     */
    private void downLoad() {
        mLoadImageDispatcher.setMessage2start(info);
        //1.lru中是否有图片
        if (mLoadImageDispatcher.isLruCacheContain(info.getUrl())) {

        } else if (isDiskCacheContain(info.getDiskCachePath())) {
            //2.硬盘是否有图片
            mLoadImageDispatcher.addLurCache(info.getUrl(), getDiskCache(info.getDiskCachePath()));
        } else {
            //3下载图片
            Bitmap bitmap = DownLoadAndAddDiskCache();
            if (bitmap != null) {
                mLoadImageDispatcher.addLurCache(info.getUrl(), bitmap);
            }
        }
        //4通知显示分发器显示图片，此处内部用了handler,不用考虑线程安全的问题
        mLoadImageDispatcher.sendMessage2ShowImage(info);

    }

    /**
     * @param path 磁盘缓存文件的路径
     * @return 此处在异步线程调用，防止影响主线程
     */
    private boolean isDiskCacheContain(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile())
            return true;
        else
            return false;
    }

    /**
     * @param path 缓存文件的路径
     * @return 返回磁盘缓存下保存的图片
     */
    private Bitmap getDiskCache(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile())
            return BitmapFactory.decodeFile(path);
        else
            return null;
    }

    /**
     * 下载并添加硬盘缓存
     * 此处可加入下载的监听，例如onStart,onComplete,onProcess，调整图片大小等监听方法，懒得搞
     *
     * @return 返回下载好的图片
     */
    private Bitmap DownLoadAndAddDiskCache() {
        String path = info.getDiskCachePath();
        InputStream is = null;
        FileOutputStream out = null;
        try {
            //开始下载获取连接
            URL url = new URL(info.getUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(conn.getInputStream());
//            is.mark(is.available());
            if (is.markSupported()){
                is.mark(1024*1024);
            }else{
                throw new RuntimeException("SwiftImageLoader'S InputStream not support mark!");
            }
            //获取图片的信息，通过options来只加载信息不全部加载图片
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            //获取imageview想要显示的宽和高，并处理图片大小并返回图片，防止内存溢出
            SwiftUtil.ImageSize imageViewSize = SwiftUtil.getImageViewSize(info.getView());
            opts.inSampleSize = SwiftUtil.caculateInSampleSize(opts,
                    imageViewSize.width, imageViewSize.height);
            opts.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, opts);
            conn.disconnect();
            //下载好图片后添加硬盘缓存
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }

            try {
                if (out != null)
                    out.flush();
                out.close();
            } catch (IOException e) {
            }
        }
        return null;
    }
}
