
  日志管理类
   引入：
   
   
    allprojects {
    		repositories {
    			maven { url 'https://jitpack.io' }
    		}
    	}



   dependencies {
   
   
   	        implementation 'com.github.csyPasserby:LogManage:v1.0.1'
            
   	}



    使用方法：

  //初始化，建议放在MyApplication初始化，past表示保存几天的日志数据
        LogManageUtils.init(this,PAST);

        //写入数据
        LogManageUtils.write("init..");

        //表示获取当前时间段的日志文件列表。返回fileList，可做上传服务器
        LogManageUtils.getSpellLog(startDate,endDate);
        
        //获取这段时间内的日志文件进行压缩成zip文件。可作为日志上传到服务器
        LogManageUtils.getUploadLogFile(startDate,endDate);
        
        有建议可直接发邮箱779772455@qq.com
