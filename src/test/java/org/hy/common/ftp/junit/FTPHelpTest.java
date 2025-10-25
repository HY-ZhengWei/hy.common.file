package org.hy.common.ftp.junit;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.ftp.FTPHelp;
import org.hy.common.ftp.FTPInfo;
import org.hy.common.ftp.FTPPath;
import org.hy.common.ftp.enums.PathType;
import org.junit.Test;





/**
 * 测试FTP下载功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-08-17
 */
public class FTPHelpTest
{
    
    @Test
    public void test_getPathType()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("127.0.0.1");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("iot202498");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(true);
        
        try (FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo))
        {
            v_FTPHelp.connect();
            
            PathType v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/1.0.0/");
            System.out.println("目录，有子文件 = " + v_PathType.getComment());
            
            v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/testDir");
            System.out.println("目录，但无文件 = " + v_PathType.getComment());
            
            v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/我是一个不存在的目录");
            System.out.println("目录，不存在的 = " + v_PathType.getComment());
            
            v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/testFile");
            System.out.println("文件，是存在的 = " + v_PathType.getComment());
            
            v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/我是一个不存在的假文件.txt");
            System.out.println("文件，不存在的 = " + v_PathType.getComment());
            
            v_PathType = v_FTPHelp.getPathType("/opt/ftpRoot/1.0.0/hy.common.callflow-1.0.0.jar");
            System.out.println("文件，是存在的 = " + v_PathType.getComment());
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    // @Test
    public void test_getFiles()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("127.0.0.1");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("iot202498");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(true);
        
        try (FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo))
        {
            v_FTPHelp.connect();
            
            List<FTPPath> v_Files = v_FTPHelp.getFiles("/opt/ftpRoot/");
            Help.print(v_Files);
            
            v_Files = v_FTPHelp.getFiles("/opt/ftpRoot/" ,true);
            Help.print(v_Files);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    @SuppressWarnings("unused")
    // @Test
    public void testDownload()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("127.0.0.1");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(false);
        
        try (FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo))
        {
            v_FTPHelp.connect();
            
            String v_Ret          = null;
            int    v_SucceedCount = 0;
            int    v_FailCount    = 0;
            
            for (int v_Index=0; v_Index<100; v_Index++)
            {
                v_Ret = v_FTPHelp.download("/share/c1/1/0/20120817/1997/1000016.V3" ,"C:\\Ftp_Download.test" + v_Index);
                
                System.out.println("-- " + v_Index + ": " + v_Ret);
                if ( v_Ret == null )
                {
                    v_SucceedCount++;
                }
                else
                {
                    v_FailCount++;
                }
            }
            
            assertTrue(v_Ret == null);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    // @Test
    public void testDownloadToString()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("127.0.0.1");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(false);
        v_FTPInfo.setInitPath("/share/ftp");
        
        try (FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo))
        {
            v_FTPHelp.connect();
            v_FTPHelp.setDataSafe(false);
            
            String v_FileText = v_FTPHelp.download("hosts");
            
            System.out.println(v_FileText);
            System.out.println(StringHelp.hexToBytes(v_FileText));
            System.out.println(new String(StringHelp.hexToBytes(v_FileText)));
            
            String v_Text = "ab123我爱你456!@";
            System.out.println(new String(v_Text.getBytes()));
            System.out.println(new String(StringHelp.hexToBytes(StringHelp.bytesToHex(v_Text.getBytes()))));
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    // @Test
    public void testUpload()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("127.0.0.1");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp01");
        v_FTPInfo.setPassword("password01");
        v_FTPInfo.setLocalPassiveMode(false);
        v_FTPInfo.setInitPath("/ftp/file01");
        
        try (FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo))
        {
            v_FTPHelp.connect();
            
            String v_Ret = v_FTPHelp.upload("O:\\WorkSpace_SearchDesktop\\SearchDesktop\\UltraEdit_3.tar.gz" ,"/ftp/file01/HY_20130308.txt");
            
            System.out.println(v_Ret);
            
            assertTrue(v_Ret == null);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
    
    
    public static void main(String [] args)
    {
        FTPHelpTest v_Test = new FTPHelpTest();
        
        v_Test.testDownloadToString();
    }

}
