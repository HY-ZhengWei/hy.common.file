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
 */
public class FileReadEvent extends BaseEvent
{
	
	private static final long serialVersionUID = -471575663529784193L;
	
	
	
	public FileReadEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	public FileReadEvent(Object i_Source, long i_Size) 
	{
		super(i_Source, i_Size);
	}
	
}
