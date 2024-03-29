package org.hy.common.file.event;

import org.hy.common.Date;





/**
 * 文件拷贝(FileHelp)的事件的默认实现
 * 
 * 此类中可以有 setter 方法，主要用于内部。
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-03
 */
public class DefaultFileCopyEvent extends FileCopyEvent 
{
	
	private static final long serialVersionUID = 4754268007015014587L;
	
	
	
	public DefaultFileCopyEvent(Object i_Source) 
	{
		super(i_Source);
	}
	
	
	
	public DefaultFileCopyEvent(Object i_Source ,long i_FileSize) 
	{
		super(i_Source ,i_FileSize);
	}

	
	
	public void setBeginTime(Date i_BeginTime)
	{
		this.beginTime = i_BeginTime;
	}
	
	
	
	public void setBeginTime()
	{
		this.setBeginTime(new Date());
	}
	
	
	
	public void setEndTime(Date i_EndTime) 
	{
		this.endTime = i_EndTime;
	}
	
	
	
	public void setEndTime()
	{
		this.setEndTime(new Date());
	}
	
	
	
	public void setInfo(String i_Info)
    {
        this.info = i_Info;
    }
	
	
	
	public void setSize(long i_Size)
	{
		this.size = i_Size;
	}
	
	
	
	public void setCompleteSize(long i_CompleteSize) 
	{
		this.completedSize = i_CompleteSize;
	}
	
	
	
	/**
	 * 成功完成
	 */
	public void setSucceedFinish()
	{
		this.setEndTime();
		this.setCompleteSize(this.getSize());
	}
	
}
