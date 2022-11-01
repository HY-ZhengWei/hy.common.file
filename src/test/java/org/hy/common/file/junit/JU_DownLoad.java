package org.hy.common.file.junit;

import java.util.Properties;

import org.hy.common.file.FileHelp;





/**
 * 测试类：下载文件(图片)
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-12-17
 */
public class JU_DownLoad
{
    
    public static void main(String [] args)
    {
        // 代理服务器的IP
        String v_HttpProxy     = "127.0.0.1";
        // 代理服务器的端口
        String v_HttpProxyPort = "8080";
    
        Properties v_SystemProperties = System.getProperties();
        v_SystemProperties.setProperty("http.proxyHost", v_HttpProxy);
        v_SystemProperties.setProperty("http.proxyPort", v_HttpProxyPort);
        
        
        FileHelp v_FHelp = new FileHelp();
        
        // v_FHelp.setOverWrite(true);
        // v_FHelp.download("http://www.baidu.com/img/bdlogo.gif" ,"C:\\FileHelp_DownLoad.gif");
        
        v_FHelp.setOverWrite(true);
        v_FHelp.download("https://kyfw.12306.cn/otn/czxx/queryByTrainNo?train_no=410000K31806&from_station_telecode=XAY&to_station_telecode=NNZ&depart_date=2014-01-22" ,"C:\\12306.xml");
    }
    
}
