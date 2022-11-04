package org.hy.common.ftp.event;

import org.hy.common.BaseEvent;





/**
 * FTP动作(FTPHelp)的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-03-20
 */
public class FTPEvent extends BaseEvent
{
    
    private static final long serialVersionUID = -471575663529784193L;
    
    /**
     * FTP的动作类型
     * 
     * 1. 上传动作
     * 2. 下载动作
     */
    protected int actionType;
    
    
    
    public FTPEvent(Object i_Source)
    {
        super(i_Source);
    }
    
    
    
    public FTPEvent(Object i_Source, long i_Size)
    {
        super(i_Source, i_Size);
    }
    
    
    
    public int getActionType()
    {
        return this.actionType;
    }
    
}
