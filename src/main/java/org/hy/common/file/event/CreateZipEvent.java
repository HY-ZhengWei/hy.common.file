package org.hy.common.file.event;

import org.hy.common.BaseEvent;





/**
 * 创建Zip文件(FileHelp)的事件
 * 
 * 此类是个只读类，即只有 getter 方法。主要对外提供使用。提供一些个性化的功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-04
 */
public class CreateZipEvent extends BaseEvent 
{
    
    private static final long serialVersionUID = -1867506498707135684L;
    
    /** 每一个将（或是正在）压缩的文件进度事件 */
    protected CreateZipEvent perSource;
    
    /** 忽略原文件异常的次数(如原文件不存、或不可读等异常) */
    protected long           ignoreErrorSize;
    
    

    public CreateZipEvent(Object i_Source) 
    {
        super(i_Source);
        this.ignoreErrorSize = 0;
    }
    
    
    
    public CreateZipEvent(Object i_Source, long i_Size) 
    {
        super(i_Source, i_Size);
        this.ignoreErrorSize = 0;
    }
    
    
    
    public CreateZipEvent getPerSource()
    {
        return this.perSource;
    }
    
    
    
    public long getIgnoreErrorSize()
    {
        return this.ignoreErrorSize;
    }
    
    
    
    /**
     * 判断每个将要压缩的文件，是否刚刚准备开始进行压缩。
     * 
     * @return
     */
    public boolean isPerSourceBefore()
    {
        return this.getPerSource().getCompletedSize() == 0;
    }
    
    
    
    /**
     * 判断每个正在压缩的文件，是否刚刚完成压缩。
     * 
     * @return
     */
    public boolean isPerSourceAfter()
    {
        return this.getPerSource().getCompletedSize() == this.getPerSource().getSize();
    }
    
}
