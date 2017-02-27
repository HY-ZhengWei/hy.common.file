package org.hy.common.file.event;

import java.util.EventListener;





/**
 * 创建CSV文件(FileHelp)的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-04
 */
public interface CreateCSVListener extends EventListener 
{
	
	/**
	 * 创建文件之前
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean createCSVBefore(CreateCSVEvent e);
	
	

	/**
	 * 创建文件进度
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean createCSVProcess(CreateCSVEvent e);
	
	
	
	/**
	 * 创建文件完成之后
	 * 
	 * @param e
	 */
	public void createCSVAfter(CreateCSVEvent e);
	
}
