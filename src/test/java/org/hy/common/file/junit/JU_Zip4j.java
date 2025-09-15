package org.hy.common.file.junit;

import java.io.IOException;

import org.hy.common.file.FileHelp;
import org.junit.Test;





public class JU_Zip4j
{
    
    @Test
    public void create() throws IOException
    {
        FileHelp v_FHelp    = new FileHelp();
        String   v_Password = "123456";
        String   v_File     = "C:\\Users\\ZLX\\Desktop\\郑伟.docx";
        String   v_Save     = "C:\\Users\\ZLX\\Desktop\\New.docx";
        
        v_FHelp.createZip4j(v_Save ,v_File ,v_Password);
        v_FHelp.UnCompressZip4j(v_Save ,"D:\\" ,v_Password);
    }
    
}
