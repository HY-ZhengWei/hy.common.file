package org.hy.common.ftp.event;

import java.util.EventListener;





/**
 * FTP动作(FTPHelp)的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-03-20
 */
public interface FTPListener extends EventListener
{
    
    /**
     * 传送文件之前
     * 
     * @param e
     * @return   返回值表示是否继续传送
     */
    public boolean ftpBefore(FTPEvent e);
    
    

    /**
     * 传送文件进度
     * 
     * @param e
     * @return   返回值表示是否继续传送
     */
    public boolean ftpProcess(FTPEvent e);
    
    
    
    /**
     * 传送文件完成之后
     * 
     * @param e
     */
    public void ftpAfter(FTPEvent e);
    
}
