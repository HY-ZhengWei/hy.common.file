package org.hy.common.xml.log;

import java.io.Serializable;





/**
 * 日志统计类：统计粒度：方法 + 线程
 *
 * @author      ZhengWei(HY)
 * @createDate  2026-01-17
 * @version     v1.0
 */
public class LoggerMethodThread implements Serializable
{
    
    private static final long serialVersionUID = 8492880039683720829L;
    
    

    /** 代码行号。用于统计方法执行累计用时的方法最后执行行号，计算线程、方法、行号三者的关系 */
    private Integer lineNumber;
    
    /** 开始执行时间 */
    private Long    execStartTime;
    
    /** 最后执行时间 */
    private Long    execLastTime;
    
    
    
    public LoggerMethodThread()
    {
        this.reset();
    }
    
    
    
    /**
     * 重置统计数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-01-17
     * @version     v1.0
     *
     */
    public void reset()
    {
        this.lineNumber    = null;
        this.execStartTime = null;
        this.execLastTime  = null;
    }

    
    
    /**
     * 获取：代码行号。用于统计方法执行累计用时的方法最后执行行号，计算线程、方法、行号三者的关系
     */
    public Integer getLineNumber()
    {
        return lineNumber;
    }

    
    /**
     * 设置：代码行号。用于统计方法执行累计用时的方法最后执行行号，计算线程、方法、行号三者的关系
     * 
     * @param i_LineNumber 代码行号。用于统计方法执行累计用时的方法最后执行行号，计算线程、方法、行号三者的关系
     */
    public void setLineNumber(Integer i_LineNumber)
    {
        this.lineNumber = i_LineNumber;
    }

    
    /**
     * 获取：开始执行时间
     */
    public Long getExecStartTime()
    {
        return execStartTime;
    }

    
    /**
     * 设置：开始执行时间
     * 
     * @param i_ExecStartTime 开始执行时间
     */
    public void setExecStartTime(Long i_ExecStartTime)
    {
        this.execStartTime = i_ExecStartTime;
    }

    
    /**
     * 获取：最后执行时间
     */
    public Long getExecLastTime()
    {
        return execLastTime;
    }

    
    /**
     * 设置：最后执行时间
     * 
     * @param i_ExecLastTime 最后执行时间
     */
    public void setExecLastTime(Long i_ExecLastTime)
    {
        this.execLastTime = i_ExecLastTime;
    }
    
    
    /**
     * 计算执行用时时长。
     * 
     *   注：请外界确保非空
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-01-17
     * @version     v1.0
     *
     * @return
     */
    public Long getExecTimeLen()
    {
        return this.execLastTime.longValue() - this.execStartTime.longValue();
    }
    
}
