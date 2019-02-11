package org.hy.common.file.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarFile;

import org.hy.common.file.FileHelp;
import org.junit.Test;





/**
 * 读取Jar包中XML文件的测试
 *
 * @author      ZhengWei(HY)
 * @createDate  2019-02-08
 * @version     v1.0
 */
public class JU_JarReadXML
{
    
    /**
     * 读取Jar包中的文件内容
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-08
     * @version     v1.0
     *
     * @throws IOException
     */
    @Test
    public void test_Jar() throws IOException
    {
        FileHelp v_FileHelp = new FileHelp();
        JarFile  v_JarFile  = new JarFile("D:/WorkSpace_SearchDesktop/hy.common.xjava/xjava.jar");
        String   v_Content  = v_FileHelp.getContent(v_JarFile ,"plugins/UnitConvert.xml" ,"UTF-8" ,true);
        
        v_JarFile.close();
        
        System.out.println(v_Content);
    }
    
    
    
    
    /**
     * 下面的方法未能成功读取。但JarFile的方式可以实现，见test_Jar()方法。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-08
     * @version     v1.0
     *
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @Test
    public void test_JarReadXML() throws ClassNotFoundException, IOException
    {
        FileHelp v_FileHelp = new FileHelp();
        String   v_FilePath = "file:/D:/WorkSpace_SearchDesktop/hy.common.xjava/xjava.jar!/org/hy/common/xml/plugins/UnitConvert.xml";
        URL      v_URL      = new URL(v_FilePath);
        
        
        FileInputStream v_InputStream = null;
        String          v_Content     = "";
        
        try
        {
            v_InputStream = new FileInputStream(new File(v_URL.toURI()));
            v_Content = v_FileHelp.getContent(v_InputStream ,"UTF-8" ,true);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        
        System.out.println(v_Content);
    }
    
}
