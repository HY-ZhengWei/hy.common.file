package org.hy.common.file.event;





/**
 * 解压缩Zip文件(FileHelp)的事件监听器接口
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-07
 */
public interface UnCompressZipListener 
{
	/**
	 * 创建文件之前
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean unCompressZipBefore(UnCompressZipEvent e);
	
	

	/**
	 * 创建文件进度
	 * 
	 * @param e
	 * @return   返回值表示是否继续
	 */
	public boolean unCompressZipProcess(UnCompressZipEvent e);
	
	
	
	/**
	 * 创建文件完成之后
	 * 
	 * @param e
	 */
	public void unCompressZipAfter(UnCompressZipEvent e);

}
