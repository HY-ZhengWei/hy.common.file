package org.hy.common.xml.log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.StaticReflect;
import org.hy.common.file.FileHelp;





/**
 * 日志创建初始独立出来，防止并发锁的问题
 *
 * @author      ZhengWei(HY)
 * @createDate  2025-10-10
 * @version     v1.0
 */
public class LoggerSync
{
    
    private static volatile LoggerSync                 $LoggerSync = null;
    
    
    
    
    /** 全局控制参数：是否启用SLF4J。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    protected static boolean                           $IsEnabled_SLF4J = true;
    
    /** 全局控制参数：是否启用Log4J。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    protected static boolean                           $IsEnabled_Log4J = true;
    
    /** 全局控制参数：是否启用System.out.println输出日志。目标对象实例化前的有效，日志对象实例化后，修改是没有任何效果的 */
    protected static boolean                           $IsEnabled_Print = false;

    
    
    private static Class<?>                            $LogClass;
    
    private static Class<?>                            $LogManager;
    
    
    
    /** 日志实现类库的类型（1：SLF4J  2：Log4J） */
    protected static int                               $LogType    = -1;
                                                       
    /** 日志实现类库的版本 */
    protected static int                               $LogVersion = -1;
                                                       
    /** 指出每个严重的错误事件将会导致应用程序的退出。这个级别比较高了。重大错误，这种级别你可以直接停止程序了 */
    protected static Level                             $LogLevelFatal;
                          
    /** 指出虽然发生错误事件，但仍然不影响系统的继续运行。打印错误和异常信息，如果不想输出太多的日志，可以使用这个级别 */
    protected static Level                             $LogLevelError;
                          
    /** 表明会出现潜在错误的情形，有些信息不是错误信息，但是也要给程序员的一些提示。 */
    protected static Level                             $LogLevelWarn;
                          
    /** 消息在粗粒度级别上突出强调应用程序的运行过程。打印一些你感兴趣的或者重要的信息 */
    protected static Level                             $LogLevelInfo;
                          
    /** 指出细粒度信息事件对调试应用程序是非常有帮助的，主要用于开发过程中打印一些运行信息 */
    protected static Level                             $LogLevelDebug;
                                                       
    /** 最低的日志级别 */
    protected static Level                             $LogLevelTrace;
                                                       
    protected static Method                            $FatalIsEnabled;
                                                       
    protected static Method                            $ErrorIsEnabled;
                                                       
    protected static Method                            $WarnIsEnabled;
                                                       
    protected static Method                            $InfoIsEnabled;
                                                       
    protected static Method                            $DebugIsEnabled;
                                                       
    protected static Method                            $TraceIsEnabled;
                                                       
    protected static Method                            $LogMethodNull;
                                                       
    protected static Method                            $LogMethod;
                                                       
    protected static Method                            $LogMethod_Log4j2Throwable;
    
    
    
    private LoggerSync()
    {
    }
    
    
    
    public static LoggerSync getInstance()
    {
        if ( $LoggerSync == null )
        {
            synchronized ( LoggerSync.class )
            {
                if ( $LoggerSync == null )
                {
                    $LoggerSync = new LoggerSync();
                }
            }
        }
        return $LoggerSync;
    }
    
    
    
    /**
     * 控制并发而独立出来
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-10-10
     * @version     v1.0
     *
     * @param i_Logger      日志对象
     * @param i_ClassName   日志归属类
     * @param i_IsPrintln   没有任何Log4j版本时，是否采用System.out.println()方法输出
     */
    public synchronized void init(Logger i_Logger ,String i_ClassName ,Boolean i_IsPrintln)
    {
        initLogTypeVersion();
        
        if ( $LogManager != null )
        {
            try
            {
                Method v_Methd = $LogManager.getMethod("getLogger" ,String.class);
                i_Logger.setLog(StaticReflect.invoke(v_Methd ,i_ClassName));
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
            
            if ( i_Logger.getLog() != null )
            {
                boolean v_HaveLogger = false;
                try
                {
                    if ( $LogType == Logger.$LogType_SLF4J )
                    {
                        v_HaveLogger = initSLF4JMethod(i_Logger.getLog());
                        initSLF4JLevels();
                        
                        $FatalIsEnabled = null;
                        $ErrorIsEnabled = null;
                        $WarnIsEnabled  = null;
                        $InfoIsEnabled  = null;
                        $DebugIsEnabled = null;
                        $TraceIsEnabled = null;
                    }
                    else if ( $LogType == Logger.$LogType_Log4J )
                    {
                        v_HaveLogger = initLog4JMethod(i_Logger.getLog());
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
                
                if ( !v_HaveLogger && (($IsEnabled_Print && i_IsPrintln == null) || (i_IsPrintln !=null && i_IsPrintln.booleanValue())) )
                {
                    try
                    {
                        i_Logger.setLogClass(Help.forName(i_ClassName));
                        
                        if ( $LogMethod != getLogMethodNull() )
                        {
                            $LogMethod = getLogMethodNull();
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        else if ( ($IsEnabled_Print && i_IsPrintln == null) || (i_IsPrintln !=null && i_IsPrintln.booleanValue()) )
        {
            try
            {
                i_Logger.setLogClass(Help.forName(i_ClassName));
                
                if ( $LogMethod != getLogMethodNull() )
                {
                    showLoggerInfo(i_Logger.getLogClass());
                    $LogMethod = getLogMethodNull();
                }
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            if ( $LogMethod != getLogMethodNull() )
            {
                showLoggerInfo(null);
                $LogMethod = getLogMethodNull();
            }
        }
    }
    
    
    
    /**
     * 获取没有第三方日志库时的方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-10-10
     * @version     v1.0
     *
     * @return
     */
    private synchronized Method getLogMethodNull()
    {
        if ( $LogMethodNull == null )
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
        return $LogMethodNull;
    }
    
    
    
    /**
     * 初始化SLF4J的日志输出方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *              v2.0  2025-09-30  添加：返回值，是否有日志对象及日志配置
     *
     * @param i_Log  SLF4J实现类
     * @return       是否有日志对象及日志配置
     */
    private synchronized boolean initSLF4JMethod(Object i_Log)
    {
        if ( $LogMethod != null )
        {
            return true;
        }
        
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
                        return showLoggerInfo(i_Log);
                    }
                }
            }
        }
        
        $LogMethod = getLogMethodNull();
        return showLoggerInfo(i_Log);
    }
    
    
    
    /**
     * 初始化Log4J的日志输出方法
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *              v2.0  2025-09-30  添加：返回值，是否有日志对象及日志配置
     *
     * @param i_Log   Log4J实现类
     * @return        是否有日志对象及日志配置
     */
    private synchronized boolean initLog4JMethod(Object i_Log)
    {
        if ( $LogMethod != null )
        {
            return true;
        }
        
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
                        return showLoggerInfo(i_Log);
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
                        return showLoggerInfo(i_Log);
                    }
                }
            }
        }
        
        $LogMethod = getLogMethodNull();
        return showLoggerInfo(i_Log);
    }
    
    
    
    /**
     * 初始化日志的种类及版本信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-11
     * @version     v1.0
     *
     */
    private synchronized void initLogTypeVersion()
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
                $LogType      = Logger.$LogType_SLF4J;
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
                    $LogType      = Logger.$LogType_Log4J;
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
                    $LogType      = Logger.$LogType_Log4J;
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
     * 显示启用的日志引擎
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-15
     * @version     v1.0
     *              v2.0  2025-09-30  添加：返回值，是否有日志对象及日志配置
     * 
     * @param i_Log     日志类库的具体的实现类
     * @return          是否有日志对象及日志配置
     */
    public boolean showLoggerInfo(Object i_Log)
    {
        FileHelp      v_FileHelp = new FileHelp();
        StringBuilder v_Buffer   = new StringBuilder();
        InputStream   v_LogInput = null;
        boolean       v_Ret      = true;
        
        try
        {
            if ( $LogType == Logger.$LogType_SLF4J )
            {
                String v_LoggerName = i_Log.getClass().getName();
                if ( "org.slf4j.helpers.NOPLogger".equalsIgnoreCase(v_LoggerName) )
                {
                    v_LogInput = Logger.class.getResourceAsStream("SFL4J_NoLogger.txt");
                    v_Buffer.append("Loading logger is SLF4J ,but not any implementation (").append(Date.getNowTime().getFullMilli()).append(")\n");
                    v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
                    v_Ret = false;
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
                else
                {
                    v_Ret = false;
                }
            }
            else if ( $LogType == Logger.$LogType_Log4J )
            {
                v_LogInput = Logger.class.getResourceAsStream("Log4J.txt");
                v_Buffer.append("Loading logger is Log4J " + $LogVersion + ".x (").append(Date.getNowTime().getFullMilli()).append(")\n");
                v_Buffer.append(v_FileHelp.getContent(v_LogInput ,"UTF-8" ,true));
            }
            else if ( i_Log != null )
            {
                v_Buffer.append("Loading logger is System.out (").append(Date.getNowTime().getFullMilli()).append(")\n\n");
                v_Ret = false;
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
            v_Ret = false;
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
        return v_Ret;
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
    private synchronized void initSLF4JLevels()
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
    private synchronized void initLog4JLevels()
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
    
}
