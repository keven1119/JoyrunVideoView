package com.joyrun.video.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;
import com.joyrun.video.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageLoader {
    private static final String TAG = "ImageLoader";
    private static final int TAG_KEY_URL = R.id.imageview_cover;
    private static final int MESSAGE_POST_RESULT = 1;
    private Context mContext;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskCache;

    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 10; // 指定DiskLruCache缓存空间大小  10M的空间
    private static final String CACHE_DIR_NAME = "bitmap"; // 缓存文件夹名称
    private static final int MEMORY_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8; //指定LruCache缓存大小 内存的八分之一

    /****************线程池相关参数********************/
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() + 1;
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
    private static final int KEEP_ALIVE = 10;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger();
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };

    // 创建线程池
    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(), sThreadFactory);

    // 实现运行在主线程的handler
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            LoadResult result = (LoadResult) msg.obj;
            ImageView imageView = result.imageView;
            String url = (String) imageView.getTag(TAG_KEY_URL);
            if (url.equals(result.url)) { //解决加载出来后，ImageView已经滑过的问题
                imageView.setImageBitmap(result.bitmap);
                Log.d(TAG, "set image bitmap");
            } else {
                Log.w(TAG, "set image bitmap ,but url has changed , ignored");
            }
        }
    };


    private ImageLoader(Context context) {
        mContext = context;
        mMemoryCache = ImageUtil.openLruCache(MEMORY_CACHE_SIZE);
        mDiskCache = ImageUtil.openDiskLruCache(mContext, DISK_CACHE_SIZE, CACHE_DIR_NAME);
    }

    public static ImageLoader build(Context context) {
        return new ImageLoader(context);
    }


    /**
     * 把bitmap放入内存中
     * @param key
     * @param bitmap
     */
    private void addBitmap2MemoryCache(String key, Bitmap bitmap) {
        if (loadBitmMapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从内存中加载
     * @param key
     * @return
     */
    private Bitmap loadBitmMapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    /**
     * 从磁盘中加载
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap =  ImageUtil.getFromDiskLruCache(url, mDiskCache, reqWidth, reqHeight);
        if (bitmap != null) { //放入到内存中
            mMemoryCache.put(url, bitmap);
        }
        return bitmap;
    }

    /**
     * 从网络中加载
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) {
        if (Looper.myLooper() == Looper.getMainLooper()) { //判断是否在主线程
            throw new RuntimeException("can not visit net work from UI Thread");
        }
        if (mDiskCache == null) {
            return null;
        }
        String key = ImageUtil.encode(url);
        ImageUtil.put2DisLruCache(url, mDiskCache);
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    /**
     * 同步加载
     * @return
     */
    public Bitmap loadBitmap(String url, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmMapFromMemoryCache(url);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmMapFromMemoryCache, url:" + url);
            return bitmap;
        }

        bitmap = loadBitmapFromDiskCache(url, reqWidth, reqHeight);
        if (bitmap != null) {
            Log.d(TAG, "loadBitmapFromDiskCache, url:" + url);
            return bitmap;
        }

        bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);
        Log.d(TAG, "loadBitmapFromHttp, url:" + url);
        if (bitmap == null ) {
            Log.d(TAG, "DiskLrucache is not created");
            Bitmap httpBitmap = ImageUtil.downLoadImage(url);
            if(httpBitmap != null) {
                Log.d(TAG, "orignal bitmap width==>" + httpBitmap.getWidth() + "     height==>" + httpBitmap.getHeight());
                Log.d(TAG, "request width ==>" + reqWidth + "    request height==>" + reqHeight);
                bitmap = ratio(httpBitmap, reqWidth, reqHeight);
                httpBitmap.recycle();
                Log.d(TAG, "ratio bitmap width==>" + bitmap.getWidth() + "     height==>" + bitmap.getHeight());
            }
        }
        return bitmap;
    }

    /**
     * 将bitmap进行压缩
     * Compress image by size, this will modify image width/height.
     * Used to get thumbnail
     *
     * @param image
     * @param pixelW target pixel of width
     * @param pixelH target pixel of height
     * @return
     */
    public static Bitmap ratio(Bitmap image, float pixelW, float pixelH) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, os);
        if( os.toByteArray().length / 1024>1024) {//判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
            os.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 50, os);//这里压缩50%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        //开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        float hh = pixelH;// 设置高度为240f时，可以明显看到图片缩小了
        float ww = pixelW;// 设置宽度为120f，可以明显看到图片缩小了
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (w > h && w > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) be = 1;
        newOpts.inSampleSize = be;//设置缩放比例
        //重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        is = new ByteArrayInputStream(os.toByteArray());
        bitmap = BitmapFactory.decodeStream(is, null, newOpts);
        //压缩好比例大小后再进行质量压缩
//      return compress(bitmap, maxSize); // 这里再进行质量压缩的意义不大，反而耗资源，删除
        return bitmap;
    }

    /**
     * 异步加载
     * @param url
     * @param imageView
     * @param reqWidth
     * @param reqHeight
     */
    public void bindBitmap(final String url, final ImageView imageView, final int reqWidth, final int reqHeight) {
        imageView.setTag(TAG_KEY_URL, url);
        Bitmap bitmap = loadBitmMapFromMemoryCache(url);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(url, reqWidth, reqHeight);

                if (bitmap != null) {
                    LoadResult result = new LoadResult(imageView, url, bitmap);
                    Message msg = mHandler.obtainMessage(MESSAGE_POST_RESULT, result);
                    mHandler.sendMessage(msg);
                }
            }
        };

        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    /**
     * 消息的封装实体
     */
    private static class LoadResult {
        public ImageView imageView;
        public String url;
        public Bitmap bitmap;

        public LoadResult(ImageView imageView, String url, Bitmap bitmap) {
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }
}