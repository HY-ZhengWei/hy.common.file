package org.hy.common.file.event;

import org.hy.common.BaseEvent;





/**
 * 创建CSV文件(FileHelp)的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-04
 */
public class CreateCSVEvent extends BaseEvent
{
	
	private static final long serialVersionUID = 4666587001409023439L;
	
	
	
	public CreateCSVEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	public CreateCSVEvent(Object i_Source, long i_Size) 
	{
		super(i_Source, i_Size);
	}
	
}
