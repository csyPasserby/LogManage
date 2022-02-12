package com.csy.logmanageutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 创建时间：2022/2/9
 * 创建人：    csy
 * 联系方式：779772455@qq.com
 */

@SuppressLint({"SimpleDateFormat", "StaticFieldLeak"})
public class LogManageUtils {
    private static String tag = "LogManageUtils";
    private static int PAST = 7;//默认删除不属于PAST内的的日志
    private static Context mContext;
    private static volatile LogManageUtils instance;
    //获取今天的日期，作为文件名
    private static final String TODAY = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    //作为当前时间，写入文件时使用
    private static final SimpleDateFormat logSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static File todayLogFile;

    private LogManageUtils() {

    }

    /**
     *
     * @param context 上下文
     * @param past 保存多少天的日志
     */
    public static void init(Context context,int past) {
        if (null == mContext || null == instance || null == todayLogFile|| !todayLogFile.exists()) {
            Log.i("LogManageUtils", "init");
            mContext = context;
            PAST = past;
            instance = new LogManageUtils();
            todayLogFile = getLogFile();
            deleteSevenDaysAgoLog();
        }

    }


    /**
     *
     * @param str 写入的数据
     */
    public static void write(Object str) {
        if (null == mContext || null == instance || null == todayLogFile|| !todayLogFile.exists())return;
            String logStr = getFunctionInfo() + " - " + str.toString();
            Log.i(tag, logStr);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(todayLogFile, true));
                bw.write(logStr);
                bw.write("\r\n");
                bw.flush();
            } catch (Exception e) {
                Log.e(tag, "Write failure !!! " + e.toString());
            }


    }

    //写入带入当前信息
    private static String getFunctionInfo() {
        StackTraceElement[] sts = Thread.currentThread().getStackTrace();
        for (StackTraceElement st : sts) {
            if (!st.isNativeMethod() && !st.getClassName().equals(Thread.class.getName()) && !st.getClassName().equals(instance.getClass().getName())) {
                tag = st.getFileName();
                return "[" + logSdf.format(new Date()) + " " + st.getClassName() + " Line:" + st.getLineNumber() + "]";
            }
        }

        return null;
    }

    //获取当天文件
    private static File getLogFile() {
        File file;
        if (Environment.getExternalStorageState().equals("mounted")) {
            file = new File(Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/");
        } else {
            file = new File(mContext.getFilesDir().getPath() + "/Log/");
        }

        if (!file.exists()) {
            file.mkdir();
        }

        File logFile = new File(file.getPath() + "/" + TODAY +".txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (Exception exception) {
                Log.e("LogManageUtils", "Create log file failure" + exception.toString());
            }
        }

        return logFile;
    }

    /**
     * 给定开始和结束时间，遍历之间的所有日期
     * @param startAt 开始时间，例：2017-04-04
     * @param endAt   结束时间，例：2017-04-11
     * @return 返回日期数组
     */
    public static List<String> queryData(String startAt, String endAt) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = null;
        Date endDate =null;
        try {
            startDate = dateFormat.parse(startAt);
            endDate = dateFormat.parse(endAt);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        List<String> dates = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        assert startDate != null;
        start.setTime(startDate);
        Calendar end = Calendar.getInstance();
        assert endDate != null;
        end.setTime(endDate);
        while (start.before(end) || start.equals(end)) {
            dates.add(dateFormat.format(start.getTime()));
            start.add(Calendar.DAY_OF_YEAR, 1);
        }
        return dates;
    }

    /**
     *
     * @param startDate 开始日期
     * @param endingDate 结束日期
     * @return 日志的压缩文件
     */
    public static File getUploadLogFile(String startDate, String endingDate){
       List<File> fileList = getSpellLog(startDate,endingDate);
        File file;
        if (Environment.getExternalStorageState().equals("mounted")) {
            file = new File(Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/upload/");
        } else {
            file = new File(mContext.getFilesDir().getPath() + "/Log/upload/");
        }
        if (!file.exists()) {
            file.mkdir();
        }
        for (File file1 : fileList) {
            File lastLogFile = new File(file.getPath()+ "/"+file1.getName());
         boolean isCopy = copyFile(file1.getPath(),lastLogFile.getPath());
         //把文件复制到另外的upload文件夹下
         if (!isCopy){
             //如果不成功则进行移动
             file1.renameTo(lastLogFile);
         }
        }
        String uploadLogZip = null;
        try {

            if (Environment.getExternalStorageState().equals("mounted")) {
                 uploadLogZip = Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/upload.zip";
                //压缩目录
            } else {
                uploadLogZip = mContext.getFilesDir().getPath() + "/Log/upload.zip";
            }
            ZipFolder(file.getPath(),uploadLogZip);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        assert uploadLogZip != null;
        return new File(uploadLogZip);
    }

    /**
     *
     * @param startDate  开始日期
     * @param endingDate 结束日期
     * @return  获取日期范围内的日志文件
     */
    public static List<File> getSpellLog(String startDate, String endingDate){
        List<File> fileLists = new ArrayList<>(); //最终筛选出来的文件列表
        File file;
        if (Environment.getExternalStorageState().equals("mounted")) {
            file = new File(Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/");
        } else {
            file = new File(mContext.getFilesDir().getPath() + "/Log/");
        }
        if (!file.exists()) {
            file.mkdir();
        }
       File[] logFileList = file.listFiles();
        //获取Log文件夹下所有的文件
        for (File file1 : Objects.requireNonNull(logFileList)) {

            if (isDateScope(startDate,endingDate,file1.getName())){
                fileLists.add( new File(file.getPath() + "/" + file1.getName() ));
            }
        }
        return fileLists;
    }

    //判断当前日期是否属于日期范围内

    /**
     *
     * @param startDate  开始日期
     * @param endingDate  结束日期
     * @param date  匹配日期
     * @return  返回匹配日期是否属于日期范围内
     */
    private static boolean isDateScope(String startDate, String endingDate, String date){
       List<String> dateLists = queryData(startDate,endingDate);
        for (String dateList : dateLists) {
           if (date.contains(dateList)){
               return true;
           }
        }

        return false;
    }
    //默认删除七天前的日志
    private static void deleteSevenDaysAgoLog(){
        File file;
        if (Environment.getExternalStorageState().equals("mounted")) {
            file = new File(Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/");
        } else {
            file = new File(mContext.getFilesDir().getPath() + "/Log/");
        }
        if (!file.exists()) {
            file.mkdir();
        }
        File[] logFileList = file.listFiles();
        //获取Log文件夹下所有的文件
        for (File file1 : Objects.requireNonNull(logFileList)) {
            if (!isDateScope(getPastDate(PAST),TODAY,file1.getName())){
                //删除不属于7天内文件

                if (file1.exists()) {
                    file1.delete();
                }
            }
        }
        File UploadDirectoryFile;
        if (Environment.getExternalStorageState().equals("mounted")) {
            UploadDirectoryFile = new File(Objects.requireNonNull(mContext.getExternalFilesDir("Log")).getPath() + "/upload/");
        } else {
            UploadDirectoryFile = new File(mContext.getFilesDir().getPath() + "/Log/upload/");
        }
        if (UploadDirectoryFile.exists()) {
            File[] UploadDirectoryFileList = UploadDirectoryFile.listFiles();
            if (UploadDirectoryFileList != null) {
                for (File file1 : UploadDirectoryFileList) {
                    if (file1.exists()) {
                        file1.delete();
                    }
                }
            }
        }
    }

    /**
     *
     * @param past  获取过去第几天的日期
     * @return  返回过去第几天的日期
     */
    public static String getPastDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(today);
    }

    /**
     *
     * @param oldPathName 需要被复制的文件路径
     * @param newPathName 复制到指定路径
     * @return 返回是否复制成功
     */
    public static boolean copyFile(String oldPathName, String newPathName) {
        try {
            File oldFile = new File(oldPathName);
        if (!oldFile.exists() || !oldFile.isFile() || !oldFile.canRead()) {
            return false;
        }
            FileInputStream fileInputStream = new FileInputStream(oldPathName);    //读入原文件
            FileOutputStream fileOutputStream = new FileOutputStream(newPathName);
            byte[] buffer = new byte[1024];
            int byteRead;
            while ((byteRead = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, byteRead);
            }
            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 压缩文件和文件夹
     *
     * @param srcFileString 要压缩的文件或文件夹
     * @param zipFileString 压缩完成的Zip路径
     * @throws Exception  抛出异常
     */
    public static void ZipFolder(String srcFileString, String zipFileString) throws Exception {
        //创建ZIP
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFileString));
        //创建文件
        File file = new File(srcFileString);
        //压缩
        ZipFiles(file.getParent()+ File.separator, file.getName(), outZip);
        //完成和关闭
        outZip.finish();
        outZip.close();
    }
    /**
     * 压缩文件
     *
     * @param folderString 文件夹路径
     * @param fileString    文件名
     * @param zipOutputSteam 输入的文件
     * @throws Exception  压缩异常抛出
     */
    private static void ZipFiles(String folderString, String fileString, ZipOutputStream zipOutputSteam) throws Exception {
        if (zipOutputSteam == null)
            return;
        File file = new File(folderString + fileString);
        if (file.isFile()) {
            ZipEntry zipEntry = new ZipEntry(fileString);
            FileInputStream inputStream = new FileInputStream(file);
            zipOutputSteam.putNextEntry(zipEntry);
            int len;
            byte[] buffer = new byte[4096];
            while ((len = inputStream.read(buffer)) != -1) {
                zipOutputSteam.write(buffer, 0, len);
            }
            zipOutputSteam.closeEntry();
        } else {
            //文件夹
            String[] fileList = file.list();
            //没有子文件和压缩
            assert fileList != null;
            if (fileList.length <= 0) {
                ZipEntry zipEntry = new ZipEntry(fileString + File.separator);
                zipOutputSteam.putNextEntry(zipEntry);
                zipOutputSteam.closeEntry();
            }
            //子文件和递归
            for (String s : fileList) {
                ZipFiles(folderString + fileString + "/", s, zipOutputSteam);
            }
        }
    }
}
