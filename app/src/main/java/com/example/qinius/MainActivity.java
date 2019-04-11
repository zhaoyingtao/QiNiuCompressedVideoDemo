package com.example.qinius;

import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.qiniu.pili.droid.shortvideo.PLShortVideoTranscoder;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;

import static com.qiniu.pili.droid.shortvideo.PLErrorCode.ERROR_LOW_MEMORY;
import static com.qiniu.pili.droid.shortvideo.PLErrorCode.ERROR_NO_VIDEO_TRACK;
import static com.qiniu.pili.droid.shortvideo.PLErrorCode.ERROR_SRC_DST_SAME_FILE_PATH;

public class MainActivity extends AppCompatActivity {
    private boolean isCompressing;
    public static final String LOCAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/Android/data/com.example.qinius/";
    private TextView textView;
    private TextView tv_time;
    private TextView tv_startSize;
    private TextView tv_endSize;
    private float percentage;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.compresse_tv);
        tv_time = findViewById(R.id.tv_time);
        tv_startSize = findViewById(R.id.tv_startSize);
        tv_endSize = findViewById(R.id.tv_endSize);
        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 修改这里的原始视频路径
                String videoPath = LOCAL_PATH + "testcompressevideo.mp4";
                tv_startSize.setText("压缩前视频大小:" + getFormatSize(getFileSize(new File(videoPath))));
                startCompressed(videoPath);
            }
        });

    }

    private void startCompressed(String videoPath) {
        if (isCompressing) {
            return;
        }
        isCompressing = true;
        //PLShortVideoTranscoder初始化，三个参数，第一个context，第二个要压缩文件的路径，第三个视频压缩后输出的路径
        PLShortVideoTranscoder mShortVideoTranscoder = new PLShortVideoTranscoder(this, videoPath, LOCAL_PATH + "compress/transcoded.mp4");
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        retr.setDataSource(videoPath);
        String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT); // 视频高度
        String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH); // 视频宽度
        String rotation = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION); // 视频旋转方向
        int transcodingBitrateLevel = 6;
        startTime = System.currentTimeMillis();
        mShortVideoTranscoder.transcode(Integer.parseInt(width), Integer.parseInt(height), getEncodingBitrateLevel(transcodingBitrateLevel), false, new PLVideoSaveListener() {
            @Override
            public void onSaveVideoSuccess(String s) {
                handler.sendEmptyMessage(103);
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        LogUtil.e("onSaveVideoSuccess======== " + s);
//                    }
//                });
            }

            @Override
            public void onSaveVideoFailed(final int errorCode) {
                isCompressing = false;
                Log.e("snow", "save failed: " + errorCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (errorCode) {
                            case ERROR_NO_VIDEO_TRACK:
//                                ToastUtils.getInstance().showToast("该文件没有视频信息！");
                                break;
                            case ERROR_SRC_DST_SAME_FILE_PATH:
//                                ToastUtils.getInstance().showToast("源文件路径和目标路径不能相同！");
                                break;
                            case ERROR_LOW_MEMORY:
//                                ToastUtils.getInstance().showToast("手机内存不足，无法对该视频进行时光倒流！");
                                break;
                            default:
//                                ToastUtils.getInstance().showToast("transcode failed: " + errorCode);
                        }
                    }
                });
            }

            @Override
            public void onSaveVideoCanceled() {
                handler.sendEmptyMessage(101);
//                LogUtil.e("onSaveVideoCanceled");
            }

            @Override
            public void onProgressUpdate(float percentag) {
                percentage = percentag;
                handler.sendEmptyMessage(100);
//                LogUtil.e("onProgressUpdate==========" + percentage);
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    textView.setText("" + percentage);
                    break;
                case 101:
                    textView.setText("压缩取消了");
                    break;
                case 102:
                    break;
                case 103:
                    String compressedFilePath = LOCAL_PATH + "compress/transcoded.mp4";
                    tv_endSize.setText("压缩后视频大小:" + getFormatSize(getFileSize(new File(compressedFilePath))));
                    int time = (int) ((System.currentTimeMillis() - startTime) / 1000);
                    tv_time.setText("压缩用时：" + time + "秒");
                    textView.setText("压缩完成");
                    break;
            }
            return false;
        }
    });


    /**
     * 设置压缩质量
     *
     * @param position
     * @return
     */
    private int getEncodingBitrateLevel(int position) {
        return ENCODING_BITRATE_LEVEL_ARRAY[position];
    }

    /**
     * 选的越高文件质量越大，质量越好,对应压缩后尺寸更大
     */
    public static final int[] ENCODING_BITRATE_LEVEL_ARRAY = {
            500 * 1000,
            800 * 1000,
            1000 * 1000,
            1200 * 1000,
            1600 * 1000,
            2000 * 1000,
            2500 * 1000,
            4000 * 1000,
            8000 * 1000,
    };


    /**
     * 获取指定文件大小
     *
     * @return
     * @throws Exception
     */
    private static long getFileSize(File file) {
        long size = 0;
        try {
            if (file.exists()) {
                FileInputStream fis = null;
                fis = new FileInputStream(file);
                size = fis.available();
            } else {
                file.createNewFile();
                Log.e("获取文件大小", "文件不存在!");
            }
            return size;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 格式化单位
     *
     * @param size
     */

    public String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "MB";
        }
        double megaByte = kiloByte / 1024;
//        if (megaByte < 1) {
//
//            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
//            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
//        }
        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }
        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

}
