package org.hy.common.xml.junit.log;

import org.hy.common.xml.log.LogStackTrace;
import org.junit.Test;

public class JU_LogStackTrace
{
    
    @Test
    public void test_LogStackTrace()
    {
        LogStackTrace.calcLocation("org.hy.common.xml.log.LogStackTrace");
    }
    
}
