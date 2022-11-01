package org.hy.common.file.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hy.common.file.FileHelp;
import org.junit.Test;





/**
 * 测试单元：压缩
 *
 * @author      ZhengWei(HY)
 * @createDate  2018-03-19
 * @version     v1.0
 */
public class JU_CreateZip
{
    
    public void test_Zip() throws IOException
    {
        File       v_RootFile = new File("D:\\apache-tomcat-7.0.47\\webapps\\calc");
        File       v_ZipFile  = new File("D:\\apache-tomcat-7.0.47\\webapps\\calc-Test.zip");
        FileHelp   v_FileHelp = new FileHelp();
        List<File> v_Files    = new ArrayList<File>();
        
        v_Files.add(new File("D:\\apache-tomcat-7.0.47\\webapps\\calc"));
        v_Files.addAll(v_FileHelp.getFiles(v_RootFile ,true));
        
        v_ZipFile.delete();
        v_FileHelp.createZip(v_ZipFile.toString() ,v_RootFile.getParent() ,v_Files);
    }
    
    
    
    @Test
    public void test_Zip4j() throws IOException
    {
        File       v_RootFile = new File("D:\\apache-tomcat-7.0.47\\webapps\\calc");
        File       v_ZipFile  = new File("D:\\apache-tomcat-7.0.47\\webapps\\calc-Test.zip");
        FileHelp   v_FileHelp = new FileHelp();
        
        v_ZipFile.delete();
        v_FileHelp.createZip4j(v_ZipFile.toString() ,v_RootFile.toString() ,true ,"hy");
    }
    
}
