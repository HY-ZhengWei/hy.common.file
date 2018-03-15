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
	 * @param e
	 * @return   返回值表示是否继续拷贝
	 */
	public boolean readBefore(FileReadEvent e);
	
	

	/**
	 * 读取文件内容的进度
	 * 
	 * @param e
	 * @return   返回值表示是否继续拷贝
	 */
	public boolean readProcess(FileReadEvent e);
	
	
	
	/**
	 * 读取文件内容完成之后
	 * 
	 * @param e
	 */
	public void readAfter(FileReadEvent e);
	
}
