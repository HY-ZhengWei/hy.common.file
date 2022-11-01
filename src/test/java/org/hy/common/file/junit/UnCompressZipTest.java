package org.hy.common.file.junit;

import static org.junit.Assert.*;

import java.io.File;
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
    
    
    public void test_001() throws IOException
    {
		String   v_ZipName  = "D:\\apache-tomcat-7.0.47\\webapps\\calc-Test.zip";
		File     v_ZipFile  = new File(v_ZipName);
        FileHelp v_FileHelp = new FileHelp();

		if ( v_ZipFile.exists() )
		{
			v_FileHelp.UnCompressZip(v_ZipName, "E:\\", true);
		}
    }
    
    
    
    @Test
    public void test_002() throws IOException 
    {
        FileHelp v_FileHelp = new FileHelp();
        File     v_ZipFile  = new File("D:\\apache-tomcat-7.0.47\\webapps\\calc-Test.zip");

		if ( v_ZipFile.exists() )
		{
			v_FileHelp.UnCompressZip4j(v_ZipFile, "D:\\apache-tomcat-7.0.47\\webapps\\zip4j", "hy");
		}
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
