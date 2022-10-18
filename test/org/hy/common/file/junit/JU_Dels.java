package org.hy.common.file.junit;

import java.io.File;

import org.hy.common.Help;
import org.hy.common.file.FileHelp;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;




/**
 * 测试单元：批量删除的功能
 *
 * @author      ZhengWei(HY)
 * @createDate  2022-09-13
 * @version     v1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JU_Dels
{
    
    @Test
    public void test_dels()
    {
        dels(new File("//192.168.31.199/Data2022/ZhengWei/WSS") ,new String[]{"CVS" ,".svn" ,"classes"} ,new String[] {".DS_Store"});
        System.out.println("检索完成");
    }
    
    
    
    public void dels(File i_FindFolder ,String [] i_DelFolderNames ,String [] i_DelFileNames)
    {
        if ( i_FindFolder == null )
        {
            return;
        }
        
        if ( !i_FindFolder.exists() && !i_FindFolder.isDirectory() )
        {
            return;
        }
        
        File [] v_Files = i_FindFolder.listFiles();
        if ( Help.isNull(v_Files) )
        {
            return;
        }
        
        for (File v_File : v_Files)
        {
            if ( v_File.isDirectory() )
            {
                if ( v_File.getName().equals(".git")
                  || v_File.getName().equals(".gradle")
                  || v_File.getName().equals(".idea")
                  || v_File.getName().equals(".metadata") )
                {
                    continue;
                }
                
                boolean v_IsFindDel = false;
                for (String v_DelFolderName : i_DelFolderNames)
                {
                    if ( v_File.getName().equals(v_DelFolderName) )
                    {
                        System.out.println(v_File.getAbsolutePath() + " ... DEL");
                        FileHelp v_FileHelp = new FileHelp();
                        v_FileHelp.delFiles(v_File ,0 ,true);
                        v_File.delete();
                        v_IsFindDel = true;
                        break;
                    }
                }
                
                if ( !v_IsFindDel )
                {
                    System.out.println(v_File.getAbsolutePath());
                    dels(v_File ,i_DelFolderNames ,i_DelFileNames);
                }
            }
            else
            {
                for (String v_DelFileName : i_DelFileNames)
                {
                    if ( v_File.getName().equals(v_DelFileName) )
                    {
                        System.out.println(v_File.getAbsolutePath() + " ... DEL");
                        v_File.delete();
                        break;
                    }
                }
            }
        }
    }
    
}