package cn.com.syiyi.swiftimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.ImageView;
import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by lintao.song on 2015/10/26.
 * 图片加载主类
 * 目前配置功能均采用链式的方式，还有很多配置的功能，比如队列的存取方式，监听,log等等。
 */
public class SwiftLoader {
    private Context mContext;
    private static SwiftLoader mInstance = null;
    //任务队列
    private LinkedList<ImageTaskInfo> mImageTaskInfoQueue;
    //任务队列分发器
    private DownLoadTaskDispatcher mDownLoadTaskDispatcher;
    //任务执行的线程池
    private ExecutorService mTaskThreadPool;
    //控制任务的信号量
    private Semaphore mTaskSemaphore;
    //handler初始化信号量
    private Semaphore mHandlerInitSemaphore;
    //负责任务分发的单线程
    private ExecutorService mSingleThread;
    //缓存的硬盘路径
    private static String diskCachePath;
    //加载图片的分发器
    private LoadImageDispatcher mloadImageDispatcherDispatcher;
    //默认加载的图片
    private static Bitmap bitmapBeforeLoading;

    /**
     * @param view     要显示的图片的imageview
     * @param url      图片的网址
     * @param listener 图片的网址
     */
    public void displayImg(ImageView view, String url, onLoadImageListener listener) {
        if (mInstance == null)
            throw new RuntimeException("SwiftLoader must be instantiated");
        //利用listview等存在复用的view的机制，根据内存地址对比来避免相同的view加载多个图片，节省流量，线程越多，则流量浪费越多，所以可通过网络的连接方式来控制线程的数量
        for (int i = 0; i < mImageTaskInfoQueue.size(); i++) {
            ImageTaskInfo oldInfo = mImageTaskInfoQueue.get(i);
            if (oldInfo.getView() == view) {
                mImageTaskInfoQueue.remove(i);
                break;
            }
        }
        //设置图片未加载前要显示的图片，如果设置加载前图片并且该view的网络图片未下载的情况下才加载
        if (bitmapBeforeLoading != null && view.getDrawable() == null)
            view.setImageBitmap(bitmapBeforeLoading);
        //添加下载任务
        mImageTaskInfoQueue.add(new ImageTaskInfo(url, view, getDiskCachePath(), listener));
        //控制分发器开始下载任务,此处导致分发器可能会阻塞，因为有下载任务的信号量，单计数器减小时，阻塞消失则继续分发
        mDownLoadTaskDispatcher.postTask();
    }

    /**
     * @param view 要显示的图片的imageview
     * @param url  图片的网址
     */
    public void displayImg(ImageView view, String url) {
        displayImg(view, url, null);
    }

    /**
     * @param mContext
     * @return 获取实例的入口
     */
    public static SwiftLoader build(Context mContext) {
        return getInstance(mContext);
    }

    /**
     * @param mContext
     * @return 返回单例对象
     * 静态方法
     */
    public static synchronized SwiftLoader getInstance(Context mContext) {
        if (mInstance == null)
            mInstance = new SwiftLoader(mContext);
        return mInstance;
    }

    /**
     * @param mContext Context
     *                 初始化
     */
    private SwiftLoader(Context mContext) {
        this.mContext = mContext;
        mImageTaskInfoQueue = new LinkedList<>();
        mTaskThreadPool = Executors.newFixedThreadPool(getThreadCountByNetWorkType());
        mHandlerInitSemaphore = new Semaphore(0);
        mTaskSemaphore = new Semaphore(getThreadCountByNetWorkType());
        mloadImageDispatcherDispatcher = new LoadImageDispatcher();
        mDownLoadTaskDispatcher = new DownLoadTaskDispatcher(mImageTaskInfoQueue, mTaskThreadPool, mTaskSemaphore, mHandlerInitSemaphore, mloadImageDispatcherDispatcher);
        mSingleThread = Executors.newSingleThreadExecutor();
        mSingleThread.execute(mDownLoadTaskDispatcher);
        try {
            mHandlerInitSemaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 线程的和信号量的数量
     * 此处应可配置，根据网络类型选择网络个数，懒得写
     */
    private int getThreadCountByNetWorkType() {
        return 4;
    }

    /**
     * 获取硬盘缓存路径
     * 不存在外置磁盘则用内部磁盘缓存
     */
    private String getDiskCachePath() {
        if (diskCachePath == null) {
            String cachePath;
            if (Environment.MEDIA_MOUNTED.equals(Environment
                    .getExternalStorageState())) {
                cachePath = mContext.getExternalCacheDir().getPath();
            } else {
                cachePath = mContext.getCacheDir().getPath();
            }
            diskCachePath = cachePath;
        }
        return diskCachePath;
    }

    /**
     * @param path 自定义缓存路径
     * @return 返回入口类
     */
    public SwiftLoader setDiskCachePath(String path) {
        if (path == null || path.trim().equals(""))
            throw new RuntimeException("SwiftImageLoader:DiskCache Path can't be null or empty!");
        File file = new File(path);
        Boolean iscreateFile;
        //文件不是目录或者不存在则创建目录，创建失败发出异常
        if (!file.exists() || file.isFile())
            iscreateFile = file.mkdirs();
        else {
            iscreateFile = true;
        }
        if (!iscreateFile)
            throw new RuntimeException("SwiftImageLoader:DiskCache Path create fail!");
        diskCachePath = path;
        return this;
    }

    /**
     * @param bitmap 自定义加载图片前的默认图片
     * @return 返回入口类
     */
    public SwiftLoader setImageBeforeLoading(Bitmap bitmap) {
        bitmapBeforeLoading = bitmap;
        return this;
    }

    /**
     * @param resourseId 自定义加载图片前的默认图片的id
     * @return 返回入口类
     */
    public SwiftLoader setImageBeforeLoading(int resourseId) {
        bitmapBeforeLoading = BitmapFactory.decodeResource(mInstance.mContext.getResources(), resourseId);
        return this;
    }
}
