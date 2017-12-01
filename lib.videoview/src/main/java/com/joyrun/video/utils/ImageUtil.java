package com.joyrun.video.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageUtil {

    /**
     * bitmap压缩
     *
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 通过文件描述符的方法加载bitmap
     *
     * @param fd
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(FileDescriptor fd, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    /**
     * 计算压缩比例
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if ((reqWidth != 0 && reqHeight != 0) && (height > reqHeight || width > reqWidth)) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    /**
     * 打开一个LruCache
     *
     * @return
     */
    public static LruCache<String, Bitmap> openLruCache(int caCheSize) {
        LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(caCheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) { //返回bitmap的大小
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
        return imageCache;
    }

    /**
     * 打开DiskLruCache
     *
     * @param context
     * @return
     */
    public static DiskLruCache openDiskLruCache(Context context, long maxSize, String cacheDirName) {
        DiskLruCache diskLruCache = null;
        File cacheDir = getDiskCacheDir(context, cacheDirName);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        Log.i("cacheDir", cacheDir.getPath());
        try {
            if (getUsableSpace(cacheDir) > maxSize) {
                diskLruCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, maxSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return diskLruCache;
    }

    /**
     * 获取缓存路径
     *
     * @param context
     * @param name
     * @return
     */
    private static File getDiskCacheDir(Context context, String name) {
        String cacheDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cacheDir = context.getExternalCacheDir().getPath();
        } else {
            cacheDir = context.getCacheDir().getPath();
        }
        return new File(cacheDir + File.separator + name);
    }

    /**
     * 获取系统版本号
     *
     * @param context
     * @return
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * MD5加密
     *
     * @param url
     * @return
     */
    public static String encode(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] result = digest.digest(url.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : result) {
                int num = b & 0xff;
                String str = Integer.toHexString(num);
                if (str.length() == 1) {
                    sb.append("0");
                }
                sb.append(str);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            // can't reach
            return "";
        }
    }


    /**
     * 获取可用空间
     * @param path
     * @return
     */
    public static long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        StatFs stats = new StatFs(path.getPath());
        return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
    }

    /**
     * 下载图片
     *
     * @param urlString
     * @param outputStream
     * @return
     */
    public static boolean downLoadImage(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 下载图片
     * @param urlString
     * @return bitmap
     */
    public static Bitmap downLoadImage(String urlString) {
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        Bitmap bitmap = null;
        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), 8 * 1024);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * 加载一个图片放入DisLruCache
     *
     * @param imageUrl
     * @param disLruCache
     */
    public static void put2DisLruCache(String imageUrl, DiskLruCache disLruCache) {
        String key = ImageUtil.encode(imageUrl);

        DiskLruCache.Editor editor = null;
        try {
            editor = disLruCache.edit(key);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                boolean success = ImageUtil.downLoadImage(imageUrl, outputStream);
                if (success) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            disLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从DiskLruCache中取bitmap
     * @param imageUrl
     * @param disLruCache
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap getFromDiskLruCache(String imageUrl, DiskLruCache disLruCache, int reqWidth, int reqHeight) {
        try {
            String key = encode(imageUrl);
            DiskLruCache.Snapshot snapshot = disLruCache.get(key);
            if (snapshot != null) {
                FileInputStream inputStream = (FileInputStream) snapshot.getInputStream(0);
                FileDescriptor fd = inputStream.getFD();
                Bitmap bitmap = decodeSampledBitmapFromResource(fd, reqWidth, reqHeight);
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}