package org.hy.common.file.junit;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import org.hy.common.file.FileHelp;





/**
 * 解压缩文件成字符串的测试类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2013-04-10
 */
public class UnCompressZipTest 
{
    
    @Test
    public void test_001() throws IOException
    {
        FileHelp v_FileHelp = new FileHelp();
        
        v_FileHelp.UnCompressZip("D:\\apache-tomcat-7.0.47\\webapps\\1.zip" ,"E:\\" ,true);
    }

    
	
	public void testUnCompressZipURLString() 
	{
		FileHelp v_FileHelp = new FileHelp();
		
		try 
		{
			String v_Context = v_FileHelp.UnCompressZip(new URL("file:\\C:\\ui.XBF.zip.xml") ,"ui.XBF.xml" ,"UTF-8");
			System.out.println(v_Context);
		} 
		catch (Exception e) 
		{
			fail("Not yet implemented");
			e.printStackTrace();
		}
		
		assertTrue(true);
	}

}
