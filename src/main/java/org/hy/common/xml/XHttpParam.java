package org.hy.common.xml;

import org.hy.common.Help;





/**
 * Http访问的请求参数
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-08-06
 */
public class XHttpParam
{
    /** 在HTTP请求中的参数名称 */
    private String urlParamName;
    
    /** 
     * XJava标记的参数名称
     * 
     * 不主动设置 this.urlParamName 时，默认与 this.paramName 相同。
     */
    private String paramName;
    
    /** 
     * 参数值(非必选)(默认值)
     * 
     * 当此字段有值时，外界还对此字段输入其它值，则以外界为准。
     * 否则，已此值为准。相当于"默认值"的概念
     */
    private String paramValue;
    
    
    
    public XHttpParam()
    {
        
    }
    
    
    
    /**
     * 判断是否有效的
     * 
     * @return
     */
    public boolean isValid()
    {
        if ( Help.isNull(this.urlParamName) || Help.isNull(this.paramName) )
        {
            return false;
        }
        else
        {
            return true;
        }
    }
    
    
    
    public String getParamName()
    {
        return paramName;
    }

    
    
    public void setParamName(String i_ParamName)
    {
        if ( Help.isNull(i_ParamName) )
        {
            throw new NullPointerException("XHttp param name is null.");
        }
        
        this.paramName = i_ParamName.trim();
        
        if ( Help.isNull(this.urlParamName) )
        {
            this.urlParamName = this.paramName;
        }
    }

    
    
    public String getParamValue()
    {
        return paramValue;
    }

    
    
    public void setParamValue(String i_ParamValue)
    {
        this.paramValue = i_ParamValue;
    }


    
    public String getUrlParamName()
    {
        return urlParamName;
    }



    public void setUrlParamName(String urlParamName)
    {
        this.urlParamName = urlParamName;
    }

}
