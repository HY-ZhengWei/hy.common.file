package org.hy.common.xml.junit.log;

import org.hy.common.xml.log.Logger;
import org.hy.common.xml.log.LoggerFactory;
import org.junit.Test;





public class JU_Log_IsSame
{
    
    private static final Logger $Logger = LoggerFactory.getLogger(JU_Log_IsSame.class);
    
    
    
    @Test
    public void test_Log_IsSame()
    {
        $Logger.info("info msg");
        $Logger.debug("debug msg");
        $Logger.warn("warn msg");
        $Logger.error("error msg");
        
        $Logger.info("info msg" ,"123456");
        $Logger.info("info msg" ,"123456" ,"654321");
    }
    
}
