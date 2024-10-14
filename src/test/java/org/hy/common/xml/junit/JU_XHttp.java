package org.hy.common.xml.junit;

import java.util.HashMap;
import java.util.Map;

import org.hy.common.Date;
import org.hy.common.Return;
import org.hy.common.xml.XHttp;
import org.junit.Test;





/**
 * 测试单元：XHttp
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-10-11
 * @version     v1.0
 */
public class JU_XHttp
{
    
    /**
     * 测试自定义的请求头
     * 
     * @author      ZhengWei(HY)
     * @createDate  2024-10-11
     * @version     v1.0
     *
     */
    @Test
    public void test_HttpHead()
    {
        XHttp  v_XHttp = new XHttp();

        v_XHttp.setProtocol("https");
        v_XHttp.setIp("www.lpslab.cn");
        v_XHttp.setUrl("/msData/operationLog/queryModule");
        v_XHttp.setContentType("application/json");
        v_XHttp.setCharset("UTF-8");
        v_XHttp.setRequestType(XHttp.$Request_Type_Post);

        Map<String ,String> v_HeadDatas = new HashMap<String ,String>();
        v_HeadDatas.put("workID"        ,"123");
        v_HeadDatas.put("serviceDataID" ,"456");
        v_HeadDatas.put("processID"     ,"789");
        
        Return<?> v_Ret = v_XHttp.request("token=1&r=" + Date.getNowTime().getTime() ,"{\"userID\": \"HY\"}" ,v_HeadDatas);

        System.out.println(v_Ret);
        System.out.println(v_Ret.getParamStr());
    }
    
}
