package org.hy.common.file.event;

import java.util.EventListener;





/**
 * 拷贝文件(FileHelp)的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-03
 */
public interface FileCopyListener extends EventListener 
{
	
	/**
	 * 拷贝文件之前
	 * 
	 * @param e
	 * @return   返回值表示是否继续拷贝
	 */
	public boolean copyBefore(FileCopyEvent e);
	
	

	/**
	 * 拷贝文件进度
	 * 
	 * @param e
	 * @return   返回值表示是否继续拷贝
	 */
	public boolean copyProcess(FileCopyEvent e);
	
	
	
	/**
	 * 拷贝文件完成之后
	 * 
	 * @param e
	 */
	public void copyAfter(FileCopyEvent e);
	
}
