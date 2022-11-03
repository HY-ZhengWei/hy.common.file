package org.hy.common.xml.log;

import org.hy.common.Help;





/**
 * 日志的堆处理类
 *
 * @author      ZhengWei(HY)
 * @createDate  2020-06-12
 * @version     v1.0
 */
public class LogStackTrace
{
    
    protected LogStackTrace()
    {
        
    }
    
    
    
    /**
     * 计算类在堆信息的位置
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @param i_FQCN
     * @return
     */
    public static StackTraceElement calcLocation(final String i_FQCN) 
    {
        if ( Help.isNull(i_FQCN) ) 
        {
            return null;
        }
        
        try
        {
            final StackTraceElement [] v_StackTrace = new Throwable().getStackTrace();
            boolean                    v_IsFound    = false;
            
            for (int i = 0; i < v_StackTrace.length; i++)
            {
                final String v_ClassName = v_StackTrace[i].getClassName();
                
                if ( i_FQCN.equals(v_ClassName) )
                {
                    v_IsFound = true;
                }
                else if ( v_IsFound )
                {
                    return v_StackTrace[i];
                }
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return null;
    }
    
}
