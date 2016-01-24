package com.clock.utils.crash;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * app奔溃异常处理器
 * <p/>
 * 使用此类需要在AndroidManifest.xml配置读写SD卡的权限
 * Created by Clock on 2016/1/24.
 */
public class CrashExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");
    /**
     * 默认存放闪退信息的文件夹名称
     */
    private static final String DEFAULT_CRASH_FOLDER_NAME = "Log";
    /**
     * appSD卡默认目录
     */
    private static final String DEFAULT_APP_FOLDER_NAME = "DefaultCrash";


    private Context mApplicationContext;
    /**
     * app在SD卡上的主目录
     */
    private File mAppMainFolder;
    /**
     * 保存闪退日志的文件目录
     */
    private File mCrashInfoFolder;
    /**
     * 是否向远程服务器发送错误信息
     */
    private boolean mReportToServer = false;

    /**
     * @param context
     * @param appMainFolderName   app程序主目录名，配置后位于SD卡一级目录下
     * @param crashInfoFolderName 闪退日志保存目录名，配置后位于 appMainFolderName 配置的一级目录下
     * @param reportToServer      是否向服务器发送奔溃信息
     */
    public CrashExceptionHandler(Context context, String appMainFolderName, String crashInfoFolderName, boolean reportToServer) {
        this.mApplicationContext = context.getApplicationContext();
        if (!TextUtils.isEmpty(appMainFolderName)) {
            this.mAppMainFolder = new File(Environment.getExternalStorageDirectory(), appMainFolderName);
        } else {
            this.mAppMainFolder = new File(Environment.getExternalStorageDirectory(), DEFAULT_APP_FOLDER_NAME);
        }
        if (!TextUtils.isEmpty(crashInfoFolderName)) {
            this.mCrashInfoFolder = new File(mAppMainFolder, crashInfoFolderName);
        } else {
            this.mCrashInfoFolder = new File(mAppMainFolder, DEFAULT_CRASH_FOLDER_NAME);
        }
        mReportToServer = reportToServer;
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
        //System.exit(1);
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
            if (mReportToServer) {
                sendCrashInfoToServer(ex);
            }
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
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                if (!mAppMainFolder.exists()) {//app目录不存在则先创建目录
                    mAppMainFolder.mkdirs();
                }
                if (!mCrashInfoFolder.exists()) {//闪退日志目录不存在则先创建闪退日志目录
                    mCrashInfoFolder.mkdirs();
                }
                String timeStampString = DATE_FORMAT.format(new Date());//当先的时间格式化
                String crashLogFileName = timeStampString + ".log";
                File crashInfoFile = new File(mCrashInfoFolder, crashLogFileName);
                crashInfoFile.createNewFile();
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(crashInfoFile.getAbsolutePath())), true);
                ex.printStackTrace(pw);
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 发送发送闪退信息到远程服务器
     *
     * @param ex
     */
    private void sendCrashInfoToServer(Throwable ex) {

    }
}
