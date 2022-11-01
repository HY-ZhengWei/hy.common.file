package org.hy.common.file.junit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.hy.common.file.FileHelp;
import org.junit.Test;





public class JU_ResizeImage
{
    
    @Test
    public void test_resizeImage() throws IOException
    {
        FileHelp v_FileHelp = new FileHelp();
        
        BufferedImage v_Image = v_FileHelp.resizeImage("C:\\Users\\ZhengWei\\Desktop\\0380.png" ,400 ,176);
        
        ImageIO.write(v_Image ,"png" ,new File("C:\\Users\\ZhengWei\\Desktop\\0380_small.png"));
    }
    
}
