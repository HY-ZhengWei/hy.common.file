package org.hy.common.file.junit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;

import org.hy.common.Return;
import org.hy.common.file.FileHelp;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;





/**
 * 生成图片滑动登录验证的大图和小图
 *
 * @author      ZhengWei(HY)
 * @createDate  2021-05-17
 * @version     v1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JU_CheckImage
{
    
    @Test
    public void test_CheckImage() throws MalformedURLException, IOException
    {
        Color v_TransparentColor = new Color(255 ,255 ,255);
        
        BufferedImage v_OutLineImageA = FileHelp.getContentImage("C:\\WorkSpace\\hy.common.file\\test\\org\\hy\\common\\file\\junit\\CheckImageTemplateA.png");
        BufferedImage v_OutLineImageB = FileHelp.getContentImage("C:\\WorkSpace\\hy.common.file\\test\\org\\hy\\common\\file\\junit\\CheckImageTemplateB.png");
        Return<?>[][] v_OutlineData   = FileHelp.getImageOutline(v_OutLineImageA ,v_TransparentColor.getRGB());
        BufferedImage v_OrgImage      = FileHelp.getContentImage("C:\\WorkSpace\\hy.common.file\\test\\org\\hy\\common\\file\\junit\\CheckImage.png");
        BufferedImage v_NewImage      = new BufferedImage(v_OutlineData.length ,v_OutlineData[0].length, BufferedImage.TYPE_INT_ARGB);
        
        FileHelp.cutImage(v_OrgImage ,v_NewImage ,v_OutlineData ,200 ,200);
        FileHelp.overAlphaImage(v_OrgImage ,v_OutLineImageB ,200 ,200 ,0.5F);
        
        ImageIO.write(v_NewImage, "png", new File("C:\\WorkSpace\\hy.common.file\\test\\org\\hy\\common\\file\\junit\\NewSmallImage.png"));
        ImageIO.write(v_OrgImage, "png", new File("C:\\WorkSpace\\hy.common.file\\test\\org\\hy\\common\\file\\junit\\NewOrgImage.png"));
    }
    
}
