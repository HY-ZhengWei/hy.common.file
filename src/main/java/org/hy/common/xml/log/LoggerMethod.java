package org.hy.common.xml.log;

import java.io.Serializable;





/**
 * 日志统计类：统计粒度：方法
 * 
 * @author      ZhengWei(HY)
 * @createDate  2026-01-17
 * @version     v1.0
 */
public class LoggerMethod implements Serializable
{
    
    private static final long serialVersionUID = 4551271643969861505L;
    
    

    /** 方法平均用时（包括成功及异常情况） */
    private Double execAvgTime;
    
    /** 上一次的方法平均用时（包括成功及异常情况） */
    private Double execAvgTimeOld;
    
    /** 离散度（包括成功及异常情况） */
    private Double dispersion;
    
    /** 上一次的离散度（包括成功及异常情况） */
    private Double dispersionOld;
    
    
    
    public LoggerMethod()
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
        this.execAvgTime    = 0D;
        this.execAvgTimeOld = 0D;
        this.dispersion     = 0D;
        this.dispersionOld  = 0D;
    }

    
    
    /**
     * 获取：方法平均用时（包括成功及异常情况）
     */
    public Double getExecAvgTime()
    {
        return execAvgTime;
    }

    
    /**
     * 设置：方法平均用时（包括成功及异常情况）
     * 
     * @param i_ExecAvgTime 方法平均用时（包括成功及异常情况）
     */
    protected void setExecAvgTime(Double i_ExecAvgTime)
    {
        this.execAvgTime = i_ExecAvgTime;
    }

    
    /**
     * 获取：上一次的方法平均用时（包括成功及异常情况）
     */
    protected Double getExecAvgTimeOld()
    {
        return execAvgTimeOld;
    }

    
    /**
     * 设置：上一次的方法平均用时（包括成功及异常情况）
     * 
     * @param i_ExecAvgTimeOld 上一次的方法平均用时（包括成功及异常情况）
     */
    protected void setExecAvgTimeOld(Double i_ExecAvgTimeOld)
    {
        this.execAvgTimeOld = i_ExecAvgTimeOld;
    }

    
    /**
     * 获取：离散度（包括成功及异常情况）
     */
    public Double getDispersion()
    {
        return dispersion;
    }

    
    /**
     * 设置：离散度（包括成功及异常情况）
     * 
     * @param i_Dispersion 离散度（包括成功及异常情况）
     */
    protected void setDispersion(Double i_Dispersion)
    {
        this.dispersion = i_Dispersion;
    }

    
    /**
     * 获取：上一次的离散度（包括成功及异常情况）
     */
    protected Double getDispersionOld()
    {
        return dispersionOld;
    }

    
    /**
     * 设置：上一次的离散度（包括成功及异常情况）
     * 
     * @param i_DispersionOld 上一次的离散度（包括成功及异常情况）
     */
    protected void setDispersionOld(Double i_DispersionOld)
    {
        this.dispersionOld = i_DispersionOld;
    }
    
}
