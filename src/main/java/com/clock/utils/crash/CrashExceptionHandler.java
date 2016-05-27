package com.clock.utils.crash;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.clock.utils.common.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * app奔溃异常处理器
 * <p/>
 * 使用此类需要在AndroidManifest.xml配置以下权限
 * <p/>
 * <bold>android.permission.READ_EXTERNAL_STORAGE</bold>
 * <p/>
 * <bold>android.permission.WRITE_EXTERNAL_STORAGE</bold>
 * <p/>
 * <bold>android.permission.READ_PHONE_STATE</bold>
 * <p/>
 * Created by Clock on 2016/1/24.
 */
public class CrashExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final static String TAG = CrashExceptionHandler.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    private Context mApplicationContext;
    /**
     * 保存闪退日志的文件目录
     */
    private File mCrashInfoFolder;
    /**
     * 向远程服务器发送错误信息
     */
    private CrashExceptionRemoteReport mCrashExceptionRemoteReport;

    /**
     * @param context
     * @param crashInfoFolder 保存闪退日志的文件夹目录
     */
    public CrashExceptionHandler(Context context, File crashInfoFolder) {
        this.mApplicationContext = context.getApplicationContext();
        this.mCrashInfoFolder = crashInfoFolder;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        ex.printStackTrace();
        handleException(ex);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //杀死进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 配置远程传回log到服务器的设置
     *
     * @param crashExceptionRemoteReport
     */
    public void configRemoteReport(CrashExceptionRemoteReport crashExceptionRemoteReport) {
        this.mCrashExceptionRemoteReport = crashExceptionRemoteReport;
    }

    /**
     * 处理异常
     *
     * @param ex
     */
    private void handleException(Throwable ex) {
        if (ex == null) {
            return;
        } else {
            saveCrashInfoToFile(ex);
            sendCrashInfoToServer(ex);

            //使用Toast来显示异常信息
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    try {
                        Toast.makeText(mApplicationContext, "程序出现异常 , 即将退出....", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Looper.loop();
                }
            }.start();
        }
    }

    /**
     * 保存闪退信息到本地文件中
     *
     * @param ex
     */
    private void saveCrashInfoToFile(Throwable ex) {
        try {
            if (mCrashInfoFolder == null) {
                return;
            }

            if (!mCrashInfoFolder.exists()) {//闪退日志目录不存在则先创建闪退日志目录
                mCrashInfoFolder.mkdirs();
            }

            if (mCrashInfoFolder.exists()) {
                String timeStampString = DATE_FORMAT.format(new Date());//当先的时间格式化
                String crashLogFileName = timeStampString + ".log";
                File crashLogFile = new File(mCrashInfoFolder, crashLogFileName);
                crashLogFile.createNewFile();

                //记录闪退环境的信息
                RandomAccessFile randomAccessFile = new RandomAccessFile(crashLogFile, "rw");
                randomAccessFile.writeChars("------------Crash Environment Info------------" + "\n");
                randomAccessFile.writeChars("------------Manufacture: " + SystemUtils.getDeviceManufacture() + "------------" + "\n");
                randomAccessFile.writeChars("------------DeviceName: " + SystemUtils.getDeviceName() + "------------" + "\n");
                randomAccessFile.writeChars("------------SystemVersion: " + SystemUtils.getSystemVersion() + "------------" + "\n");
                randomAccessFile.writeChars("------------DeviceIMEI: " + SystemUtils.getDeviceIMEI(mApplicationContext) + "------------" + "\n");
                randomAccessFile.writeChars("------------AppVersion: " + SystemUtils.getAppVersion(mApplicationContext) + "------------" + "\n");
                randomAccessFile.writeChars("------------Crash Environment Info------------" + "\n");
                randomAccessFile.writeChars("\n");
                randomAccessFile.close();

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(crashLogFile.getAbsolutePath(), true)), true);
                ex.printStackTrace(pw);//写入奔溃的日志信息
                pw.close();

            } else {
                Log.e(TAG, "crash info folder create failure!!!");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送发送闪退信息到远程服务器
     *
     * @param ex
     */
    private void sendCrashInfoToServer(Throwable ex) {
        if (mCrashExceptionRemoteReport != null) {
            mCrashExceptionRemoteReport.onCrash(ex);
        }
    }

    /**
     * 闪退日志远程奔溃接口，主要考虑不同app下，把log回传给服务器的方式不一样，所以此处留一个对外开放的接口
     */
    public static interface CrashExceptionRemoteReport {
        /**
         * 当闪退发生时，回调此接口函数
         *
         * @param ex
         */
        public void onCrash(Throwable ex);
    }
}
