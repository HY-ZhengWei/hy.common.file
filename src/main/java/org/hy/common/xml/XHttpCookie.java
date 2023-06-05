package org.hy.common.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hy.common.Help;
import org.hy.common.app.Param;





/**
 * Http访问中的Cookie信息 
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-01-28
 * @version     v1.0
 */
public class XHttpCookie extends HashMap<String ,Object>
{

    private static final long serialVersionUID = 3946666166283157908L;
    
    
    
    /**
     * 方便XJava中的xml配置文件使用
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-28
     * @version     v1.0
     *
     * @param i_Param
     */
    public void setAdd(Param i_Param)
    {
        if ( i_Param != null && !Help.isNull(i_Param.getName()) )
        {
            super.put(i_Param.getName() ,i_Param);
        }
    }
    
    
    
    @Override
    public synchronized String toString()
    {
        int v_Size = super.size();
        if ( v_Size <= 0 )
        {
            return "";
        }

        StringBuilder                       v_Buffer   = new StringBuilder();
        Iterator<Map.Entry<String ,Object>> v_Iterator = entrySet().iterator();

        for (int i=1 ; ; i++) 
        {
            Map.Entry<String ,Object> v_Entry = v_Iterator.next();
            
            v_Buffer.append(v_Entry.getKey());
            v_Buffer.append('=');
            
            if ( v_Entry.getValue() != null )
            {
                v_Buffer.append(v_Entry.getValue().toString());
            }
            else
            {
                v_Buffer.append("");
            }

            if ( i < v_Size )
            {
                v_Buffer.append(';');
            }
            else
            {
                return v_Buffer.toString();
            }
        }
    }
    
}
