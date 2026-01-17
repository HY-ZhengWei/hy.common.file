package org.hy.common.xml.log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hy.common.Counter;
import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.Max;
import org.hy.common.PartitionMap;
import org.hy.common.StringHelp;
import org.hy.common.TablePartition;
import org.hy.common.TablePartitionBusway;





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
 *              v8.0  2025-09-30  修正：有日志类无日志配置时是否采用System.out
 *              v9.0  2025-10-10  移除：日志创建初始独立出来，防止并发锁的问题
 *              v10.0 2026-01-07  添加：最大用时
 *              v11.0 2026-01-17  添加：独立计算平均用时，并有要保存它
 *                                添加：独立统计方法的执行次数，并有要保存它
 *                                添加：离散度的计算（建议人：程志华）
 */
public class Logger
{
    
    /** 常量：日志引擎的类型为：SLF4J */
    public static final int                            $LogType_SLF4J = 1;
    
    /** 常量：日志引擎的类型为：Log4J */
    public static final int                            $LogType_Log4J = 2;
    
    private static final String                        $FQCN = Logger.class.getName();
    
    /**
     * 全部日志处理类的集合。可用于日志分析
     * 
     * Map.key  为分区标示。为使用日志引擎的类名称
     */
    private static PartitionMap<String ,Logger>        $Loggers = new TablePartition<String ,Logger>();
          
    
                                                       
    /** 获取日志对象 */
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
     * 统计方法的执行次数（包括成功及异常情况）。
     *    与 requestCount 区别是，它以一个方法为统计主体，无任此方法中有多少次 "日志" 均按 1 统计。
     *    而 requestCount 是以 "日志" 输出次数统计的。
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    private Counter<String>                            methodExecCount;
    
    /**
     * 统计方法执行累计用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    private Counter<String>                            methodExecSumTime;
    
    /**
     * 统计方法执行最大用时。
     * 
     *   注：不受方法内多次 "日志" 输出的影响，是首次日志输出到最后日志输出间的总用时的最大用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    private Max<String>                                methodExecMaxTime;
    
    /**  
     * 方法名称粒度的统计（非数据流累加的数据）。
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     */
    private Map<String ,LoggerMethod>                  methodExec;
    
    /**
     * 方法名称 + 线程号粒度的统计（非数据流累加的数据）。
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key  是：方法名称 + 线程号
     * 
     * 仅限内部使用
     */
    private Map<String ,LoggerMethodThread>            methodExecThread;
    
    
    
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
        LoggerSync.$IsEnabled_SLF4J = true;
        LoggerSync.$IsEnabled_Log4J = false;
        LoggerSync.$IsEnabled_Print = false;
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
        LoggerSync.$IsEnabled_SLF4J = true;
        LoggerSync.$IsEnabled_Log4J = false;
        LoggerSync.$IsEnabled_Print = false;
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
        LoggerSync.$IsEnabled_SLF4J = false;
        LoggerSync.$IsEnabled_Log4J = true;
        LoggerSync.$IsEnabled_Print = false;
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
        LoggerSync.$IsEnabled_SLF4J = false;
        LoggerSync.$IsEnabled_Log4J = false;
        LoggerSync.$IsEnabled_Print = true;
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
        return LoggerSync.$LogLevelFatal;
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
        return LoggerSync.$LogLevelError;
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
        return LoggerSync.$LogLevelWarn;
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
        return LoggerSync.$LogLevelInfo;
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
        return LoggerSync.$LogLevelDebug;
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
        return LoggerSync.$LogLevelTrace;
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
        this.requestCount         = new Counter<String>();
        this.requestTime          = new ConcurrentHashMap<String ,Long>();
        this.methodExecCount      = new Counter<String>();
        this.methodExecSumTime    = new Counter<String>();
        this.methodExec           = new ConcurrentHashMap<String ,LoggerMethod>();
        this.methodExecMaxTime    = new Max<String>();
        this.methodExecThread     = new ConcurrentHashMap<String ,LoggerMethodThread>();
        this.addLogger();
        
        LoggerSync.getInstance().init(this ,i_ClassName ,i_IsPrintln);
    }
    
    
    
    /**
     * 获取：获取日志对象
     */
    protected Object getLog()
    {
        return log;
    }


    
    /**
     * 设置：获取日志对象
     * 
     * @param i_Log 获取日志对象
     */
    protected void setLog(Object i_Log)
    {
        this.log = i_Log;
    }


    
    /**
     * 获取：没有任何Log4j版本时，是否采用System.out.println()方法输出
     */
    protected Class<?> getLogClass()
    {
        return logClass;
    }


    
    /**
     * 设置：没有任何Log4j版本时，是否采用System.out.println()方法输出
     * 
     * @param i_LogClass 没有任何Log4j版本时，是否采用System.out.println()方法输出
     */
    protected void setLogClass(Class<?> i_LogClass)
    {
        this.logClass = i_LogClass;
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
        if ( LoggerSync.$LogLevelFatal == null )
        {
            return "--";
        }
        
        if ( LoggerSync.$LogLevelFatal.equals(i_Level) )
        {
            return "fatal";
        }
        else if ( LoggerSync.$LogLevelError.equals(i_Level) )
        {
            return "error";
        }
        else if ( LoggerSync.$LogLevelWarn.equals(i_Level) )
        {
            return "warn";
        }
        else if ( LoggerSync.$LogLevelInfo.equals(i_Level) )
        {
            return "info";
        }
        else if ( LoggerSync.$LogLevelDebug.equals(i_Level) )
        {
            return "debug";
        }
        else if ( LoggerSync.$LogLevelTrace.equals(i_Level) )
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
        
        for (String v_Key : this.methodExecCount.keySet())
        {
            this.methodExecCount.set(v_Key ,0L);
        }
        
        for (String v_Key : this.methodExecSumTime.keySet())
        {
            this.methodExecSumTime.set(v_Key ,0L);
        }
        
        for (String v_Key : this.methodExecMaxTime.keySet())
        {
            this.methodExecMaxTime.set(v_Key ,0L);
        }
        
        for (LoggerMethod v_Item : this.methodExec.values())
        {
            v_Item.reset();
        }
        
        for (LoggerMethodThread v_Item : this.methodExecThread.values())
        {
            v_Item.reset();
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
            
            if ( i_Level == LoggerSync.$LogLevelWarn || i_Level == LoggerSync.$LogLevelError || i_Level == LoggerSync.$LogLevelFatal )
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
            String             v_MThreadID   = v_StackTrace.getMethodName() + Thread.currentThread().getId();
            LoggerMethodThread v_MExecThread = this.methodExecThread.get(v_MThreadID);
            long               v_NowTime     = Date.getNowTime().getTime();
            if ( v_MExecThread == null || v_MExecThread.getLineNumber() == null )
            {
                // 新的开始。即方法内首次 "日志" 输出 
                v_MExecThread = new LoggerMethodThread();
                v_MExecThread.setExecStartTime(v_NowTime);
                this.methodExecCount.put(v_StackTrace.getMethodName() ,1L);
                
                LoggerMethod v_MethodExec = this.methodExec.get(v_StackTrace.getMethodName());
                if ( v_MethodExec != null )
                {
                    v_MethodExec.setExecAvgTimeOld(v_MethodExec.getExecAvgTime());
                    v_MethodExec.setDispersionOld( v_MethodExec.getDispersion());
                }
            }
            else if ( v_StackTrace.getLineNumber() > v_MExecThread.getLineNumber() )
            {
                // 二次及以上的 "日志" 输出，才能统计用时时长等指标
                // 计算上次与本次的时间差，所在要统计时间，只有要有两行日志。
                // 首个日志时 v_LastTime 定为空
                // 其后的日志 用当前时间 - v_LastTime 即为用时时长  
                
                // 防止系统时间出现紊乱、回退等问题
                if ( v_MExecThread.getExecLastTime() != null && v_MExecThread.getExecLastTime().longValue() <= v_NowTime )
                {
                    long v_TimeLen = v_NowTime - v_MExecThread.getExecLastTime().longValue();
                    this.methodExecSumTime.put(v_StackTrace.getMethodName() ,v_TimeLen);      // 分量用时累加，即每两次 "日志" 输出间的执行用时
                    
                    v_MExecThread.setExecLastTime(v_NowTime);                                 // 必在上行之后执行
                    v_TimeLen = v_MExecThread.getExecTimeLen();                               // 方法级的执行用时。不受方法内多次 "日志" 输出的影响，是首次日志输出到最后日志输出间的总用时的最大用时
                    this.methodExecMaxTime.put(v_StackTrace.getMethodName() ,v_MExecThread.getExecTimeLen());
                    
                    // 计算平均用时、及它的方差--离散度
                    long         v_ExecSumTime  = this.methodExecSumTime.get(v_StackTrace.getMethodName());
                    long         v_ExecSumCount = this.methodExecCount  .get(v_StackTrace.getMethodName());
                    LoggerMethod v_MethodExec   = this.methodExec       .get(v_StackTrace.getMethodName());
                    if ( v_MethodExec == null )
                    {
                        v_MethodExec = new LoggerMethod();
                        this.methodExec.put(v_StackTrace.getMethodName() ,v_MethodExec);
                    }
                    
                    v_MethodExec.setExecAvgTime(v_ExecSumTime / v_ExecSumCount * 1.0D);
                    if ( v_ExecSumCount >= 2 )
                    {
                        // 离散度n = (离散度n-1 + (Xn - AVGn-1) * (Xn - AVGn)) / (n - 1)
                        double v_Dispersion = Help.round((v_MethodExec.getDispersionOld() + (v_TimeLen - v_MethodExec.getExecAvgTimeOld()) * (v_TimeLen - v_MethodExec.getExecAvgTime())) / (v_ExecSumCount - 1.0) ,2);
                        v_MethodExec.setDispersion(v_Dispersion);
                    }
                }
            }
            else
            {
                // 新的开始。即方法内首次 "日志" 输出 
                // 线程号被复用时
                v_MExecThread.setExecStartTime(v_NowTime);
                this.methodExecCount.put(v_StackTrace.getMethodName() ,1L);
                
                LoggerMethod v_MethodExec = this.methodExec.get(v_StackTrace.getMethodName());
                if ( v_MethodExec != null )
                {
                    v_MethodExec.setExecAvgTimeOld(v_MethodExec.getExecAvgTime());
                    v_MethodExec.setDispersionOld( v_MethodExec.getDispersion());
                }
            }
            
            v_MExecThread.setLineNumber(v_StackTrace.getLineNumber());
            v_MExecThread.setExecLastTime(v_NowTime);
            this.methodExecThread.put(v_MThreadID ,v_MExecThread);
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
     * 统计方法的执行次数（包括成功及异常情况）。
     *    与 requestCount 区别是，它以一个方法为统计主体，无任此方法中有多少次 "日志" 均按 1 统计。
     *    而 requestCount 是以 "日志" 输出次数统计的。
     *    
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    public Counter<String> getMethodExecCount()
    {
        return methodExecCount;
    }
    
    
    
    /**
     * 统计方法执行累计用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    public Counter<String> getMethodExecSumTimes()
    {
        return methodExecSumTime;
    }
    
    
    
    /**
     * 获取：方法级的统计粒度下的多项指标。
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     */
    public Map<String ,LoggerMethod> getMethodExecDatas()
    {
        return this.methodExec;
    }


    
    /**
     * 统计方法执行最大用时（包括成功及异常情况）。
     * 
     *   注：不受方法内多次 "日志" 输出的影响，是首次日志输出到最后日志输出间的总用时的最大用时
     * 
     * 无论是否对接Log4J、SLF4J、Logback，均进行日志统计。
     * 
     *   Map.Key   是：方法名称
     *   Map.Value 是：累计用时
     */
    public Max<String> getMethodExecMaxTimes()
    {
        return methodExecMaxTime;
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
        if ( LoggerSync.$FatalIsEnabled != null )
        {
            try
            {
                return (boolean)LoggerSync.$FatalIsEnabled.invoke(this.log);
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
        if ( LoggerSync.$ErrorIsEnabled != null )
        {
            try
            {
                return (boolean)LoggerSync.$ErrorIsEnabled.invoke(this.log);
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
        if ( LoggerSync.$WarnIsEnabled != null )
        {
            try
            {
                return (boolean) LoggerSync.$WarnIsEnabled.invoke(this.log);
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
        if ( LoggerSync.$InfoIsEnabled != null )
        {
            try
            {
                return (boolean) LoggerSync.$InfoIsEnabled.invoke(this.log);
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
        if ( LoggerSync.$DebugIsEnabled != null )
        {
            try
            {
                return (boolean) LoggerSync.$DebugIsEnabled.invoke(this.log);
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
        if ( LoggerSync.$TraceIsEnabled != null )
        {
            try
            {
                return (boolean) LoggerSync.$TraceIsEnabled.invoke(this.log);
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
        
        if ( this.log != null && LoggerSync.$LogMethod != LoggerSync.$LogMethodNull )
        {
            try
            {
                if ( i_Marker == null || i_Marker != null && i_Marker.getClass().getName().endsWith("Marker") )
                {
                    if ( LoggerSync.$LogType == $LogType_SLF4J )
                    {
                        LoggerSync.$LogMethod.invoke(this.log ,i_Marker ,$FQCN ,i_Level.getLevel() ,i_Message ,i_Arguments ,i_Throwable);
                    }
                    else if ( LoggerSync.$LogType == $LogType_Log4J )
                    {
                        if ( LoggerSync.$LogVersion == 1 )
                        {
                            LoggerSync.$LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Message + StringHelp.toString(i_Arguments) ,i_Throwable);
                        }
                        else
                        {
                            if ( i_Throwable == null )
                            {
                                LoggerSync.$LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Marker ,i_Message ,i_Arguments);
                            }
                            else
                            {
                                LoggerSync.$LogMethod_Log4j2Throwable.invoke(this.log ,$FQCN ,i_Level.getLevel() ,i_Marker ,i_Message + StringHelp.toString(i_Arguments) ,i_Throwable);
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
                    
                    if ( LoggerSync.$LogType == $LogType_SLF4J )
                    {
                        LoggerSync.$LogMethod.invoke(this.log ,null ,$FQCN ,i_Level.getLevel() ,v_Message ,v_Arguments ,i_Throwable);
                    }
                    else if ( LoggerSync.$LogType == $LogType_Log4J )
                    {
                        if ( LoggerSync.$LogVersion == 1 )
                        {
                            LoggerSync.$LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,v_Message + StringHelp.toString(v_Arguments) ,i_Throwable);
                        }
                        else
                        {
                            if ( i_Throwable == null )
                            {
                                LoggerSync.$LogMethod.invoke(this.log ,$FQCN ,i_Level.getLevel() ,null ,v_Message ,v_Arguments);
                            }
                            else
                            {
                                LoggerSync.$LogMethod_Log4j2Throwable.invoke(this.log ,$FQCN ,i_Level.getLevel() ,null ,v_Message + StringHelp.toString(v_Arguments) ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelFatal ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelFatal ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelFatal ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelFatal ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelFatal ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelFatal ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelFatal ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelError ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelError ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelError ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelError ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelError ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelError ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelError ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelWarn ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelWarn ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelWarn ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelWarn ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelWarn ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelWarn ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelWarn ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelInfo ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelInfo ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelInfo ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelInfo ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelInfo ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelInfo ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelInfo ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelDebug ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelDebug ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelDebug ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelDebug ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelDebug ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelDebug ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelDebug ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelTrace ,i_Message ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelTrace ,i_Message ,new Object[] {i_Argument} ,null);
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
            this.log(null ,LoggerSync.$LogLevelTrace ,i_Message.toString() ,null ,null);
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
        this.log(null ,LoggerSync.$LogLevelTrace ,i_Message ,null ,i_Throwable);
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
        this.log(null ,LoggerSync.$LogLevelTrace ,"" ,null ,i_Throwable);
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
        this.log(i_Marker ,LoggerSync.$LogLevelTrace ,i_Message ,i_Arguments ,null);
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
        this.log(i_Marker ,LoggerSync.$LogLevelTrace ,i_Message ,null ,i_Throwable);
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
        return LoggerSync.$LogType;
    }


    
    /**
     * 获取：日志实现类库的版本
     */
    public int getLogVersion()
    {
        return LoggerSync.$LogVersion;
    }
    
}
