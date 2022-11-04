package org.hy.common.ftp.junit;

import static org.junit.Assert.assertTrue;

import org.hy.common.StringHelp;
import org.hy.common.ftp.FTPHelp;
import org.hy.common.ftp.FTPInfo;





/**
 * 测试FTP下载功能
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-08-17
 */
public class FTPHelpTest
{

    @SuppressWarnings("unused")
    // @Test
    public void testDownload()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("133.64.89.12");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(false);
        
        FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo);
        
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
        
        v_FTPHelp.close();
        
        
        assertTrue(v_Ret == null);
    }
    
    
    
    // @Test
    public void testDownloadToString()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("133.64.89.12");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp");
        v_FTPInfo.setPassword("ftp");
        v_FTPInfo.setLocalPassiveMode(false);
        v_FTPInfo.setInitPath("/share/ftp");
        
        FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo);
        v_FTPHelp.connect();
        v_FTPHelp.setDataSafe(false);
        
        String v_FileText = v_FTPHelp.download("hosts");
        
        v_FTPHelp.close();
        
        System.out.println(v_FileText);
        System.out.println(StringHelp.hexToBytes(v_FileText));
        System.out.println(new String(StringHelp.hexToBytes(v_FileText)));
        
        String v_Text = "ab123我爱你456!@";
        System.out.println(new String(v_Text.getBytes()));
        System.out.println(new String(StringHelp.hexToBytes(StringHelp.bytesToHex(v_Text.getBytes()))));
    }
    
    
    
    // @Test
    public void testUpload()
    {
        FTPInfo v_FTPInfo = new FTPInfo();
        
        v_FTPInfo.setIp("133.64.32.46");
        v_FTPInfo.setPort(21);
        v_FTPInfo.setUser("ftp01");
        v_FTPInfo.setPassword("password01");
        v_FTPInfo.setLocalPassiveMode(false);
        v_FTPInfo.setInitPath("/ftp/file01");
        
        FTPHelp v_FTPHelp = new FTPHelp(v_FTPInfo);
        
        v_FTPHelp.connect();
        
        String v_Ret = v_FTPHelp.upload("O:\\WorkSpace_SearchDesktop\\SearchDesktop\\UltraEdit_3.tar.gz" ,"/ftp/file01/HY_20130308.txt");
        
        v_FTPHelp.close();
        
        System.out.println(v_Ret);
        
        assertTrue(v_Ret == null);
    }
    
    
    
    public static void main(String [] args)
    {
        FTPHelpTest v_Test = new FTPHelpTest();
        
        v_Test.testDownloadToString();
    }

}
