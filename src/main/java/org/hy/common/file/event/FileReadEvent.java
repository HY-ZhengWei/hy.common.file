package org.hy.common.file.event;

import org.hy.common.BaseEvent;





/**
 * 读取文件内容(FileHelp.getContent())的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-03-15
 * @version     v1.0
 *              v2.0  2021-06-27  添加：读取事件的所有监听器，均可以影响读取结果
 */
public class FileReadEvent extends BaseEvent
{
    
    private static final long serialVersionUID = -471575663529784193L;
    
    
    /** 读取到的文件内容（部分内容，内容大小与FileHelp.bufferSize有关） */
    protected byte [] dataByte;
    
    /** 读取到的文件内容（部分内容，内容大小与FileHelp.bufferSize有关） */
    protected String  dataString;
    
    
    
    public FileReadEvent(Object i_Source)
    {
        super(i_Source);
    }
    
    
    
    public FileReadEvent(Object i_Source, long i_Size)
    {
        super(i_Source, i_Size);
    }


    
    /**
     * 获取：读取到的文件内容（部分内容，内容大小与FileHelp.bufferSize有关）
     */
    public byte [] getDataByte()
    {
        return dataByte;
    }
    

    
    /**
     * 获取：读取到的文件内容（部分内容，内容大小与FileHelp.bufferSize有关）
     */
    public String getDataString()
    {
        return dataString;
    }
    
    
    
    public void setDataString(String i_Data)
    {
        this.dataString = i_Data;
    }
    
    
    public void setDataByte(byte [] i_Data)
    {
        this.dataByte = i_Data;
    }
    
}
