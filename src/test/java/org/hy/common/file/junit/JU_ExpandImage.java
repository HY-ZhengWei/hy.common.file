package org.hy.common.file.junit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import org.hy.common.file.FileHelp;
import org.junit.Test;





/**
 * 测试单元：扩大图片的画布大小（从中心点扩大）
 *
 * @author      ZhengWei(HY)
 * @createDate  2023-09-06
 * @version     v1.0
 */
public class JU_ExpandImage
{
    
    @Test
    public void test() throws MalformedURLException, IOException
    {
        BufferedImage v_Image = FileHelp.getContentImage("D:\\WorkSpace\\hy.common.file\\src\\test\\java\\org\\hy\\common\\file\\junit\\expandImage.png");
        
        v_Image = FileHelp.expandImage(v_Image ,1010 ,573 ,0x00FFFFFF);
        
        ImageIO.write(v_Image, "png", new File("C:\\Users\\hyzhe\\Desktop\\JU_ExpandImage.png"));
    }
    
}
