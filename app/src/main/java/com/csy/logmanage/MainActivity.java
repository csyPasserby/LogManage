package com.csy.logmanage;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.csy.logmanageutils.LogManageUtils;

public class MainActivity extends AppCompatActivity {
    private final int PAST = 7;
    private final String startDate = "2022-02-10";
    private final  String endDate = "2022-02-12";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化，建议放在MyApplication初始化，past表示保存几天的日志数据
        LogManageUtils.init(this,PAST);
        //写入数据
        LogManageUtils.write("init..");
        //表示获取当前时间段的日志文件列表。返回fileList，可做上传服务器
        LogManageUtils.getSpellLog(startDate,endDate);
        //获取这段时间内的日志文件进行压缩成zip文件。可作为日志上传到服务器
        LogManageUtils.getUploadLogFile(startDate,endDate);

    }
}