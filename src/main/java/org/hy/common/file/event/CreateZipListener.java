package org.hy.common.file.event;





/**
 * 创建Zip文件(FileHelp)的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-04
 */
public interface CreateZipListener 
{
	/**
	 * 创建文件之前
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean createZipBefore(CreateZipEvent e);
	
	

	/**
	 * 创建文件进度
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean createZipProcess(CreateZipEvent e);
	
	
	
	/**
	 * 创建文件完成之后
	 * 
	 * @param e
	 */
	public void createZipAfter(CreateZipEvent e);

}
