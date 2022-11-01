package org.hy.common.file.event;

import java.util.EventListener;





/**
 * 读取文件内容(FileHelp.getContent())的事件监听器接口
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-03-15
 * @version     v1.0
 */
public interface FileReadListener extends EventListener
{
    
    /**
     * 读取文件内容之前
     * 
     * @param i_Event
     * @return          返回值表示是否继续拷贝
     */
    public boolean readBefore(FileReadEvent i_Event);
    
    

    /**
     * 读取文件内容的进度
     * 
     * @param io_Event  读取事件的所有监听器，均可以影响读取结果
     * @return          返回值表示是否继续拷贝
     */
    public boolean readProcess(FileReadEvent io_Event);
    
    
    
    /**
     * 读取文件内容完成之后
     * 
     * @param i_Event
     */
    public void readAfter(FileReadEvent i_Event);
    
}
