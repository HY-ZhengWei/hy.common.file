package org.hy.common.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.hy.common.Help;
import org.hy.common.MethodReflect;
import org.hy.common.Return;
import org.hy.common.StringHelp;
import org.hy.common.XJavaID;
import org.hy.common.xml.log.Logger;





/**
 * Http访问
 *
 * 1. 可按对象参数发起请求
 * 2. 可按集合参数发起请求
 * 3. 可直接按完整的URL发起请求
 * 4. 支持代理访问
 * 5. 支持HTTPS安全访问
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-08-06
 *           V2.0  2016-06-22  添加1：haveQuestionMark 参数。是否自动填充问号(?)。
 *                             添加2：当配置文件中定义的请求参数，外界没有在请求时传入值时，
 *                                   同时配置文件中也没有定义默认时，此请求参数将不再拼接到生成的URL中。
 *           V3.0  2017-02-21  修改1：将属性responseInfo(响应信息)删除，不再通过属性的方式保存响应信息。
 *                                   而是通过请求方法request(...)返回值直接返回响应信息。
 *                                   这样最大好处是：不用再每次请求时创建一个XHttp的实例。
 *           V3.1  2018-11-15  添加1：新类型的转义方法 isEncode。
 *           V4.0  2020-06-09  添加1：支持序列化接口、XJavaID接口
 *           V5.0  2020-06-24  添加1：通过日志引擎规范输出日志
 *           V6.0  2020-12-22  添加1：请求体中的独立数据与动态请求数据共存的请求处理功能
 *           v7.0  2022-04-01  添加1：连接超时时长 和 读取数据的超时时长（建议人：王力）
 *           v8.0  2023-11-08  添加1：Get请求也允许Body体发数据
 *                             添加2：返回真实完整的请求URL
 *           v9.0  2024-10-10  添加1：整合request()方法的冗余代码
 *                             添加2：支持自定义的请求头的传参
 *           v10.0 2025-04-03  添加1：简约的URL。简约而不简单。即是否为没有？和&的地址
 */
public final class XHttp extends SerializableDef implements XJavaID
{
    private static final long   serialVersionUID = 9198603998217342471L;

    private static final Logger $Logger = new Logger(XHttp.class);
    
    /** 请求类型:get方式 */
    public static  final int    $Request_Type_Get  = 1;
    
    /** 请求类型:post方式 */
    public static  final int    $Request_Type_Post = 2;
    
    /** URL转义时被排除的字符 */
    private static final String $NotInString       = "=&?";
    
    /** 用于Https安全访问 */
    private static SSLContext   $SSLContext;
    
    
    /** XJava对象池中的ID标识 */
    private String                     xjavaID;
    
    /** 请求协议类型(http、https) */
    private String                     protocol;
    
    /** HTTP主机IP地址(此属性也可以是域名，如www.sina.com) */
    private String                     ip;
    
    /** HTTP主机端口(默认为0，表示可以不明确说明访问端口) */
    private int                        port;
    
    /** HTTP请求地址。不含"http://IP:Port/"。不含请求参数  */
    private String                     url;
    
    /** HTTP请求参数 */
    private List<XHttpParam>           httpParams;
    
    /** 请求参数无默认值的数量 */
    @SuppressWarnings("unused")
    private int                        notDefaultParamValueCount;
    
    /** 请求类型。1:get方式(默认值)  2:post方式 */
    private int                        requestType;
    
    /** 请求内容类型(默认:text/html) */
    private String                     contentType;
    
    /** 请求字符集名称(默认:UTF-8) */
    private String                     charset;
    
    /** 是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义。与 isEncode 互斥 */
    private boolean                    isToUnicode;
    
    /** 是否对请求参数转义(默认:false)。采用 URLEncoder.encode() 方式转义。与 isToUnicode 互斥 */
    private boolean                    isEncode;
    
    /**
     * 是否自动填充问号(?) 。默认:true
     * 当生成访问URL时，是否生成如 http://ip:port/xx?yy=zz 中的问号
     */
    private boolean                    haveQuestionMark;
    
    /** 代理 */
    private Proxy                      proxy;
    
    /** 代理服务器的IP */
    private String                     proxyHost;
    
    /** 代理服务器的端口 */
    private int                        proxyPort;
    
    /** Cookie信息 */
    private XHttpCookie                cookie;
    
    /** 注释 */
    private String                     comment;
    
    /** 连接超时（单位：毫秒）。零值：表示永远不超时 */
    private int                        connectTimeout;
    
    /** 读取数据超时时长（单位：毫秒）。零值：表示永远不超时 */
    private int                        readTimeout;
    
    
    
    public XHttp()
    {
        this.protocol                  = "http";
        this.port                      = 0;
        this.requestType               = $Request_Type_Get;
        this.notDefaultParamValueCount = 0;
        this.contentType               = "text/html";
        this.charset                   = "UTF-8";
        this.isToUnicode               = true;
        this.isEncode                  = false;
        this.haveQuestionMark          = true;
        this.proxy                     = null;
        this.cookie                    = new XHttpCookie();
        this.connectTimeout            = 30 * 1000;
        this.readTimeout               = 300 * 1000;
    }
    
    
    
    /**
     * 发起Http请求 -- 无参数的
     * 
     * @return  返回是否请求成功
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     */
    public Return<?> request()
    {
/*
        // 代理服务器的IP
        String v_HttpProxy     = "";
        // 代理服务器的端口
        String v_HttpProxyPort = "";
    
        Properties v_SystemProperties = System.getProperties();
        v_SystemProperties.setProperty("http.proxyHost", v_HttpProxy);
        v_SystemProperties.setProperty("http.proxyPort", v_HttpProxyPort);
*/
        
        return this.request((Object)null);
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当为Get请求时，表示请求URL的动态请求参加
     *                      2. 当为Post请求时，表示请求体中的请求数据
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData)
    {
        return this.request(i_UrlData ,(String)null);
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,Map<String ,?> i_BodyData)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData));
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,Map<String ,?> i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData) ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *              v2.0  2020-12-22  添加：请求体中的独立数据与动态请求数据共存的请求处理功能
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,Object i_BodyData)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData);
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,Object i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *              v2.0  2020-12-22  添加：请求体中的独立数据与动态请求数据共存的请求处理功能
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,String i_BodyData)
    {
        try
        {
            return this.request_core(this.getParamsUrl(i_UrlData) ,i_BodyData ,null);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
    }
    
    
    
    /**
     * 发起Http请求 -- 对象参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Object i_UrlData ,String i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        try
        {
            return this.request_core(this.getParamsUrl(i_UrlData) ,i_BodyData ,i_HeadDatas);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当为Get请求时，表示请求URL的动态请求参数
     *                      2. 当为Post请求时，表示请求体中的请求数据
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData)
    {
        return this.request(i_UrlData ,(String)null);
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-12-22
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当为Get请求时，表示请求URL的动态请求参数
     *                      2. 当为Post请求时，表示请求体中的请求数据
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,Map<String ,?> i_BodyData)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData));
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当为Get请求时，表示请求URL的动态请求参数
     *                      2. 当为Post请求时，表示请求体中的请求数据
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,Map<String ,?> i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData) ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-12-22
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,Object i_BodyData)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData);
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,Object i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *              v2.0  2020-12-22  添加：请求体中的独立数据与动态请求数据共存的请求处理功能
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,String i_BodyData)
    {
        return this.request_core(this.getParamsUrl(i_UrlData) ,i_BodyData ,null);
    }
    
    
    
    /**
     * 发起Http请求 -- 集合参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(Map<String ,?> i_UrlData ,String i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        return this.request_core(this.getParamsUrl(i_UrlData) ,i_BodyData ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当为Get请求时，表示请求URL的动态请求参加
     *                      2. 当为Post请求时，表示请求体中的请求数据
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData)
    {
        return this.request(i_UrlData ,(String)null);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-12-22
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,Map<String ,?> i_BodyData)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData));
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,Map<String ,?> i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        return this.request(i_UrlData ,this.getParamsUrl(i_BodyData) ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-12-22
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,Object i_BodyData)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参加
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,Object i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        String v_BodyData = "";
        
        try
        {
            v_BodyData = this.getParamsUrl(i_BodyData);
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return new Return<Object>().paramStr("").set(false).exception(exce);
        }
        
        return this.request(i_UrlData ,v_BodyData ,i_HeadDatas);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2013-08-06
     * @version     v1.0
     *              v2.0  2020-12-22  添加：请求体中的独立数据与动态请求数据共存的请求处理功能
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,String i_BodyData)
    {
        return this.request_core(i_UrlData ,i_BodyData ,null);
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     * 
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    public Return<?> request(String i_UrlData ,String i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        return this.request_core(i_UrlData ,i_BodyData ,i_HeadDatas);
    }
    
    
    
    /**
     * 设置请求头信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param io_URLConn   连接对象
     * @param i_HeadDatas  请求头参数
     */
    private void setHeadDatas(HttpURLConnection io_URLConn ,Map<String ,String> i_HeadDatas)
    {
        if ( Help.isNull(i_HeadDatas) )
        {
            return;
        }
        else
        {
            for (Map.Entry<String ,String> v_Item : i_HeadDatas.entrySet())
            {
                io_URLConn.setRequestProperty(v_Item.getKey() ,v_Item.getValue());
            }
        }
    }
    
    
    
    /**
     * 发起Http请求 -- 参数字符串
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-10
     * @version     v1.0
     *
     * @param   i_UrlData   请求URL路径中的参数，即普通的请求参数。
     *                      这个字符串已被外界拼接好了。
     *                      1. 当i_BodyData为空时，请求URL的参加不追加 i_UrlData
     *                      2. 当i_BodyData有值时，请求URL的参加追加   i_UrlData为新的请求参数
     * 
     * @param   i_BodyData  请求体中的数据
     *                      1. 当i_BodyData为空时，请求体中的数据使用 i_UrlData
     *                      2. 当i_BodyData有值时，请求体中的数据使用 i_BodyData
     *                      3. 只用于Post请求
     *                      
     * @param   i_HeadDatas 请求头数据。Map.key为请求头参数名称，Map.value为请求参数值
     * 
     * @return  返回是否请求成功。
     *          Return.paramObj  真实完整的请求URL
     *          Return.paramStr  保存响应信息
     *          Return.exception 保存异常信息
     * @return
     */
    private Return<?> request_core(String i_UrlData ,String i_BodyData ,Map<String ,String> i_HeadDatas)
    {
        if ( Help.isNull(this.getIp()) )
        {
            throw new NullPointerException("XHttp ip is null.");
        }
        
        Return<Object>    v_Ret        = new Return<Object>().paramStr("");
        URL               v_URL        = null;
        HttpURLConnection v_URLConn    = null;
        OutputStream      v_URLOut     = null;
        InputStream       v_URLInput   = null;
        BufferedReader    v_Reader     = null;
        StringBuilder     v_RespBuffer = new StringBuilder();
        
        try
        {
            String v_ParamsUrl = i_UrlData.trim();
            
            if ( this.requestType == $Request_Type_Post )
            {
                String v_URLParamStr = this.getUrl();
                
                if ( !Help.isNull(i_BodyData) )
                {
                    if ( this.isToUnicode )
                    {
                        v_ParamsUrl = StringHelp.escape_toUnicode(v_ParamsUrl ,$NotInString);
                    }
                    else if ( this.isEncode )
                    {
                        v_ParamsUrl = StringHelp.encode(v_ParamsUrl ,this.getCharset() ,$NotInString);
                    }
                    
                    if ( this.haveQuestionMark && v_URLParamStr.indexOf('?') < 0 )
                    {
                        v_URLParamStr = v_URLParamStr + "?";
                    }
                    else
                    {
                        if ( !Help.isNull(v_ParamsUrl) && v_ParamsUrl.indexOf("?") > 0 )
                        {
                            v_ParamsUrl = "&" + v_ParamsUrl;
                        }
                        else
                        {
                            // 简约的URL。简约而不简单。即是否为没有？和&的地址
                            // Nothing.
                        }
                    }
                }
                
                if ( this.getPort() == 0 )
                {
                    v_Ret.setParamObj(this.getProtocol() + "://" + this.getIp() + v_URLParamStr + (!Help.isNull(i_BodyData) ? v_ParamsUrl : ""));
                    v_URL = new URL(v_Ret.paramObj.toString());
                }
                else
                {
                    v_Ret.setParamObj(this.getProtocol() + "://" + this.getIp() + ":" + this.getPort() + v_URLParamStr + (!Help.isNull(i_BodyData) ? v_ParamsUrl : ""));
                    v_URL = new URL(this.getProtocol() ,this.getIp() ,this.getPort() ,v_URLParamStr + (!Help.isNull(i_BodyData) ? v_ParamsUrl : ""));
                }
                
                if ( this.proxy != null )
                {
                    v_URLConn = (HttpURLConnection)v_URL.openConnection(this.proxy);
                }
                else
                {
                    v_URLConn = (HttpURLConnection)v_URL.openConnection();
                }
                
                if ( "https".equals(this.getProtocol()) )
                {
                    ((HttpsURLConnection)v_URLConn).setSSLSocketFactory(getSSLContext().getSocketFactory());
                    ((HttpsURLConnection)v_URLConn).setHostnameVerifier(new TrustAnyHostnameVerifier());
                }
                
                v_URLConn.setUseCaches(false);
                v_URLConn.setDoOutput(true);
                v_URLConn.setRequestMethod("POST");
                v_URLConn.setRequestProperty("Content-Type" ,this.getContentType() + "; charset=" + this.getCharset());
                v_URLConn.setRequestProperty("User-Agent"   ,"Mozilla/5.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                v_URLConn.setRequestProperty("Cookie"       ,this.cookie.toString());
                v_URLConn.setConnectTimeout(this.connectTimeout);
                v_URLConn.setReadTimeout(this.readTimeout);
                this.setHeadDatas(v_URLConn ,i_HeadDatas);
                
                v_URLOut = v_URLConn.getOutputStream();
                if ( v_URLOut != null )
                {
                    try
                    {
                        if ( !Help.isNull(i_BodyData) )
                        {
                            v_URLOut.write(i_BodyData.getBytes(this.getCharset()));
                        }
                        else
                        {
                            v_URLOut.write(v_ParamsUrl.getBytes(this.getCharset()));
                        }
                    }
                    catch (Exception exce)
                    {
                        $Logger.error(exce);
                        return v_Ret.set(false).exception(exce);
                    }
                    finally
                    {
                        try
                        {
                            v_URLOut.flush();
                        }
                        catch (Exception exce)
                        {
                            $Logger.error(exce);
                            return v_Ret.set(false).exception(exce);
                        }
                        try
                        {
                            v_URLOut.close();
                        }
                        catch (Exception exce)
                        {
                            $Logger.error(exce);
                            return v_Ret.set(false).exception(exce);
                        }
                    }
                    v_URLOut = null;
                }
            }
            else
            {
                if ( this.isToUnicode )
                {
                    v_ParamsUrl = StringHelp.escape_toUnicode(v_ParamsUrl ,$NotInString);
                }
                else if ( this.isEncode )
                {
                    v_ParamsUrl = StringHelp.encode(v_ParamsUrl ,this.getCharset() ,$NotInString);
                }
                
                String v_URLParamStr = this.getUrl();
                if ( this.haveQuestionMark && v_URLParamStr.indexOf('?') < 0 )
                {
                    v_URLParamStr = v_URLParamStr + "?";
                }
                else
                {
                    if ( !Help.isNull(v_ParamsUrl) && v_ParamsUrl.indexOf("?") > 0 )
                    {
                        v_ParamsUrl = "&" + v_ParamsUrl;
                    }
                    else
                    {
                        // 简约的URL。简约而不简单。即是否为没有？和&的地址
                        // Nothing.
                    }
                }
                
                if ( this.getPort() == 0 )
                {
                    v_Ret.setParamObj(this.getProtocol() + "://" + this.getIp() + v_URLParamStr + v_ParamsUrl);
                    v_URL = new URL(v_Ret.paramObj.toString());
                }
                else
                {
                    v_Ret.setParamObj(this.getProtocol() + "://" + this.getIp() + ":" + this.getPort() + v_URLParamStr + v_ParamsUrl);
                    v_URL = new URL(this.getProtocol() ,this.getIp() ,this.getPort() ,v_URLParamStr + v_ParamsUrl);
                }
                
                if ( this.proxy != null )
                {
                    v_URLConn = (HttpURLConnection)v_URL.openConnection(this.proxy);
                }
                else
                {
                    v_URLConn = (HttpURLConnection)v_URL.openConnection();
                }
                
                if ( "https".equals(this.getProtocol()) )
                {
                    ((HttpsURLConnection)v_URLConn).setSSLSocketFactory(getSSLContext().getSocketFactory());
                    ((HttpsURLConnection)v_URLConn).setHostnameVerifier(new TrustAnyHostnameVerifier());
                }
                
                v_URLConn.setUseCaches(false);
                v_URLConn.setRequestMethod("GET");
                v_URLConn.setRequestProperty("Content-Type" ,this.getContentType() + "; charset=" + this.getCharset());
                v_URLConn.setRequestProperty("User-Agent"   ,"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                v_URLConn.setRequestProperty("Cookie"       ,this.cookie.toString());
                v_URLConn.setConnectTimeout(this.connectTimeout);
                v_URLConn.setReadTimeout(this.readTimeout);
                this.setHeadDatas(v_URLConn ,i_HeadDatas);
                
                if ( !Help.isNull(i_BodyData) )
                {
                    v_URLConn.setDoOutput(true);
                    v_URLOut = v_URLConn.getOutputStream();
                    if ( v_URLOut != null )
                    {
                        try
                        {
                            v_URLOut.write(i_BodyData.getBytes(this.getCharset()));
                        }
                        catch (Exception exce)
                        {
                            $Logger.error(exce);
                            return v_Ret.set(false).exception(exce);
                        }
                        finally
                        {
                            try
                            {
                                v_URLOut.flush();
                            }
                            catch (Exception exce)
                            {
                                $Logger.error(exce);
                                return v_Ret.set(false).exception(exce);
                            }
                            try
                            {
                                v_URLOut.close();
                            }
                            catch (Exception exce)
                            {
                                $Logger.error(exce);
                                return v_Ret.set(false).exception(exce);
                            }
                        }
                        v_URLOut = null;
                    }
                }
                
                // 获取GET方式下，真实的客户端SessionID Add 2015-03-04
                String v_SessionID = "";
                String v_Key       = "";
                for (int v_KeyIndex=1; (v_Key = v_URLConn.getHeaderFieldKey(v_KeyIndex)) != null; v_KeyIndex++)
                {
                    if ( v_Key.equalsIgnoreCase("set-cookie") )
                    {
                        String v_CookieVal = v_URLConn.getHeaderField(v_KeyIndex);
                        v_CookieVal = v_CookieVal.substring(0 ,v_CookieVal.indexOf(';') > -1 ? v_CookieVal.indexOf(';') : v_CookieVal.length() - 1);
                        v_SessionID = v_SessionID + v_CookieVal + ";";
                        
                        String [] v_CookieArr = v_CookieVal.split("=");
                        this.cookie.put(v_CookieArr[0] ,v_CookieArr[1]);
                    }
                }
            }
            
            
            v_URLInput = v_URLConn.getInputStream();
            v_Reader   = new BufferedReader(new InputStreamReader(v_URLInput ,this.getCharset()));
            String v_LineData = "";
            
            while ( (v_LineData = v_Reader.readLine()) != null )
            {
                v_RespBuffer.append(v_LineData);
            }
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return v_Ret.set(false).exception(exce);
        }
        finally
        {
            if ( v_URLOut != null )
            {
                try
                {
                    v_URLOut.flush();
                    v_URLOut.close();
                }
                catch (IOException exce)
                {
                    // Nothing.
                }
                
                v_URLOut = null;
            }
            
            if ( v_Reader != null )
            {
                try
                {
                    v_Reader.close();
                }
                catch (IOException exce)
                {
                    // Nothing.
                }
                
                v_Reader = null;
            }
            
            if ( v_URLInput != null )
            {
                try
                {
                    v_URLInput.close();
                }
                catch (IOException exce)
                {
                    
                }
                
                v_URLInput = null;
            }
            
            v_URLConn = null;
            v_URL     = null;
        }
        
        return v_Ret.paramStr(v_RespBuffer.toString().trim()).set(true);
    }
    
    
    
    /**
     * 发起Http请求 -- 外界自定义URL
     * 
     * 使用Get方式发起请求
     * 
     * @param   i_URL  完整的请求URL。如http://IP:Port/xxx.do?p1=v1&p2=v2
     * @return  返回是否请求结果的字符串
     *          当异常时，返回异常信息
     */
    public static String requestGet(String i_FullURL)
    {
        return requestGet(i_FullURL ,null ,false ,false);
    }
    
    
    
    /**
     * 发起Http请求 -- 外界自定义URL
     * 
     * 使用Get方式发起请求
     * 
     * @param   i_URL          完整的请求URL。如http://IP:Port/xxx.do?p1=v1&p2=v2
     * @param   i_Proxy        代理
     * @return  返回是否请求结果的字符串
     *          当异常时，返回异常信息
     */
    public static String requestGet(String i_FullURL ,Proxy i_Proxy)
    {
        return requestGet(i_FullURL ,i_Proxy ,false ,false);
    }
    
    
    
    /**
     * 发起Http请求 -- 外界自定义URL
     * 
     * 使用Get方式发起请求
     * 
     * @param   i_URL          完整的请求URL。如http://IP:Port/xxx.do?p1=v1&p2=v2
     * @param   i_IsEncode     是否对请求参数转义(默认:false)。采用 URLEncoder.encode() 方式转义
     * @param   i_IsToUnicode  是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义
     * @return  返回是否请求结果的字符串
     *          当异常时，返回异常信息
     */
    public static String requestGet(String i_FullURL ,boolean i_IsEncode ,boolean i_IsToUnicode)
    {
        return requestGet(i_FullURL ,null ,i_IsEncode ,i_IsToUnicode);
    }
    
    
    
    /**
     * 发起Http请求 -- 外界自定义URL
     * 
     * 使用Get方式发起请求
     * 
     * v2.0  2018-11-15  添加：两种转义功能
     * 
     * @param   i_URL          完整的请求URL。如http://IP:Port/xxx.do?p1=v1&p2=v2
     * @param   i_Proxy        代理
     * @param   i_IsEncode     是否对请求参数转义(默认:false)。采用 URLEncoder.encode() 方式转义
     * @param   i_IsToUnicode  是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义
     * @return  返回是否请求结果的字符串
     *          当异常时，返回异常信息
     */
    public static String requestGet(String i_FullURL ,Proxy i_Proxy ,boolean i_IsEncode ,boolean i_IsToUnicode)
    {
        if ( Help.isNull(i_FullURL) )
        {
            throw new NullPointerException("XHttp FullURL is null.");
        }
        
        URL               v_URL        = null;
        HttpURLConnection v_URLConn    = null;
        InputStream       v_URLInput   = null;
        BufferedReader    v_Reader     = null;
        StringBuilder     v_RespBuffer = new StringBuilder();
        
        try
        {
            String [] v_Params = i_FullURL.split("\\?");
            if ( v_Params.length >= 2 )
            {
                String v_ParamStr = i_FullURL.substring(i_FullURL.indexOf('?'));
                if ( i_IsToUnicode )
                {
                    v_URL = new URL(v_Params[0] + StringHelp.escape_toUnicode(v_ParamStr ,$NotInString));
                }
                else if ( i_IsEncode )
                {
                    v_URL = new URL(v_Params[0] + StringHelp.encode(v_ParamStr ,"UTF-8" ,$NotInString));
                }
                else
                {
                    v_URL = new URL(i_FullURL);
                }
            }
            else
            {
                v_URL = new URL(i_FullURL);
            }
            
            if ( i_Proxy != null )
            {
                v_URLConn = (HttpURLConnection)v_URL.openConnection(i_Proxy);
            }
            else
            {
                v_URLConn = (HttpURLConnection)v_URL.openConnection();
            }
            v_URLConn.setUseCaches(false);
            v_URLConn.setRequestMethod("GET");
            v_URLConn.setRequestProperty("Content-Type" ,"text/html; charset=UTF-8");
            v_URLConn.setRequestProperty("User-Agent"   ,"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            
            
            v_URLInput = v_URLConn.getInputStream();
            v_Reader   = new BufferedReader(new InputStreamReader(v_URLInput ,"UTF-8"));
            String v_LineData = "";
            
            while ( (v_LineData = v_Reader.readLine()) != null )
            {
                v_RespBuffer.append(v_LineData);
            }
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            return exce.getMessage();
        }
        finally
        {
            if ( v_Reader != null )
            {
                try
                {
                    v_Reader.close();
                }
                catch (IOException exce)
                {
                    // Nothing.
                }
                
                v_Reader = null;
            }
            
            if ( v_URLInput != null )
            {
                try
                {
                    v_URLInput.close();
                }
                catch (IOException exce)
                {
                    
                }
                
                v_URLInput = null;
            }
            
            v_URLConn = null;
            v_URL     = null;
        }
        

        return v_RespBuffer.toString().trim();
    }
    
    
    
    /**
     * 获取参数拼接成的部分URL字符串。
     * 
     * 格式为：UrlParamName1=ParamValue1&UrlParamName2=ParamValue2...
     * 
     * @param i_ParamObj
     * @return
     * @throws NoSuchMethodException
     */
    private String getParamsUrl(Object i_ParamObj) throws NoSuchMethodException
    {
        if ( Help.isNull(this.httpParams) )
        {
            return "";
        }
        
        StringBuilder v_ParamBuffer = new StringBuilder();
        boolean       v_IsFirst     = true;
        
        for (int i=0; i<this.httpParams.size(); i++)
        {
            XHttpParam v_XHttpParam = this.httpParams.get(i);
            String     v_ParamValue = this.getParamValue(v_XHttpParam ,i_ParamObj);
            
            if ( Help.isNull(v_ParamValue) )
            {
                v_ParamValue = Help.NVL(v_XHttpParam.getParamValue());  // 取配置文件中的默认值
            }
            
            if ( !Help.isNull(v_ParamValue) )
            {
                if ( !v_IsFirst )
                {
                    v_ParamBuffer.append("&");
                }
                
                v_ParamBuffer.append(v_XHttpParam.getUrlParamName()).append("=").append(v_ParamValue);
                v_IsFirst = false;
            }
        }
        
        return v_ParamBuffer.toString();
    }
    
    
    
    /**
     * 获取参数拼接成的部分URL字符串。
     * 
     * 格式为：UrlParamName1=ParamValue1&UrlParamName2=ParamValue2...
     * 
     * @param i_ParamValues
     * @return
     * @throws NoSuchMethodException
     */
    private String getParamsUrl(Map<String ,?> i_ParamValues)
    {
        if ( Help.isNull(this.httpParams) )
        {
            return "";
        }
        
        StringBuilder v_ParamBuffer = new StringBuilder();
        boolean       v_IsFirst     = true;
        
        for (int i=0; i<this.httpParams.size(); i++)
        {
            XHttpParam v_XHttpParam = this.httpParams.get(i);
            String     v_ParamValue = this.getParamValue(v_XHttpParam ,i_ParamValues);
            
            if ( Help.isNull(v_ParamValue) )
            {
                v_ParamValue = Help.NVL(v_XHttpParam.getParamValue());  // 取配置文件中的默认值
            }
            
            if ( !Help.isNull(v_ParamValue) )
            {
                if ( !v_IsFirst )
                {
                    v_ParamBuffer.append("&");
                }
                
                v_ParamBuffer.append(v_XHttpParam.getUrlParamName()).append("=").append(v_ParamValue);
                v_IsFirst = false;
            }
        }
        
        return v_ParamBuffer.toString();
    }
    
    
    
    /**
     * 获取对象参数的值
     * 
     * @param i_XHttpParam
     * @param i_ParamObj
     * @return
     * @throws NoSuchMethodException
     */
    private String getParamValue(XHttpParam i_XHttpParam ,Object i_ParamObj) throws NoSuchMethodException
    {
        if ( i_ParamObj == null )
        {
            return Help.NVL(i_XHttpParam.getParamValue());
        }
        
        String v_MethodName = i_XHttpParam.getParamName();
        
        if ( !v_MethodName.startsWith("get") )
        {
            v_MethodName = "get" + v_MethodName;
        }
        
        List<Method> v_Methods = MethodReflect.getMethodsIgnoreCase(i_ParamObj.getClass() ,v_MethodName ,0);
        
        
        if ( v_Methods.size() == 1 )
        {
            try
            {
                Object v_Value = v_Methods.get(0).invoke(i_ParamObj);
                
                if ( v_Value == null )
                {
                    return Help.NVL(i_XHttpParam.getParamValue());
                }
                else
                {
                    return v_Value.toString();
                }
            }
            catch (Exception exce)
            {
                $Logger.error("XHttp param name [" + i_XHttpParam.getParamName() + "] is not find Object method." ,exce);
                throw new NoSuchMethodException("XHttp param name [" + i_XHttpParam.getParamName() + "] is not find Object method.");
            }
        }
        else if ( v_Methods.size() >= 2 )
        {
            throw new NoSuchMethodException("XHttp param name [" + i_XHttpParam.getParamName() + "] method is not only.");
        }
        else
        {
            return Help.NVL(i_XHttpParam.getParamValue());
        }
    }
    
    
    
    /**
     * 获取对象参数的值
     * 
     * @param i_XHttpParam
     * @param i_ParamObj
     * @return
     * @throws NoSuchMethodException
     */
    private String getParamValue(XHttpParam i_XHttpParam ,Map<String ,?> i_ParamValues)
    {
        if ( Help.isNull(i_ParamValues) )
        {
            return Help.NVL(i_XHttpParam.getParamValue());
        }
        
        
        if ( i_ParamValues.containsKey(i_XHttpParam.getParamName()) )
        {
            Object v_Value = i_ParamValues.get(i_XHttpParam.getParamName());
            
            if ( v_Value == null )
            {
                return Help.NVL(i_XHttpParam.getParamValue());
            }
            else
            {
                return v_Value.toString();
            }
        }
        else
        {
            return Help.NVL(i_XHttpParam.getParamValue());
        }
    }
    
    
    
    /**
     * 添加请求参数
     * 
     * @param i_HttpParam
     */
    public synchronized void setAddParam(XHttpParam i_HttpParam)
    {
        if ( i_HttpParam == null || !i_HttpParam.isValid() )
        {
            throw new NullPointerException("XHttp setAddParam(XHttpParam) parameter is null or not vaild.");
        }
        
        if ( this.httpParams == null )
        {
            this.httpParams = new ArrayList<XHttpParam>();
        }
        
        this.httpParams.add(i_HttpParam);
        
        if ( Help.isNull(i_HttpParam.getParamValue()) )
        {
            this.notDefaultParamValueCount++;
        }
    }
    
    
    
    public void setParams(List<XHttpParam> i_XHttpParams)
    {
        this.httpParams                = i_XHttpParams;
        this.notDefaultParamValueCount = 0;
        
        if ( !Help.isNull(i_XHttpParams) )
            
        for (XHttpParam v_XHttpParam : i_XHttpParams)
        {
            if ( Help.isNull(v_XHttpParam.getParamValue()) )
            {
                this.notDefaultParamValueCount++;
            }
        }
    }
    
    
    
    public List<XHttpParam> getParams()
    {
        return this.httpParams;
    }
    
    
    
    /**
     * 获取 Http 响应信息(只返回 Body 标签内的信息)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-02-21
     * @version     v1.0
     *
     * @param i_ResponseInfo  响应信息
     * @return
     */
    public String getResponseBody(Return<?> i_ResponseInfo)
    {
        if ( i_ResponseInfo != null && !Help.isNull(i_ResponseInfo.paramStr) )
        {
            String v_Ret = StringHelp.getXMLSignContent(i_ResponseInfo.paramStr ,"body");
            
            if ( Help.isNull(v_Ret) )
            {
                return "";
            }
            else
            {
                int v_IndexOf     = v_Ret.indexOf('>');
                int v_LastIndexOf = v_Ret.lastIndexOf('<');
                if ( v_IndexOf >= 0 && v_LastIndexOf > v_IndexOf )
                {
                    return v_Ret.substring(v_IndexOf + 1 ,v_LastIndexOf);
                }
                else
                {
                    return "";
                }
            }
        }
        
        return "";
    }
    
    
    
    /**
     * HTTP主机IP地址(此属性也可以是域名，如www.sina.com)
     * 
     * @return
     */
    public String getIp()
    {
        return ip;
    }


    
    /**
     * HTTP主机IP地址(此属性也可以是域名，如www.sina.com)
     * 
     * @param ip
     */
    public void setIp(String ip)
    {
        this.ip = ip;
    }


    
    /**
     * HTTP主机端口(默认为0，表示可以不明确说明访问端口)
     * 
     * @return
     */
    public int getPort()
    {
        return port;
    }


    
    /**
     * HTTP主机端口(默认为0，表示可以不明确说明访问端口)
     * 
     * @param i_Port
     */
    public void setPort(int i_Port)
    {
        if ( 1 <= i_Port && i_Port <= 65535 )
        {
            this.port = i_Port;
        }
        else
        {
            this.port = 0;
        }
    }

    
    
    /**
     * 请求类型。1:get方式(默认值)  2:post方式
     * 
     * @return
     */
    public int getRequestType()
    {
        return requestType;
    }


    
    /**
     * 请求类型。1:get方式(默认值)  2:post方式
     * 
     * @param i_RequestType
     */
    public void setRequestType(int i_RequestType)
    {
        if ( $Request_Type_Get != i_RequestType && $Request_Type_Post != i_RequestType )
        {
            throw new IllegalArgumentException("Request type is not 1:Get or 2:Post.");
        }
        
        this.requestType = i_RequestType;
    }


    
    public String getUrl()
    {
        return url;
    }


    
    public void setUrl(String i_Url)
    {
        if ( Help.isNull(i_Url) )
        {
            this.url = "/";
        }
        
        if ( i_Url.trim().startsWith("/") )
        {
            this.url = i_Url.trim();
        }
        else
        {
            this.url = "/" + i_Url.trim();
        }
    }
    
    
    
    /**
     * 获取拼接的Http请求
     * 
     * @param i_ParamValues
     * @return
     */
    public String getRequestInfo(Map<String ,?> i_ParamValues)
    {
        StringBuilder v_Buffer = new StringBuilder();
        
        v_Buffer.append(this.getProtocol());
        v_Buffer.append("://");
        v_Buffer.append(this.getIp());
        v_Buffer.append(":");
        v_Buffer.append(this.getPort());
        v_Buffer.append(this.getUrl());
        if ( this.haveQuestionMark && this.getUrl().indexOf('?') < 0 )
        {
            v_Buffer.append("?");
        }
        
        try
        {
            v_Buffer.append(StringHelp.escape_toUnicode(this.getParamsUrl(i_ParamValues) ,$NotInString));
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            v_Buffer.append(exce.getMessage());
        }
        
        return v_Buffer.toString();
    }
    
    
    
    /**
     * 获取拼接的Http请求
     * 
     * @param i_ParamObj
     * @return
     */
    public String getRequestInfo(Object i_ParamObj)
    {
        StringBuilder v_Buffer = new StringBuilder();
        
        v_Buffer.append(this.getProtocol());
        v_Buffer.append("://");
        v_Buffer.append(this.getIp());
        v_Buffer.append(":");
        v_Buffer.append(this.getPort());
        v_Buffer.append(this.getUrl());
        if ( this.haveQuestionMark && this.getUrl().indexOf('?') < 0 )
        {
            v_Buffer.append("?");
        }
        
        try
        {
            v_Buffer.append(StringHelp.escape_toUnicode(this.getParamsUrl(i_ParamObj) ,$NotInString));
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            v_Buffer.append(exce.getMessage());
        }
        
        return v_Buffer.toString();
    }
    
    
    
    @Override
    public String toString()
    {
        StringBuilder v_Buffer = new StringBuilder();
        
        v_Buffer.append(this.getProtocol());
        v_Buffer.append("://");
        v_Buffer.append(this.getIp());
        if ( this.getPort() > 0 )
        {
            v_Buffer.append(":");
            v_Buffer.append(this.getPort());
        }
        v_Buffer.append(this.getUrl());
        if ( this.haveQuestionMark && this.getUrl().indexOf('?') < 0 )
        {
            v_Buffer.append("?");
        }
        
        try
        {
            v_Buffer.append(this.getParamsUrl(null));
        }
        catch (Exception exce)
        {
            $Logger.error(exce);
            v_Buffer.append(exce.getMessage());
        }
        
        return v_Buffer.toString();
    }


    
    /**
     * 获取：Cookie信息
     */
    public XHttpCookie getCookie()
    {
        return cookie;
    }
    
    
    
    /**
     * 设置：Cookie信息
     * 
     * @param cookie
     */
    public void setCookieMap(Map<String ,?> i_CookieMap)
    {
        this.cookie.putAll(i_CookieMap);
    }


    
    /**
     * 设置：Cookie信息
     * 
     * @param cookie
     */
    public void setCookie(XHttpCookie cookie)
    {
        this.cookie = cookie;
    }



    /**
     * 请求字符集名称(默认:UTF-8)
     * 
     * @return
     */
    public String getCharset()
    {
        return charset;
    }


    
    /**
     * 请求字符集名称(默认:UTF-8)
     * 
     * @param charset
     */
    public void setCharset(String charset)
    {
        this.charset = charset;
    }


    
    /**
     * 请求内容类型(默认:text/html)
     * 
     * @return
     */
    public String getContentType()
    {
        return contentType;
    }


    
    /**
     * 请求内容类型(默认:text/html)
     * 
     * @param contentType
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    
    
    
    /**
     * 请求协议类型(http、https)
     * 
     * @return
     */
    public String getProtocol()
    {
        if ( Help.isNull(this.protocol) )
        {
            return "http";
        }
        else
        {
            return this.protocol;
        }
    }
    
    
    
    /**
     * 请求协议类型(http、https)
     * 
     * @param i_Protocol
     */
    public void setProtocol(String i_Protocol)
    {
        this.protocol = Help.NVL(i_Protocol).trim();
    }
    
    
    
    /**
     * 是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义。与 isEncode 互斥
     */
    public boolean isToUnicode()
    {
        return isToUnicode;
    }
    
    
    
    /**
     * 是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义。与 isEncode 互斥
     */
    public boolean getToUnicode()
    {
        return this.isToUnicode;
    }
    
    
    
    /**
     * 是否对请求参数转义(默认:true)。采用 JavaScript escape() 方式转义。与 isEncode 互斥
     */
    public void setToUnicode(boolean isToUnicode)
    {
        this.isToUnicode = isToUnicode;
        
        if ( this.isToUnicode )
        {
            this.isEncode = false;
        }
    }

    
    
    /**
     * 是否对请求参数转义(默认:false)。采用 URLEncoder.encode() 方式转义。与 isToUnicode 互斥
     */
    public void setEncode(boolean isEncode)
    {
        this.isEncode = isEncode;
        
        if ( this.isEncode )
        {
            this.isToUnicode = false;
        }
    }


    
    /**
     * 是否对请求参数转义(默认:false)。采用 URLEncoder.encode() 方式转义。与 isToUnicode 互斥
     */
    public boolean isEncode()
    {
        return isEncode;
    }
    
    
    
    /**
     * 获取：是否自动填充问号(?) 。默认:true
     * 当生成访问URL时，是否生成如 http://ip:port/xx?yy=zz 中的问号
     */
    public boolean isHaveQuestionMark()
    {
        return haveQuestionMark;
    }


    
    /**
     * 设置：是否自动填充问号(?) 。默认:true
     * 当生成访问URL时，是否生成如 http://ip:port/xx?yy=zz 中的问号
     * 
     * @param haveQuestionMark
     */
    public void setHaveQuestionMark(boolean haveQuestionMark)
    {
        this.haveQuestionMark = haveQuestionMark;
    }


    
    /**
     * 代理
     * 
     * @return
     */
    public Proxy getProxy()
    {
        return proxy;
    }


    
    /**
     * 代理
     * 
     * @param i_Proxy
     */
    public void setProxy(Proxy i_Proxy)
    {
        this.proxy = i_Proxy;
    }
    
    
    
    /**
     * 代理服务器的IP
     * 
     * @return
     */
    public String getProxyHost()
    {
        return proxyHost;
    }


    
    /**
     * 代理服务器的IP
     * 
     * @param proxyHost
     */
    public void setProxyHost(String proxyHost)
    {
        this.proxyHost = proxyHost;
        
        if ( !Help.isNull(this.proxyHost) && 0 < this.proxyPort && this.proxyPort < 65535 )
        {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost ,this.proxyPort));
        }
    }


    
    /**
     * 代理服务器的端口
     * 
     * @return
     */
    public int getProxyPort()
    {
        return proxyPort;
    }


    
    /**
     * 代理服务器的端口
     * 
     * @param proxyPort
     */
    public void setProxyPort(int proxyPort)
    {
        this.proxyPort = proxyPort;
        
        if ( !Help.isNull(this.proxyHost) && 0 < this.proxyPort && this.proxyPort < 65535 )
        {
            this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost ,this.proxyPort));
        }
    }
    
    
    
    /**
     * 设置XJava池中对象的ID标识。此方法不用用户调用设置值，是自动的。
     * 
     * @param i_XJavaID
     */
    @Override
    public void setXJavaID(String i_XJavaID)
    {
        this.xjavaID = i_XJavaID;
    }



    /**
     * 获取XJava池中对象的ID标识。
     * 
     * @return
     */
    @Override
    public String getXJavaID()
    {
        return this.xjavaID;
    }

    
    
    /**
     * 获取：注释
     */
    @Override
    public String getComment()
    {
        return comment;
    }

    
    
    /**
     * 设置：注释
     * 
     * @param comment
     */
    @Override
    public void setComment(String comment)
    {
        this.comment = comment;
    }

    

    /**
     * 类似于跳过 "继续浏览此网站(不推荐)" 这样的提醒
     */
    public static class TrustAnyHostnameVerifier implements HostnameVerifier
    {
        @Override
        public boolean verify(String hostname ,SSLSession session)
        {
            return true;
        }
    }
    
    
    
    /**
     * 获取 SSLContext 对象
     * 
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    public synchronized static SSLContext getSSLContext() throws KeyManagementException, NoSuchAlgorithmException
    {
        if ( $SSLContext == null )
        {
            $SSLContext = SSLContext.getInstance("SSL");
            $SSLContext.init(null
                            ,new TrustManager[] {new TrustAnyTrustManager()}
                            ,new java.security.SecureRandom());
        }
        
        return $SSLContext;
    }
    
    
    
    /**
     * 类似于跳过 "继续浏览此网站(不推荐)" 这样的提醒
     */
    public static class TrustAnyTrustManager implements X509TrustManager
    {

        @Override
        public void checkClientTrusted(X509Certificate [] chain ,String authType) throws CertificateException
        {
        }



        @Override
        public void checkServerTrusted(X509Certificate [] chain ,String authType) throws CertificateException
        {
        }



        @Override
        public X509Certificate [] getAcceptedIssuers()
        {
            return new X509Certificate[] {};
        }
    }


    
    /**
     * 连接超时（单位：毫秒）。零值：表示永远不超时
     * 
     * @return
     */
    public int getConnectTimeout()
    {
        return connectTimeout;
    }


    
    /**
     * 设置：连接超时（单位：毫秒）。零值：表示永远不超时
     * 
     * @param connectTimeout
     */
    public void setConnectTimeout(int connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }



    /**
     * 读取数据超时时长（单位：毫秒）。零值：表示永远不超时
     * 
     * @return
     */
    public int getReadTimeout()
    {
        return readTimeout;
    }



    /**
     * 设置：读取数据超时时长（单位：毫秒）。零值：表示永远不超时
     * 
     * @param readTimeout
     */
    public void setReadTimeout(int readTimeout)
    {
        this.readTimeout = readTimeout;
    }
    
}