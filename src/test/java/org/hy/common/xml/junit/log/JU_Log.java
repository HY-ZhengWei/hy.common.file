package org.hy.common.xml.junit.log;

import org.hy.common.xml.log.Logger;
import org.hy.common.xml.log.LoggerFactory;





public class JU_Log
{
    
    public static void main(String[] args)
    {
        // 如果配置文件名为log4j.properties，且放在类加载路径下，以下两行代码可以不写
        // URL url = ClassLoader.getSystemResource("log4j.properties");
        // PropertyConfigurator.configure(url);
        
        Logger logger = LoggerFactory.getLogger(JU_Log.class);
//        logger.fatal("fatal msg");
//        logger.error("error msg");
//        logger.warn("warn msg");
//        logger.info("info msg");
//        logger.debug("debug msg");
//        logger.trace("trace msg");
//
//        logger.fatal("fatal msg" ,"123456");
//        logger.fatal("fatal msg" ,"123456" ,"654321");
        
        //logger.info("工作台：用户{}进入log4j\n", "123456");
        
        logger.info("查询用户{}的{}{}数据：{}" ,"1", "2", "3" ,"4");
        
        new JU_Log_IsSame().test_Log_IsSame();
    }
    
}
