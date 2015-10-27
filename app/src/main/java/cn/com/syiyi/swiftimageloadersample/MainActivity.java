package cn.com.syiyi.swiftimageloadersample;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import cn.com.syiyi.common.CommonAdapter;
import cn.com.syiyi.common.ImageUrls;
import cn.com.syiyi.common.ViewHolder;
import cn.com.syiyi.swiftimageloader.onLoadImageListener;
import cn.com.syiyi.swiftimageloader.SwiftLoader;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    List<String> mdatas;
    SwiftLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String sdDir = Environment.getExternalStorageDirectory().toString() + File.separator + "SwiftImageLoadeCache";
        //缓存地址以及默认图片可不设置
        loader = SwiftLoader.build(this).setImageBeforeLoading(R.drawable.a).setDiskCachePath(sdDir);
        listView = (ListView) findViewById(R.id.list_view);
        mdatas = new ArrayList<>();
        for (String url : ImageUrls.urls) {
            mdatas.add(url);
        }
        listView.setAdapter(new CommonAdapter<String>(mdatas, MainActivity.this, R.layout.item) {
            @Override
            public void convert(final ViewHolder holder, int position) {
                //此处listener可不传
                loader.displayImg((ImageView) holder.getView(R.id.image), mdatas.get(position), new onLoadImageListener() {
                    @Override
                    public void onStart() {
                        Log.i("Swift", "开始加载");
                    }

                    @Override
                    public void onComplete(Bitmap bitmap) {
                        Log.i("Swift", "加载完成：" + bitmap.getWidth() + "/" + bitmap.getHeight() + "哈哈哈");
                    }
                });
            }

            @Override
            public void onItemClick(int position) {

            }
        });

    }
}
