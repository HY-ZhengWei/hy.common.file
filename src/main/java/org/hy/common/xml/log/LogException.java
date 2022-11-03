package org.hy.common.xml.log;

import java.io.Serializable;

import org.hy.common.Date;
import org.hy.common.Help;





/**
 * Logger 执行异常日志。
 * 
 * 记录 Warn 、Error、Fatal 三种异常
 *
 * @author      ZhengWei(HY)
 * @createDate  2021-12-24
 * @version     v1.0
 */
public class LogException implements Serializable
{
    
    private static final long serialVersionUID = -9079696035472880985L;

    /** 执行时间。一般执行完成时的时间，或出现异常时的时间 */
    private String time;
    
    /** 执行异常的类型。一般为异常对象的元类类名 */
    private String type;
    
    /** 执行异常信息 */
    private String e;
    
    
    
    public LogException(final String i_Message ,final Throwable i_Throwable)
    {
        this.time = Date.getNowTime().getFullMilli();
        
        if ( i_Throwable != null )
        {
            this.type = i_Throwable.getClass().getName();
            
            if ( !Help.isNull(i_Message) )
            {
                this.e = i_Message + " -> " + i_Throwable.getLocalizedMessage();
            }
            else
            {
                this.e = i_Throwable.getLocalizedMessage();
            }
        }
        else
        {
            this.type = "";
            this.e    = Help.NVL(i_Message);
        }
        
    }
    
    
    
    /**
     * 获取：执行SQL语句
     */
    public String getTime()
    {
        return time;
    }

    
    /**
     * 设置：执行时间
     * 
     * @param time
     */
    public void setTime(String time)
    {
        this.time = time;
    }

    
    /**
     * 获取：执行SQL语句
     */
    public String getType()
    {
        return type;
    }

    
    /**
     * 设置：执行时间
     * 
     * @param time
     */
    public void setType(String type)
    {
        this.type = type;
    }
    

    /**
     * 获取：执行SQL语句
     */
    public String getE()
    {
        return e;
    }

    
    /**
     * 设置：执行时间
     * 
     * @param time
     */
    public void setE(String e)
    {
        this.e = e;
    }
    
}
