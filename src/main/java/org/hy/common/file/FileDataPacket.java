package org.hy.common.file;

import java.io.Serializable;





/**
 * 文件数据包
 *
 * @author      ZhengWei(HY)
 * @createDate  2017-10-09
 * @version     v1.0
 *              v2.0  2019-08-26  修正：size改成Long类型
 */
public class FileDataPacket implements Serializable
{
    
    private static final long serialVersionUID = -4118023882981600258L;

    /** 时间 */
    private Long    time;
    
    /** 文件名称 */
    private String  name;
    
    /** 文件的总大小 */
    private Long    size;
    
    /** 数据包的总数量 */
    private Integer dataCount;

    /** 数据包的信息 */
    private byte [] dataByte;
    
    /** 数据包的大小 */
    private Integer dataSize;

    /** 数据包的序号。下标从1开始 */
    private Integer dataNo;

    
    
    /**
     * 获取：时间
     */
    public Long getTime()
    {
        return time;
    }
    

    
    /**
     * 设置：时间
     * 
     * @param time 
     */
    public void setTime(Long time)
    {
        this.time = time;
    }
    

    
    /**
     * 获取：文件名称
     */
    public String getName()
    {
        return name;
    }
    

    
    /**
     * 设置：文件名称
     * 
     * @param name 
     */
    public void setName(String name)
    {
        this.name = name;
    }
    

    
    /**
     * 获取：文件的总大小
     */
    public Long getSize()
    {
        return size;
    }
    

    
    /**
     * 设置：文件的总大小
     * 
     * @param size 
     */
    public void setSize(Long size)
    {
        this.size = size;
    }
    

    
    /**
     * 获取：数据包的总数量
     */
    public Integer getDataCount()
    {
        return dataCount;
    }
    

    
    /**
     * 设置：数据包的总数量
     * 
     * @param dataCount 
     */
    public void setDataCount(Integer dataCount)
    {
        this.dataCount = dataCount;
    }
    

    
    /**
     * 获取：数据包的信息(另一种表达形式)
     */
    public byte [] getDataByte()
    {
        return dataByte;
    }

    
    
    /**
     * 设置：数据包的信息(另一种表达形式)
     * 
     * @param dataByte 
     */
    public void setDataByte(byte [] dataByte)
    {
        this.dataByte = dataByte;
    }
    
    

    /**
     * 获取：数据包的大小
     */
    public Integer getDataSize()
    {
        return dataSize;
    }
    

    
    /**
     * 设置：数据包的大小
     * 
     * @param dataSize 
     */
    public void setDataSize(Integer dataSize)
    {
        this.dataSize = dataSize;
    }
    

    
    /**
     * 获取：数据包的序号。下标从1开始
     */
    public Integer getDataNo()
    {
        return dataNo;
    }
    

    
    /**
     * 设置：数据包的序号。下标从1开始
     * 
     * @param dataNo 
     */
    public void setDataNo(Integer dataNo)
    {
        this.dataNo = dataNo;
    }
    
}
