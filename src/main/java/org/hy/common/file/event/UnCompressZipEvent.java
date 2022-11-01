package org.hy.common.file.event;

import org.hy.common.BaseEvent;





/**
 * 解压缩Zip文件(FileHelp)的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-07
 */
public class UnCompressZipEvent extends BaseEvent
{
	
	private static final long serialVersionUID = 1636944503027532362L;

	/** 每一个将（或是正在）压缩的文件进度事件 */
	protected UnCompressZipEvent perSource;
	
	/** 是否忽略解压缩过程中的异常(如解压包中的每个文件已存在在保存目录中: 1.无法删除的情况; 2.不允许重复的情况。) */
	protected long               ignoreErrorSize;
	
	
	
	public UnCompressZipEvent(Object i_Source) 
	{
		super(i_Source);
		this.ignoreErrorSize = 0;
	}
	
	
	
	public UnCompressZipEvent(Object i_Source, long i_Size) 
	{
		super(i_Source, i_Size);
		this.ignoreErrorSize = 0;
	}
	
	
	
	public UnCompressZipEvent getPerSource()
	{
		return this.perSource;
	}
	
	
	
	public long getIgnoreErrorSize()
	{
		return this.ignoreErrorSize;
	}
	
	
	
	/**
	 * 判断每个将要解压缩的文件，是否刚刚准备开始进行解压缩。
	 * 
	 * @return
	 */
	public boolean isPerSourceBefore()
	{
		return this.getPerSource().getCompletedSize() == 0;
	}
	
	
	
	/**
	 * 判断每个正在解压缩的文件，是否刚刚完成解压缩。
	 * 
	 * @return
	 */
	public boolean isPerSourceAfter()
	{
		return this.getPerSource().getCompletedSize() == this.getPerSource().getSize();
	}

}
