package org.hy.common.xml.log;





/**
 * 在替换Log4J 2.x时，只换引包路径即可
 *
 * @author      ZhengWei(HY)
 * @createDate  2020-06-12
 * @version     v1.0
 */
public class LogManager
{
    
    protected LogManager()
    {
        
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J 2.x的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Class
     * @return
     */
    public static Logger getLogger(Class<?> i_Class)
    {
        return getLogger(i_Class ,null);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J 2.x的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Class
     * @param i_IsPrintln  没有任何Log4j版本时，是否采用System.out.println()方法输出
     * @return
     */
    public static Logger getLogger(Class<?> i_Class ,Boolean i_IsPrintln)
    {
        return new Logger(i_Class ,i_IsPrintln);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J 2.x的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_ClassName
     * @return
     */
    public static Logger getLogger(String i_ClassName)
    {
        return getLogger(i_ClassName ,null);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J 2.x的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_ClassName
     * @param i_IsPrintln  没有任何Log4j版本时，是否采用System.out.println()方法输出
     * @return
     */
    public static Logger getLogger(String i_ClassName ,Boolean i_IsPrintln)
    {
        return new Logger(i_ClassName ,i_IsPrintln);
    }
    
}
