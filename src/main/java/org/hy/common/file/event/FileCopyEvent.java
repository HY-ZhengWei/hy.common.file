package org.hy.common.file.event;

import org.hy.common.BaseEvent;





/**
 * 文件拷贝(FileHelp)的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-03
 */
public class FileCopyEvent extends BaseEvent
{
	
	private static final long serialVersionUID = -471575663529784193L;
	
	
	
	public FileCopyEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	public FileCopyEvent(Object i_Source, long i_Size) 
	{
		super(i_Source, i_Size);
	}
	
}
