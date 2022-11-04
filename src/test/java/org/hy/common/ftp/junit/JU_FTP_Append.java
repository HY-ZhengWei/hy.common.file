package org.hy.common.ftp.junit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;





/**
 * 追加上传 -- 断点续传
 *
 * @author      ZhengWei(HY)
 * @createDate  2020-05-20
 * @version     v1.0
 */
// @Xjava(value=XType.XML)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JU_FTP_Append
{
    
    private static boolean $isInit = false;
    
    
    
    public JU_FTP_Append() throws Exception
    {
        if ( !$isInit )
        {
            $isInit = true;
            // XJava.parserAnnotation(this.getClass().getName());
        }
    }
    
    
    
    @Test
    public void test_Append()
    {
        /*
        FTPHelp v_Ftp = new FTPHelp((FTPInfo)XJava.getObject("FTPServer01"));
        
        v_Ftp.connect();
        v_Ftp.upload("D:\\WorkSpace_SearchDesktop\\hy.common.ftp\\lib\\hy.common.xjava.jar" ,"/attendence/2020-05-28/52139/test.jar" ,true);
        v_Ftp.close();
        */
    }
    
}
