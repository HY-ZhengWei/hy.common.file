package org.hy.common.xml.log;

import java.io.Serializable;





/**
 * 日志级别
 *
 * @author      ZhengWei(HY)
 * @createDate  2020-06-15
 * @version     v1.0
 */
public class Level implements Serializable
{

    private static final long serialVersionUID = 8625887890964626599L;
    
    /** 请按Log4J、SLF4J的日志级别传参 */
    private Object level;

    
    
    public Level(Object i_Level)
    {
        this.level = i_Level;
    }
    
    
    
    /**
     * 获取：请按Log4J、SLF4J的日志级别传参
     */
    public Object getLevel()
    {
        return level;
    }



    @Override
    public int hashCode()
    {
        return this.level.hashCode();
    }



    @Override
    public boolean equals(Object i_Other)
    {
        if ( this == i_Other )
        {
            return true;
        }
        else if ( i_Other == null )
        {
            return false;
        }
        else if ( i_Other instanceof Level )
        {
            // 主要用于解决不同日志类库的日志级别不一样的问题。如SLF4J有没有Fatal级
            // 不用这样的判定：his.level.equals(((Level)i_Other).getLevel())
            return false;
        }
        else
        {
            return this.level.equals(i_Other);
        }
    }
    
}
