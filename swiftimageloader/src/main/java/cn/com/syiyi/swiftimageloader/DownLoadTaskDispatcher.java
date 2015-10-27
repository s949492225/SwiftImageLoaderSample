package cn.com.syiyi.swiftimageloader;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

/**
 * Created by lintao.song on 2015/10/26.
 * 下载任务分发器
 */
public class DownLoadTaskDispatcher extends Thread {
    //下载任务的线程池
    private ExecutorService mTaskThreadPool;
    //下载任务的信号量，用于控制队列进出，与阻塞任务分发器
    private Semaphore mTaskSemaphore;
    //分发任务采用线程内部的handler,调用handler时可能handler没有初始化完成，通过信号量控制调用者在未完成时处于阻塞状态
    private Semaphore mHandlerInitSemaphore;
    //储存任务信息的链表，此链表通过信号量机制加handler机制控制并发安全
    private LinkedList<ImageTaskInfo> mImageTaskInfoQueue;
    //图片下载完成后，显示图片的分发器
    private LoadImageDispatcher mloadImageDispatcherDispatcher;
    //用于执行任务队列
    private Handler mHandler;

    public DownLoadTaskDispatcher(LinkedList<ImageTaskInfo> ImageTaskInfoQueue, ExecutorService mTaskThreadPool, Semaphore mTaskSemaphore, Semaphore mHandlerInitSemaphore, LoadImageDispatcher mloadImageDispatcherDispatcher) {
        this.mTaskThreadPool = mTaskThreadPool;
        this.mTaskSemaphore = mTaskSemaphore;
        this.mHandlerInitSemaphore = mHandlerInitSemaphore;
        this.mImageTaskInfoQueue = ImageTaskInfoQueue;
        this.mloadImageDispatcherDispatcher = mloadImageDispatcherDispatcher;
    }

    /**
     * 通过信号线机制在handler未初始化的情况下控制调用者阻塞。
     */
    @Override
    public void run() {
        try {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //执行下载任务
                    mTaskThreadPool.execute(newDownLoadTask(mImageTaskInfoQueue.removeLast(), mTaskSemaphore));
                }
            };
            //释放初始化安全控制的信号量
            mHandlerInitSemaphore.release();
            Looper.loop();
        } catch (Exception e) {
            System.out.println("******error:" + "DownLoadTaskDispatcher's run exception");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     *由下载任务分发器创建下载任务
     * @param info 下载任务信息
     * @param mTaskSemaphore 下载任务的信号量
     * @return 返回具体的下载任务
     */
    public DownloadTask newDownLoadTask(ImageTaskInfo info, Semaphore mTaskSemaphore) {
        return new DownloadTask(info, mTaskSemaphore, mloadImageDispatcherDispatcher);
    }

    /**
     * 向下载任务分发器提交下载任务
     */
    public void postTask() {
        if (mHandler == null)
            throw new RuntimeException("DownLoadTaskDispatcher's handler not init");
        mHandler.sendEmptyMessage(0);
    }
}
