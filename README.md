# file-transfer
基于netty实现的文件上传下载 （简单的个人云盘) 
  
上传：http://127.0.0.1:7100/upload?passwd=***  
下载：http://127.0.0.1:7101/?passwd=***

  
config.NettyServerConfig中可以设置上传下载的端口号，文件的目录，访问的密码(passwd)
  
打成jar包执行的时候，命令行参数可参考Main中main方法中的代码。
