package org.hy.common.xml.log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hy.common.Counter;
import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.PartitionMap;
import org.hy.common.StaticReflect;
import org.hy.common.StringHelp;
import org.hy.common.TablePartition;
import org.hy.common.TablePartitionBusway;
import org.hy.common.file.FileHelp;





/**
 * 日志引擎（日志门面）。松耦合的日志引擎，不强行引用任何第三方类库。
 * 
 * 支持Log4j 1.x 和 Log4j 2.x 两个版本的功能。
 * 
 * 支持SLF4J日志引擎的类库。
 * 
 * 支持Logback的日志类库
 * 
 * 支持没有第三方（Log4J、SLF4J、Logback）引包的情况，使用System.out输出日志。
 * 
 * 
 * 注意1：当 Log4j 2.x 与 Log4j 1.x 两版本的引包均存在时，优先使用高版本 Log4j 2.x 。
 *
 * @author      ZhengWei(HY)
 * @createDate  2019-05-27
 * @version     v1.0
 *              v2.0  2020-01-06  添加：在没有任何Log4j版本时，可采用System.out.println()方法输出
 *              v3.0  2020-06-09  添加：info(String ,Throwable)系列的日志方法。建议人：邹德福
 *                                添加：info(Throwable)系列的日志方法。建议人：邹德福
 *                                添加：info(Object)系列的日志方法。建议人：邹德福
 *                                添加：支持SLF4J的日志类库。建议人：邹德福
 *              v4.0  2020-06-10  添加：Log4j出于性能考虑，公开了判定日志级别的方法。建议人：李浩
 *              v5.0  2020-06-11  优化：内存优化、执行性能的优化、代码优雅的优化
 *              v5.1  2020-06-12  添加：日志集中管理机制；对外提供日志级别等更多方法。
 *              v5.2  2020-06-15  添加：封装日志级别。原本不用封装日志级别，直接引用Log4J、SLF4J也是可以的。
 *                                      主要用于解决不同日志类库的日志级别不一样的问题。如SLF4J有没有Fatal级。
 *              v5.3  2020-06-16  添加：区分SLF4J是引用Log4J，还是引用Logback。
 *              v6.0  2020-06-20  添加：通过方法内两次及以上的多次日志输出，尝试计算出方法执行用时。
 *                                      建议人：李浩; 解决方案：程志华
 *              v7.0  2020-06-25  添加：无日志组件的日志输出提示
 */
public class Logger
{
    
    /** 全局控制参数：是否启用SLF4J。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    private static boolean                             $IsEnabled_SLF4J = true;
    
    /** 全局控制参数：是否启用Log4J。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    private static boolean                             $IsEnabled_Log4J = true;
    
    /** 全局控制参数：是否启用System.out.println输出日志。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    private static boolean                             $IsEnabled_Print = false;
    
    
    
    /** 常量：日志引擎的类型为：SLF4J */
    public static final int                            $LogType_SLF4J = 1;
    
    /** 常量：日志引擎的类型为：Log4J */
    public static final int                            $LogType_Log4J = 2;
    
    private static final String                        $FQCN = Logger.class.getName();
    
    
    
    /** 日志实现类库的类型（1：SLF4J  2：Log4J） */
    private static int                                 $LogType    = -1;
                                                       
    /** 日志实现类库的版本 */
    private static int                                 $LogVersion = -1;
                                                       
    private static Class<?>                            $LogClass;
                                                       
    private static Class<?>                            $LogManager;
    
    /** 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了 */
    private static Level                               $LogLevelFatal;
                          
    /** 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别 */
    private static Level                               $LogLevelError;
                          
    /** 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。 */
    private static Level                               $LogLevelWarn;
                          
    /** 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息 */
    private static Level                               $LogLevelInfo;
                          
    /** 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息 */
    private static Level                               $LogLevelDebug;
                                                       
    /** 最低的日志级别 */
    private static Level                               $LogLevelTrace;
                                                       
    private static Method                              $FatalIsEnabled;
                                                       
    private static Method                              $ErrorIsEnabled;
                                                       
    private static Method                              $WarnIsEnabled;
                                                       
    private static Method                              $InfoIsEnabled;
                                                       
    private static Method                              $DebugIsEnabled;
                                                       
    private static Method                              $TraceIsEnabled;
                                                       
    private static Method                              $LogMethodNull;
                                                       
    private static Method                              $LogMethod;
                                                       
    private static Method                              $LogMethod_Log4j2Throwable;
                                                       
    /**
     * 全部日志处理类的集合。可用于日志分析
     * 
     * Map.key  为分区标示。为使用日志引擎的类名称
     */
    private static PartitionMap<String ,Logger>        $Loggers = new TablePartition<String ,Logger>();
                                                       
                                                       
                                                       
    private Object                                     log;
    
    /** 没有任何Log4j版本时，是否采用System.out.println()方法输出 */
    private Class<?>                                   logClass;
    
    /**
     * 统计日志执行次数
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：日志级别:方法名称:代码行
     *   Map.Value 是：累计执行次数
     */
    private Counter<String>                            requestCount;
    
    /**
     * 统计日志最后执行时间
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：日志级别:方法名称:代码行
     *   Map.Value 是：最后执行时间
     */
    private Map<String ,Long>                          requestTime;
    
    /**
     * 记录异常日志的具体内容
     * 
     * Map.Key   是：日志级别:方法名称:代码行
     * Map.Value 是：异常对象
     */
    private TablePartitionBusway<String ,LogException> execptionLog;
    
    /**
     * 统计方法执行累计用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称 + 线程号
     *   Map.Value 是：累计用时
     */
    private Counter<String>                            methodExecSumTime;
    
    /**
     * 用于统计方法执行累计用时的方法最后执行行号，计算线程、方法、行号三者的关系。
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称 + 线程号
     *   Map.Value 是：代码行号
     * 
     * 仅限内部使用
     */
    private Map<String ,Integer>                       methodExecLines;
    
    /**
     * 用于统计方法执行累计用时的方法最后执行时间
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称 + 线程号
     *   Map.Value 是：最后执行时间
     * 
     * 仅限内部使用
     */
    private Map<String ,Long>                          methodExecLastime;
    
    
    
    static
    {
        try
        {
            $LogMethodNull = Logger.class.getMethod("toString");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    
    /**
     * 使用SLF4J输出日志。
     * 
     * 目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的
     * 对于Web项目，请在web.xml的第一个Listener中调用此方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     */
    public static void useSLF4J()
    {
        $IsEnabled_SLF4J = true;
        $IsEnabled_Log4J = false;
        $IsEnabled_Print = false;
    }
    
    
    
    /**
     * 使用SLF4J+Logback输出日志。
     * 
     * 目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的
     * 对于Web项目，请在web.xml的第一个Listener中调用此方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-16
     * @version     v1.0
     *
     */
    public static void useLogback()
    {
        $IsEnabled_SLF4J = true;
        $IsEnabled_Log4J = false;
        $IsEnabled_Print = false;
    }
    
    
    
    /**
     * 使用Log4J输出日志。
     * 
     * 目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的
     * 对于Web项目，请在web.xml的第一个Listener中调用此方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     */
    public static void useLog4J()
    {
        $IsEnabled_SLF4J = false;
        $IsEnabled_Log4J = true;
        $IsEnabled_Print = false;
    }
    
    
    
    /**
     * 使用System.out.println输出日志。
     * 
     * 目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的
     * 对于Web项目，请在web.xml的第一个Listener中调用此方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     */
    public static void usePrint()
    {
        $IsEnabled_SLF4J = false;
        $IsEnabled_Log4J = false;
        $IsEnabled_Print = true;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelFatal()
    {
        return $LogLevelFatal;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelError()
    {
        return $LogLevelError;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelWarn()
    {
        return $LogLevelWarn;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelnfo()
    {
        return $LogLevelInfo;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelDebug()
    {
        return $LogLevelDebug;
    }
    
    
    
    /**
     * 获取实际Log4J 1.x、2.x 或 SLF4J的日志级别对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static Object getLevelTrace()
    {
        return $LogLevelTrace;
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Class
     * @return
     */
    public static Logger getLogger(Class<?> i_Class)
    {
        return new Logger(i_Class.getName() ,null);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J的构建
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Class
     * @param i_IsPrintln  没有任何Log4j版本时，是否采用System.out.println()方法输出
     * @return
     */
    public static Logger getLogger(Class<?> i_Class ,Boolean i_IsPrintln)
    {
        return new Logger(i_Class.getName() ,i_IsPrintln);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J的构建
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
        return new Logger(i_ClassName ,null);
    }
    
    
    
    /**
     * 构建日志类。可用于替换Log4J的构建
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
    
    
    
    /**
     * 构建日志类
     *
     * @author      ZhengWei(HY)
     * @createDate  2019-06-11
     * @version     v1.0
     *
     * @param i_ClassName
     */
    public Logger(String i_ClassName)
    {
        this(i_ClassName ,null);
    }
    
    
    
    /**
     * 构建日志类
     *
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Class
     */
    public Logger(Class<?> i_Class)
    {
        this(i_Class ,null);
    }
    
    
    
    /**
     * 构建日志类
     *
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Class
     * @param i_IsPrintln  没有任何Log4j版本时，是否采用System.out.println()方法输出
     */
    public Logger(Class<?> i_Class ,Boolean i_IsPrintln)
    {
        this(i_Class.getName() ,i_IsPrintln);
    }
    
    
    
    /**
     * 构建日志类
     *
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_ClassName
     * @param i_IsPrintln  没有任何Log4j版本时，是否采用System.out.println()方法输出
     */
    public Logger(String i_ClassName ,Boolean i_IsPrintln)
    {
        this.requestCount      = new Counter<String>();
        this.requestTime       = new ConcurrentHashMap<String ,Long>();
        this.methodExecSumTime = new Counter<String>();
        this.methodExecLines   = new ConcurrentHashMap<String ,Integer>();
        this.methodExecLastime = new ConcurrentHashMap<String ,Long>();
        this.addLogger();
        
        initLogTypeVersion();
        
        if ( $LogManager != null )
        {
            try
            {
                Method v_Methd = $LogManager.getMethod("getLogger" ,String.class);
                this.log = StaticReflect.invoke(v_Methd ,i_ClassName);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
            
            if ( this.log != null )
            {
                try
                {
                    if ( $LogType == $LogType_SLF4J )
                    {
                        initSLF4JMethod(this.log);
                        initSLF4JLevels();
                        
                        $FatalIsEnabled = null;
                        $ErrorIsEnabled = null;
                        $WarnIsEnabled  = null;
                        $InfoIsEnabled  = null;
                        $DebugIsEnabled = null;
                        $TraceIsEnabled = null;
                    }
                    else if ( $LogType == $LogType_Log4J )
                    {
                        initLog4JMethod(this.log);
                        initLog4JLevels();
                        
                        if ( $LogVersion == 1 )
                        {
                            $FatalIsEnabled = null;
                            $ErrorIsEnabled = null;
                            $WarnIsEnabled  = null;
                            $InfoIsEnabled  = null;
                            $DebugIsEnabled = $LogClass.getMethod("isDebugEnabled");
                            $TraceIsEnabled = null;
                        }
                        else if ( $LogVersion == 2 )
                        {
                            $FatalIsEnabled = $LogClass.getMethod("isFatalEnabled");
                            $ErrorIsEnabled = $LogClass.getMethod("isErrorEnabled");
                            $WarnIsEnabled  = $LogClass.getMethod("isWarnEnabled");
                            $InfoIsEnabled  = $LogClass.getMethod("isInfoEnabled");
                            $DebugIsEnabled = $LogClass.getMethod("isDebugEnabled");
                            $TraceIsEnabled = $LogClass.getMethod("isTraceEnabled");
                        }
                    }
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
        }
        else if ( ($IsEnabled_Print && i_IsPrintln == null) || (i_IsPrintln !=null && i_IsPrintln.booleanValue()) )
        {
            try
            {
                this.logClass = Help.forName(i_ClassName);
                
                if ( $LogMethod != $LogMethodNull )
                {
                    showLoggerInfo(this.logClass);
                    $LogMethod = $LogMethodNull;
                }
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            if ( $LogMethod != $LogMethodNull )
            {
                showLoggerInfo(null);
                $LogMethod = $LogMethodNull;
            }
        }
    }
    
    
    
    /**
     * 显示启用的日志引擎
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-15
     * @version     v1.0
     * 
     * @param i_Log     日志类库的具体的实现类
     */
    public static void showLoggerInfo(Object i_Log)
    {
        FileHelp      v_FileHelp = new FileHelp();
        StringBuilder v_Buffer   = new StringBuilder();
        InputStream   v_LogInput = null;
        
        try
        {
            if ( $LogType == $LogType_SLF4J )
            {
                String v_LoggerName = i_Log.getClass().getName();
                if ( "org.slf4j.helpers.NOPLogger".equalsIgnoreCase(v_LoggerName) )
                {
                    v_LogInput = Logger.class.getResourceAsStream("SFL4J_NoLogger.txt");
                    v_Buffer.append("Loading logger is SLF4J ,but not any implementation (").append(Date.getNowTime().getFullMilli()).append(")\n");
                    v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
                }
                else if ( "ch.qos.logback.classic.Logger".equalsIgnoreCase(v_LoggerName) )
                {
                    v_LogInput = Logger.class.getResourceAsStream("SFL4J_Logback.txt");
                    v_Buffer.append("Loading logger is SLF4J & Logback (").append(Date.getNowTime().getFullMilli()).append(")\n");
                    v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
                }
                else if ( "org.apache.logging.slf4j.Log4jLogger".equalsIgnoreCase(v_LoggerName) )
                {
                    v_LogInput = Logger.class.getResourceAsStream("SFL4J_Log4J.txt");
                    v_Buffer.append("Loading logger is SLF4J & Log4J (").append(Date.getNowTime().getFullMilli()).append(")\n");
                    v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
                }
            }
            else if ( $LogType == $LogType_Log4J )
            {
                v_LogInput = Logger.class.getResourceAsStream("Log4J.txt");
                v_Buffer.append("Loading logger is Log4J " + $LogVersion + ".x (").append(Date.getNowTime().getFullMilli()).append(")\n");
                v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
            }
            else if ( i_Log != null )
            {
                v_Buffer.append("Loading logger is System.out (").append(Date.getNowTime().getFullMilli()).append(")\n\n");
            }
            else
            {
                v_LogInput = Logger.class.getResourceAsStream("NoLogger.txt");
                v_Buffer.append("Loading logger is not any implementation (").append(Date.getNowTime().getFullMilli()).append(")\n");
                v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( v_LogInput != null )
            {
                try
                {
                    v_LogInput.close();
                }
                catch (IOException exce)
                {
                    // Nothing.
                }
                
                v_LogInput = null;
            }
        }
        
        System.out.print(v_Buffer.toString());
    }
    
    
    
    /**
     * 初始化日志的种类及版本信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     */
    private static synchronized void initLogTypeVersion()
    {
        if ( $LogClass != null )
        {
            return;
        }
        
        if ( $IsEnabled_SLF4J )
        {
            try
            {
                // SLF4J
                // v_MarkerClass = Help.forName("org.slf4j.Marker");
                $LogClass     = Help.forName("org.slf4j.Logger");
                $LogManager   = Help.forName("org.slf4j.LoggerFactory");
                $LogType      = $LogType_SLF4J;
                $LogVersion   = 1;
            }
            catch (Exception exce)
            {
                // Nothing.
            }
        }
        
        if ( $IsEnabled_Log4J )
        {
            if ( $LogClass == null )
            {
                try
                {
                    // Log4j 2.x 的版本
                    // v_MarkerClass = Help.forName("org.apache.logging.log4j.Marker");
                    $LogClass     = Help.forName("org.apache.logging.log4j.Logger");
                    $LogManager   = Help.forName("org.apache.logging.log4j.LogManager");
                    $LogType      = $LogType_Log4J;
                    $LogVersion   = 2;
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            
            if ( $LogClass == null )
            {
                try
                {
                    // Log4j 1.x 的版本
                    // v_MarkerClass = null;
                    $LogClass     = Help.forName("org.apache.log4j.Logger");
                    $LogManager   = Help.forName("org.apache.log4j.LogManager");
                    $LogType      = $LogType_Log4J;
                    $LogVersion   = 1;
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
        }
    }
    
    
    
    /**
     * 初始化SLF4J的日志输出方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_Log         SLF4J实现类
     */
    private static synchronized void initSLF4JMethod(Object i_Log)
    {
        if ( $LogMethod != null )
        {
            return;
        }
        
        showLoggerInfo(i_Log);
        
        Method [] v_Methods = i_Log.getClass().getMethods();
        
        if ( $LogVersion >= 0 )
        {
            // public void log(Marker marker, String fqcn, int level, String message, Object[] params, Throwable throwable)
            for (Method v_Method : v_Methods)
            {
                if ( "log".equals(v_Method.getName()) )
                {
                    Class<?> [] v_MPamams = v_Method.getParameterTypes();
                    
                    if ( v_MPamams.length != 6 )
                    {
                        continue;
                    }
                    
                    if ( !String.class.equals(v_MPamams[1]) )
                    {
                        continue;
                    }
                    
                    if ( !int.class.equals(v_MPamams[2]) )
                    {
                        continue;
                    }
                    
                    if ( !String.class.equals(v_MPamams[3]) )
                    {
                        continue;
                    }
                    
                    if ( !Object[].class.equals(v_MPamams[4]) )
                    {
                        continue;
                    }
                    
                    if ( Throwable.class.equals(v_MPamams[5]) )
                    {
                        $LogMethod = v_Method;
                        return;
                    }
                }
            }
        }
        
        $LogMethod = $LogMethodNull;
    }
    
    
    
    /**
     * 初始化Log4J的日志输出方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_Log         Log4J实现类
     */
    private static synchronized void initLog4JMethod(Object i_Log)
    {
        if ( $LogMethod != null )
        {
            return;
        }
        
        showLoggerInfo(i_Log);
        
        Method [] v_Methods = i_Log.getClass().getMethods();
        
        if ( $LogVersion == 1 )
        {
            // public void log(String FQCN, Priority level, Object message, Throwable t)
            for (Method v_Method : v_Methods)
            {
                if ( "log".equals(v_Method.getName()) )
                {
                    Class<?> [] v_MPamams = v_Method.getParameterTypes();
                    
                    if ( v_MPamams.length != 4 )
                    {
                        continue;
                    }
                    
                    if ( !Object.class.equals(v_MPamams[2]) )
                    {
                        continue;
                    }
                    
                    if ( Throwable.class.equals(v_MPamams[3]) )
                    {
                        $LogMethod = v_Method;
                        return;
                    }
                }
            }
        }
        else
        {
            // logIfEnabled(String FQCN ,Level level ,Marker marker ,String message ,Object [] argument)
            for (Method v_Method : v_Methods)
            {
                if ( "logIfEnabled".equals(v_Method.getName()) )
                {
                    Class<?> [] v_MPamams = v_Method.getParameterTypes();
                    
                    if ( v_MPamams.length != 5 )
                    {
                        continue;
                    }
                    
                    if ( !String.class.equals(v_MPamams[3]) )
                    {
                        continue;
                    }
                    
                    if ( Object[].class.equals(v_MPamams[4]) )
                    {
                        $LogMethod = v_Method;
                        break;
                    }
                }
            }
            
            // public void logIfEnabled(String fqcn, Level level, Marker marker, String message, Throwable t)
            for (Method v_Method : v_Methods)
            {
                if ( "logIfEnabled".equals(v_Method.getName()) )
                {
                    Class<?> [] v_MPamams = v_Method.getParameterTypes();
                    
                    if ( v_MPamams.length != 5 )
                    {
                        continue;
                    }
                    
                    if ( !String.class.equals(v_MPamams[3]) )
                    {
                        continue;
                    }
                    
                    if ( Throwable.class.equals(v_MPamams[4]) )
                    {
                        $LogMethod_Log4j2Throwable = v_Method;
                        return;
                    }
                }
            }
        }
        
        $LogMethod = $LogMethodNull;
    }
    
    
    
    /**
     * 初始化SLF4J日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_LogVersion  日志类库的版本
     */
    private static synchronized void initSLF4JLevels()
    {
        if ( $LogLevelFatal != null )
        {
            return;
        }
        
        if ( $LogVersion >= 0 )
        {
            $LogLevelFatal = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.ERROR_INT"));
            $LogLevelError = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.ERROR_INT"));  // 有意创建两个对象，方便日结级别名称的识别
            $LogLevelWarn  = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.WARN_INT"));
            $LogLevelInfo  = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.INFO_INT"));
            $LogLevelDebug = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.DEBUG_INT"));
            $LogLevelTrace = new Level(StaticReflect.getStaticValue("org.slf4j.event.EventConstants.TRACE_INT"));
        }
    }
    
    
    
    /**
     * 初始化Log4J日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_LogVersion  日志类库的版本
     */
    private static synchronized void initLog4JLevels()
    {
        if ( $LogLevelFatal != null )
        {
            return;
        }
        
        if ( $LogVersion == 1 )
        {
            $LogLevelFatal = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.FATAL"));
            $LogLevelError = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.ERROR"));
            $LogLevelWarn  = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.WARN"));
            $LogLevelInfo  = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.INFO"));
            $LogLevelDebug = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.DEBUG"));
            $LogLevelTrace = new Level(StaticReflect.getStaticValue("org.apache.log4j.Level.DEBUG"));  // 有意创建两个对象，方便日结级别名称的识别
        }
        else
        {
            $LogLevelFatal = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.FATAL"));
            $LogLevelError = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.ERROR"));
            $LogLevelWarn  = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.WARN"));
            $LogLevelInfo  = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.INFO"));
            $LogLevelDebug = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.DEBUG"));
            $LogLevelTrace = new Level(StaticReflect.getStaticValue("org.apache.logging.log4j.Level.TRACE"));
        }
    }
    
    
    
    /**
     * 获取日志级别的名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_Level    Log4J、SLF4J的日志级别
     * @return
     */
    public static String getLevelName(final Object i_Level)
    {
        if ( $LogLevelFatal == null )
        {
            return "--";
        }
        
        if ( $LogLevelFatal.equals(i_Level) )
        {
            return "fatal";
        }
        else if ( $LogLevelError.equals(i_Level) )
        {
            return "error";
        }
        else if ( $LogLevelWarn.equals(i_Level) )
        {
            return "warn";
        }
        else if ( $LogLevelInfo.equals(i_Level) )
        {
            return "info";
        }
        else if ( $LogLevelDebug.equals(i_Level) )
        {
            return "debug";
        }
        else if ( $LogLevelTrace.equals(i_Level) )
        {
            return "trace";
        }
        else
        {
            return "unknown";
        }
    }
    
    
    
    /**
     * 全部日志处理类的集合。可用于日志分析
     * 
     * Map.key  为分区标示。为使用日志引擎的类名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     * @return
     */
    public static PartitionMap<String ,Logger> getLoggers()
    {
        return $Loggers;
    }
    
    
    
    /**
     * 重置全部日志引擎的统计数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-13
     * @version     v1.0
     *
     */
    public static void resets()
    {
        for (List<Logger> v_ClassForLoggers : $Loggers.values())
        {
            for (Logger v_Logger : v_ClassForLoggers)
            {
                v_Logger.reset();
            }
        }
    }
    
    
    
    /**
     * 重置统计数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-13
     * @version     v1.0
     *
     */
    public void reset()
    {
        for (String v_Key : this.requestCount.keySet())
        {
            this.requestCount.set(v_Key ,0L);
        }
        
        for (String v_Key : this.requestTime.keySet())
        {
            this.requestTime.put(v_Key ,0L);
        }
        
        for (String v_Key : this.methodExecSumTime.keySet())
        {
            this.methodExecSumTime.set(v_Key ,0L);
        }
        
        if ( this.execptionLog != null )
        {
            for (String v_Key : this.execptionLog.keySet())
            {
                this.execptionLog.get(v_Key).clear();
            }
        }
    }
    
    
    
    /**
     * 将自己添加到统一日志集中管理中。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     */
    private void addLogger()
    {
        StackTraceElement v_StackTrace = LogStackTrace.calcLocation($FQCN);
        
        if ( v_StackTrace != null )
        {
            $Loggers.putRow(v_StackTrace.getClassName() ,this);
        }
    }
    
    
    
    /**
     * 日志统计。
     * 
     * 无论是否对接Log4J、SLF4J，均进行日志统计
     * 
     * Key是：方法名称:代码行
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-12
     * @version     v1.0
     *
     */
    private void request(final Level i_Level ,final String i_LevelName ,final String i_Message ,final Throwable i_Throwable)
    {
        StackTraceElement v_StackTrace = LogStackTrace.calcLocation($FQCN);
        
        if ( v_StackTrace != null )
        {
            String v_Key = i_LevelName + ":" + v_StackTrace.getMethodName() + ":" + v_StackTrace.getLineNumber();
            this.requestCount.put(v_Key ,1L);
            this.requestTime .put(v_Key ,Date.getNowTime().getTime());
            
            if ( i_Level == $LogLevelWarn || i_Level == $LogLevelError || i_Level == $LogLevelFatal )
            {
                synchronized ( this )
                {
                    if ( this.execptionLog == null )
                    {
                        this.execptionLog = new TablePartitionBusway<String ,LogException>();
                    }
                }
                
                this.execptionLog.putRow(v_Key ,new LogException(i_Message ,i_Throwable));
            }
            
            // 下面代码的功能是：通过方法内两次及以上的多次日志输出，尝试计算出方法执行用时
            String  v_MethodThreadID = v_StackTrace.getMethodName() + Thread.currentThread().getId();
            Integer v_LastLine       = this.methodExecLines.get(v_MethodThreadID);
            long    v_NowTime        = Date.getNowTime().getTime();
            
            if ( v_LastLine != null && v_StackTrace.getLineNumber() > v_LastLine )
            {
                Long v_LastTime = this.methodExecLastime.get(v_MethodThreadID);
                
                // 防止系统时间出现紊乱、回退等问题
                if ( v_LastTime != null && v_LastTime.longValue() <= v_NowTime )
                {
                    this.methodExecSumTime.put(v_StackTrace.getMethodName() ,v_NowTime - v_LastTime.longValue());
                }
            }
            
            this.methodExecLines  .put(v_MethodThreadID ,v_StackTrace.getLineNumber());
            this.methodExecLastime.put(v_MethodThreadID ,v_NowTime);
        }
    }
    
    
    
    /**
     * 统计日志执行次数
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：日志级别:方法名称:代码行
     *   Map.Value 是：累计执行次数
     */
    public Counter<String> getRequestCount()
    {
        return requestCount;
    }
    
    
    
    /**
     * 记录异常日志的具体内容
     * 
     * Map.Key   是：日志级别:方法名称:代码行
     * Map.Value 是：异常对象
     */
    public TablePartitionBusway<String ,LogException> getExecptionLog()
    {
        return this.execptionLog;
    }
    
    
    
    /**
     * 统计日志执行次数
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：日志级别:方法名称:代码行
     *   Map.Value 是：累计执行次数
     */
    public Map<String ,Long> getRequestTime()
    {
        return requestTime;
    }
    
    
    
    /**
     * 统计方法执行累计用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称 + 线程号
     *   Map.Value 是：累计用时
     */
    public Counter<String> getMethodExecSumTimes()
    {
        return methodExecSumTime;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isFatalEnabled()
    {
        if ( $FatalIsEnabled != null )
        {
            try
            {
                return (boolean)$FatalIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isErrorEnabled()
    {
        if ( $ErrorIsEnabled != null )
        {
            try
            {
                return (boolean)$ErrorIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isWarnEnabled()
    {
        if ( $WarnIsEnabled != null )
        {
            try
            {
                return (boolean)$WarnIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isInfoEnabled()
    {
        if ( $InfoIsEnabled != null )
        {
            try
            {
                return (boolean)$InfoIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isDebugEnabled()
    {
        if ( $DebugIsEnabled != null )
        {
            try
            {
                return (boolean)$DebugIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * Log4j出于性能考虑，公开了判定日志级别的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @return
     */
    public boolean isTraceEnabled()
    {
        if ( $TraceIsEnabled != null )
        {
            try
            {
                return (boolean)$TraceIsEnabled.invoke(this.log);
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        
        return true;
    }
    
    
    
    /**
     * 输出日志的统一方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     * @param i_Marker     标记。请按Log4J、SLF4J的Marker类型传参
     * @param i_Level      日志级别。请按Log4J、SLF4J的日志级别传参
     * @param i_Message    日志信息
     * @param i_Arguments  额外日志参数
     * @param i_Throwable  异常对象
     */
    public void log(final Object i_Marker ,final Level i_Level ,final String i_Message ,final Object [] i_Arguments ,final Throwable i_Throwable)
    {
        this.request(i_Level ,getLevelName(i_Level) ,i_Message, i_Throwable);
        
        if ( this.log != null && $LogMethod != $LogMethodNull )
        {
            try
            {
                if ( i_Marker == null || i_Marker != null && i_Marker.getClass().getName().endsWith("Marker") )
                {
                    if ( $LogType == $LogType_SLF4J )
                    {
                        $LogMethod.invoke(this.log ,i_Marker ,$FQCN ,i_Level.getLevel() ,i_Message ,i_Arguments ,i_Throwable);
                    }
                    else if ( $LogType == $LogType_Log4J )
                    {
                        if ( $LogVersion == 1 )
                        {
                            $LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Message + StringHelp.toString(i_Arguments) ,i_Throwable);
                        }
                        else
                        {
                            if ( i_Throwable == null )
                            {
                                $LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Marker ,i_Message ,i_Arguments);
                            }
                            else
                            {
                                $LogMethod_Log4j2Throwable.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Marker ,i_Message + StringHelp.toString(i_Arguments) ,i_Throwable);
                            }
                        }
                    }
                }
                else
                {
                    String    v_Message   = i_Marker.toString();
                    Object [] v_Arguments = new Object[i_Arguments.length + 1];
                    
                    v_Arguments[0] = i_Message;
                    Help.fillArray(i_Arguments ,v_Arguments ,1);
                    
                    if ( $LogType == $LogType_SLF4J )
                    {
                        $LogMethod.invoke(this.log ,null ,$FQCN ,i_Level.getLevel() ,v_Message ,v_Arguments ,i_Throwable);
                    }
                    else if ( $LogType == $LogType_Log4J )
                    {
                        if ( $LogVersion == 1 )
                        {
                            $LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,v_Message + StringHelp.toString(v_Arguments) ,i_Throwable);
                        }
                        else
                        {
                            if ( i_Throwable == null )
                            {
                                $LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,null ,v_Message ,v_Arguments);
                            }
                            else
                            {
                                $LogMethod_Log4j2Throwable.invoke(this.log ,$FQCN ,i_Level.getLevel() ,null ,v_Message + StringHelp.toString(v_Arguments) ,i_Throwable);
                            }
                        }
                    }
                }
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else if ( logClass != null )
        {
            String v_ThreadName = Thread.currentThread().getName();
            if ( i_Marker == null )
            {
                this.println(getLevelName(i_Level) + "> " + v_ThreadName + "> " + i_Message + StringHelp.toString(i_Arguments));
            }
            else
            {
                this.println(getLevelName(i_Level) + "> " + v_ThreadName + "> " + i_Marker.toString() + i_Message + StringHelp.toString(i_Arguments));
            }
            
            if ( i_Throwable != null )
            {
                i_Throwable.printStackTrace();
            }
        }
        else if ( i_Throwable != null )
        {
            i_Throwable.printStackTrace();
        }
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void fatal(final String i_Message)
    {
        this.log(null ,$LogLevelFatal ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void fatal(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelFatal ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     */
    public void fatal(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelFatal ,i_Message.toString() ,null ,null);
        }
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void fatal(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelFatal ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void fatal(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelFatal ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void fatal(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelFatal ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void fatal(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelFatal ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void error(final String i_Message)
    {
        this.log(null ,$LogLevelError ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void error(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelError ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-06-09
     * @version     v1.0
     *
     * @param i_Message
     */
    public void error(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelError ,i_Message.toString() ,null ,null);
        }
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void error(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelError ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void error(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelError ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void error(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelError ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void error(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelError ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void warn(final String i_Message)
    {
        this.log(null ,$LogLevelWarn ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void warn(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelWarn ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void warn(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelWarn ,i_Message.toString() ,null ,null);
        }
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void warn(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelWarn ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void warn(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelWarn ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void warn(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelWarn ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void warn(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelWarn ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void info(final String i_Message)
    {
        this.log(null ,$LogLevelInfo ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void info(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelInfo ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     */
    public void info(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelInfo ,i_Message.toString() ,null ,null);
        }

    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void info(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelInfo ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void info(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelInfo ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void info(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelInfo ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void info(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelInfo ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void debug(final String i_Message)
    {
        this.log(null ,$LogLevelDebug ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void debug(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelDebug ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     */
    public void debug(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelDebug ,i_Message.toString() ,null ,null);
        }
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void debug(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelDebug ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void debug(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelDebug ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void debug(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelDebug ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void debug(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelDebug ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-05-27
     * @version     v1.0
     *
     * @param i_Message
     */
    public void trace(final String i_Message)
    {
        this.log(null ,$LogLevelTrace ,i_Message ,null ,null);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Argument
     */
    public void trace(final String i_Message ,final String i_Argument)
    {
        this.log(null ,$LogLevelTrace ,i_Message ,new Object[] {i_Argument} ,null);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     */
    public void trace(final Object i_Message)
    {
        if ( i_Message != null )
        {
            this.log(null ,$LogLevelTrace ,i_Message.toString() ,null ,null);
        }
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Message
     * @param i_Throwable
     */
    public void trace(final String i_Message, final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelTrace ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     * @param i_Throwable
     */
    public void trace(final Throwable i_Throwable)
    {
        this.log(null ,$LogLevelTrace ,"" ,null ,i_Throwable);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Arguments
     */
    public void trace(final Object i_Marker ,String i_Message, final Object ... i_Arguments)
    {
        this.log(i_Marker ,$LogLevelTrace ,i_Message ,i_Arguments ,null);
    }
    
    
    
    /**
     * 最低的日志级别
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-10
     * @version     v1.0
     *
     * @param i_Marker
     * @param i_Message
     * @param i_Throwable
     */
    public void trace(final Object i_Marker ,String i_Message, final Throwable i_Throwable)
    {
        this.log(i_Marker ,$LogLevelTrace ,i_Message ,null ,i_Throwable);
    }
    
    
    
    /**
     * 采用System.out.println()方法输出
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-01-06
     * @version     v1.0
     *
     * @param i_Message
     */
    public void println(final String i_Message)
    {
        if ( logClass != null )
        {
            System.out.println("-- " + Date.getNowTime().getFullMilli() + " [" + this.logClass.getName() + "] " + i_Message);
        }
        else
        {
            System.out.println("-- " + Date.getNowTime().getFullMilli() + " " + i_Message);
        }
    }
    
    
    
    /**
     * 获取：日志实现类库的类型（1：SLF4J  2：Log4J）
     */
    public int getLogType()
    {
        return $LogType;
    }


    
    /**
     * 获取：日志实现类库的版本
     */
    public int getLogVersion()
    {
        return $LogVersion;
    }
    
}
