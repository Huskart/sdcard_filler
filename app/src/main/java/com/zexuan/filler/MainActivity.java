package com.zexuan.filler;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;


import com.stericson.RootTools.RootTools;
import com.zexuan.filler.utils.FileUtil;
import com.zexuan.filler.utils.ShellUtils;
import com.zexuan.filler.velocimeter.VelocimeterView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity {

    private VelocimeterView mVelocimeterView;
    private EditText mRemainderSpaceEdt;
    private Button fillBtn;
    private Button releaseBtn;
    private TextView statusTv;
    private static final int UPDATE_PROGRESSBAR = 0x01;
    private static final int ENABLE_BUTTON = 0x02;
    private int fillTarget = 0;         //0 datazone 1 systemzone
    private String timeStamp;
    private boolean fillOver = false;
    private String demo = "";
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MainActivity.UPDATE_PROGRESSBAR:
                    if (fillTarget == 0) {
                        float totalSpc = Float.parseFloat(getSDTotalSize());
                        float remainSpc = Float.parseFloat(getSDAvailableSize());
                        Log.e("zexuan", 100 * (totalSpc - remainSpc) + "");
                        mVelocimeterView.setValue(100 * (totalSpc - remainSpc) / totalSpc, true);
                    } else {
                        float totalSpc = Float.parseFloat(getTotalSize("/system"));
                        float remainSpc = Float.parseFloat(getAvailableSize("/system"));
                        mVelocimeterView.setValue(100 * (totalSpc - remainSpc) / totalSpc, true);
                    }
                    break;
                case MainActivity.ENABLE_BUTTON:
                    fillBtn.setEnabled(true);
                    releaseBtn.setEnabled(true);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        this.mVelocimeterView = (VelocimeterView) findViewById(R.id.velocimeter2);
        this.mVelocimeterView.setMax(100);
        float totalSpc = Float.parseFloat(getSDTotalSize());
        float remainSpc = Float.parseFloat(getSDAvailableSize());
        float value = ((totalSpc - remainSpc) / totalSpc) * 100;
        this.mVelocimeterView.setValue(value, true);
        this.mRemainderSpaceEdt = (EditText) findViewById(R.id.remainder_space_edt);
        this.fillBtn = (Button) findViewById(R.id.fill_btn);

        this.releaseBtn = (Button) findViewById(R.id.release_btn);
        this.fillBtn.setOnClickListener(new FillBtnListener());
        this.releaseBtn.setOnClickListener(new ReleaseSDCardListenerImpl());

        this.statusTv = (TextView) findViewById(R.id.status_tv);
        this.statusTv.setVisibility(View.GONE);

        TextView targetTv = (TextView) findViewById(R.id.target_tv);
        targetTv.setTextColor(Color.WHITE);

        RadioButton dataRb = (RadioButton) findViewById(R.id.data_rb);

        dataRb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    fillTarget = 0;
                } else {
                    fillTarget = 1;
                }
                reloadVelocimeter();
            }
        });
    }

    private void reloadVelocimeter() {
        if (fillTarget == 0) {
            float totalSpc = Float.parseFloat(getSDTotalSize());
            float remainSpc = Float.parseFloat(getSDAvailableSize());
            float value = ((totalSpc - remainSpc) / totalSpc) * 100;
            this.mVelocimeterView.setTargetType(0);
            this.mVelocimeterView.setValue(value, true);
        } else {
            float totalSpc = Float.parseFloat(getTotalSize("/system"));
            float remainSpc = Float.parseFloat(getAvailableSize("/system"));
            float value = ((totalSpc - remainSpc) / totalSpc) * 100;
            this.mVelocimeterView.setTargetType(1);
            this.mVelocimeterView.setValue(value, true);
        }
    }

    class ReleaseSDCardListenerImpl implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            statusTv.setVisibility(View.GONE);
            fillBtn.setEnabled(false);
            releaseBtn.setEnabled(false);
            if (fillTarget == 0) {
                Snackbar.make(v, "开始释放", Snackbar.LENGTH_SHORT).show();
                releaseSDCardSpace();
            } else {
                Snackbar.make(v, "开始释放", Snackbar.LENGTH_SHORT).show();
                releaseSystemSpace();
            }

            statusTv.setVisibility(View.INVISIBLE);
        }

    }

    private void releaseSystemSpace() {

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                ShellUtils.execCommand("mount -o remount,rw /system", true);
                ShellUtils.execCommand("chmod 777 /system", true);
                ShellUtils.execCommand("rm -rf /system/cpFile", true);

                String demo = "";
                while (!Thread.currentThread().isInterrupted()) {
                    Message message = new Message();
                    message.what = MainActivity.UPDATE_PROGRESSBAR;
                    handler.sendMessage(message);
                    try {
                        Thread.sleep(1200);
                        String availableSize = getAvailableSize("/system");
                        if (demo.equals(availableSize)) {
                            Thread.sleep(1000);
                            if (demo.equals(availableSize)) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        fillBtn.setEnabled(true);
                                        releaseBtn.setEnabled(true);
                                        statusTv.setText("释放完成");
                                        statusTv.setVisibility(View.VISIBLE);
                                    }
                                });
                                Thread.currentThread().interrupt();
                            }
                        }
                        demo = availableSize;
                        Log.e("zex", "thread run run");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        try {
            thread2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseSDCardSpace() {

        Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map(new Func1<Long, String>() {
                    @Override
                    public String call(Long aLong) {
                        FileUtil.deleteFolder(Environment.getExternalStorageDirectory().getPath() + "/cpFile");
                        return getAvailableSize(Environment.getExternalStorageDirectory().getPath());

                    }
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        if (!demo.equals(s)) {
                            demo = s;
                            return false;
                        }
                        return true;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        demo = "";
                    }

                    @Override
                    public void onError(Throwable e) {
                        demo = "";
                    }

                    @Override
                    public void onNext(String s) {
                        Log.e("zexuan", "over " + Thread.currentThread().getName());
                        fillBtn.setEnabled(true);
                        releaseBtn.setEnabled(true);
                        statusTv.setText("释放完成");
                        statusTv.setVisibility(View.VISIBLE);
                        float totalSpc = Float.parseFloat(getSDTotalSize());
                        float remainSpc = Float.parseFloat(getSDAvailableSize());
                        mVelocimeterView.setValue(100 * (totalSpc - remainSpc) / totalSpc, true);
                        demo = "";
                        unsubscribe();
                    }
                });
    }

    private String getSDTotalSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return String.valueOf(blockSize * totalBlocks / (1024 * 1024));
    }

    private String getAvailableSize(String path) {
        StatFs stat = new StatFs(path);
        return String.valueOf(stat.getFreeBytes() / (1024 * 1024));
    }

    private String getTotalSize(String path) {
        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return String.valueOf(blockSize * totalBlocks / (1024 * 1024));
    }

    private String getSDAvailableSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return String.valueOf(blockSize * availableBlocks / (1024 * 1024));
    }

    class FillBtnListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            boolean isRoot = RootTools.isRootAvailable();
            if (fillTarget == 1 && !isRoot) {
                Snackbar.make(v, "System分区需要开启Root权限", Snackbar.LENGTH_SHORT).show();
                return;
            }

            String remainSpa = mRemainderSpaceEdt.getText().toString();

            if (!isNumeric(remainSpa) || remainSpa.equals("")) {
                statusTv.setText("请正确设置参数");
                statusTv.setVisibility(View.VISIBLE);
                return;
            }
            if (Integer.parseInt(remainSpa) < 0 || Integer.parseInt(remainSpa) > getMemoryInfo(new File("/system"))) {
                statusTv.setText("请正确设置参数");
                statusTv.setVisibility(View.VISIBLE);
                return;
            }

            float availSize = fillTarget == 0 ? Float.parseFloat(getSDAvailableSize()) : Float.parseFloat(getAvailableSize("/system"));

            if (Integer.parseInt(remainSpa) > availSize) {
                statusTv.setText("请正确设置参数");
                statusTv.setVisibility(View.VISIBLE);
                return;
            }
            fillBtn.setEnabled(false);
            releaseBtn.setEnabled(false);
            statusTv.setVisibility(View.GONE);
            if (fillTarget == 0) {

                FileUtil.createDir(Environment.getExternalStorageDirectory().getPath() + "/cpFile/src/");
                Snackbar.make(v, "开始填充", Snackbar.LENGTH_SHORT).show();
                fillSDCardSpace();
            } else {
                fillSystemSpace();
                Snackbar.make(v, "分区挂载中，稍后...", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void fillSystemSpace() {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Log.e("zexuan", "call thread is " + Thread.currentThread().getName());
                ShellUtils.execCommand("mount -o remount,rw /system", true);
                ShellUtils.execCommand("chmod 777 /system", true);
                FileUtil.createDir("/system/cpFile/src/");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.CHINA).format(new Date());
                File systemFileDir = new File("/system");
                final File src = new File("/system/cpFile/src/" + timeStamp + ".txt");
                final File dest = new File("/system/cpFile/" + timeStamp + ".txt");
                try {
                    long remain = Long.parseLong(mRemainderSpaceEdt.getText().toString()) * 1024 * 1024;
                    createFixLengthFile(src, getMemoryInfo(systemFileDir) - remain);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!dest.exists()) {
                    dest.mkdirs();
                }
                try {
                    Runtime.getRuntime().exec("cp " + src + " " + dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                subscriber.onNext("");
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        fillOver = true;
                    }
                });

        Observable.interval(1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        float totalSpc = Float.parseFloat(getTotalSize("/system"));
                        float remainSpc = Float.parseFloat(getAvailableSize("/system"));
                        Log.e("zexuan" , "value is " + 100 * (totalSpc - remainSpc) / totalSpc);
                        mVelocimeterView.setValue(100 * (totalSpc - remainSpc) / totalSpc, true);
                        if (fillOver) {
                            fillOver = false;
                            statusTv.setText("填充完成");
                            statusTv.setVisibility(View.VISIBLE);
                            fillBtn.setEnabled(true);
                            releaseBtn.setEnabled(true);
                            mVelocimeterView.setValue(100, true);
                            unsubscribe();
                        }
                    }
                });
    }

    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    private long getMemoryInfo(File path) {
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return blockSize * availableBlocks;
    }

    public void fillSDCardSpace() {
        timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.CHINA).format(new Date());
        File sdcardFileDir = Environment.getExternalStorageDirectory();
        final File src = new File(Environment.getExternalStorageDirectory().getPath() + "/cpFile/src/" + timeStamp + ".txt");
        final File dest = new File(Environment.getExternalStorageDirectory().getPath() + "/cpFile/" + timeStamp + ".txt");
        try {
            long remain = Long.parseLong(this.mRemainderSpaceEdt.getText().toString()) * 1024 * 1024;
            if (remain >= 0 && remain <= getMemoryInfo(sdcardFileDir)) {
                createFixLengthFile(src, 1024 * 1024 * Long.parseLong(getAvailableSize(Environment.getExternalStorageDirectory().getPath())) - remain);
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Log.e("zexuan", Thread.currentThread().getName());
                FileUtil.copyFile(src.getPath(), dest.getPath());
                Log.e("zexuan", "copy over");
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        Log.e("zexuan", "complete");
                        fillOver = true;
                    }

                    @Override
                    public void onError(Throwable e) {
                        fillOver = true;
                    }

                    @Override
                    public void onNext(String s) {
                        Log.e("zexuan", "next " + Thread.currentThread().getName());
                    }
                });

        Observable.interval(1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        float totalSpc = Float.parseFloat(getSDTotalSize());
                        float remainSpc = Float.parseFloat(getSDAvailableSize());
                        Log.e("zexuan", 100 * (totalSpc - remainSpc) + "");
                        mVelocimeterView.setValue(100 * (totalSpc - remainSpc) / totalSpc, true);
                        if (fillOver) {
                            fillOver = false;
                            statusTv.setText("填充完成");
                            statusTv.setVisibility(View.VISIBLE);
                            fillBtn.setEnabled(true);
                            releaseBtn.setEnabled(true);
                            unsubscribe();
                        }
                    }
                });

    }

    public void createFixLengthFile(File file, long length) throws IOException {
        long start = System.currentTimeMillis();
        FileOutputStream fos = null;
        FileChannel output = null;
        try {
            fos = new FileOutputStream(file);
            output = fos.getChannel();
            output.write(ByteBuffer.allocate(1), length - 1);
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("total times " + (end - start));
    }

}
