package org.hy.common.file.junit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;





/**
 * 抖动算法V2
 *
 * @author ZhengWei(HY)
 * @createDate 2024-04-12
 * @version v1.0
 */
public class JU_FloydSteinbergDithering2
{

    public static void main(String [] args)
    {
        // 读取输入图像
        BufferedImage inputImage = null;
        try
        {
            inputImage = ImageIO.read(new File("D:\\WorkSpace\\hy.common.file\\src\\test\\java\\org\\hy\\common\\file\\junit\\expandImage.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        // 定义需要保留的颜色列表
        Color [] preservedColors = {Color.BLACK ,Color.WHITE ,Color.GREEN ,Color.BLUE ,Color.RED ,Color.YELLOW ,Color.ORANGE}; // 这里假设你想保留红、绿、蓝三种颜色
        // 进行抖动处理并保留指定颜色
        BufferedImage outputImage = ditherAndPreserveColors(inputImage ,preservedColors);
        // 将处理后的图像保存到文件
        try
        {
            ImageIO.write(outputImage ,"png" ,new File("D:\\output_v2_BWGBRYO.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }



    public static BufferedImage ditherAndPreserveColors(BufferedImage inputImage ,Color [] preservedColors)
    {
        int width  = inputImage.getWidth();
        int height = inputImage.getHeight();
        
        BufferedImage outputImage = new BufferedImage(width ,height ,BufferedImage.TYPE_INT_RGB);
        // 定义Floyd-Steinberg误差扩散核
        int [] [] ditherMatrix = {{0 ,0 ,7} ,{3 ,5 ,1}};
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                Color oldColor = new Color(inputImage.getRGB(x ,y));
                // 如果颜色在保留列表中，则不做处理
                if ( isColorPreserved(oldColor ,preservedColors) )
                {
                    outputImage.setRGB(x ,y ,oldColor.getRGB());
                    continue;
                }
                // 其他颜色进行抖动处理
                Color newColor = findClosestColor(oldColor);
                // 将新颜色应用到当前像素
                outputImage.setRGB(x ,y ,newColor.getRGB());
                // 计算误差
                int quantErrorRed = oldColor.getRed() - newColor.getRed();
                int quantErrorGreen = oldColor.getGreen() - newColor.getGreen();
                int quantErrorBlue = oldColor.getBlue() - newColor.getBlue();
                // 将误差扩散到周围像素
                for (int [] d : ditherMatrix)
                {
                    int dx = d[1];
                    int dy = d[0];
                    if ( x + dx >= 0 && x + dx < width && y + dy >= 0 && y + dy < height )
                    {
                        Color neighborColor = new Color(inputImage.getRGB(x + dx ,y + dy));
                        int newRed = neighborColor.getRed() + quantErrorRed * d[2] / 16;
                        int newGreen = neighborColor.getGreen() + quantErrorGreen * d[2] / 16;
                        int newBlue = neighborColor.getBlue() + quantErrorBlue * d[2] / 16;
                        newRed = Math.min(255 ,Math.max(0 ,newRed));
                        newGreen = Math.min(255 ,Math.max(0 ,newGreen));
                        newBlue = Math.min(255 ,Math.max(0 ,newBlue));
                        Color updatedColor = new Color(newRed ,newGreen ,newBlue);
                        inputImage.setRGB(x + dx ,y + dy ,updatedColor.getRGB());
                    }
                }
            }
        }
        return outputImage;
    }



    public static Color findClosestColor(Color color)
    {
        // 这里是简单的例子，可以根据需要实现更复杂的颜色匹配算法
        int r = color.getRed() < 128 ? 0 : 255;
        int g = color.getGreen() < 128 ? 0 : 255;
        int b = color.getBlue() < 128 ? 0 : 255;
        return new Color(r ,g ,b);
    }



    public static boolean isColorPreserved(Color color ,Color [] preservedColors)
    {
        for (Color preservedColor : preservedColors)
        {
            if ( color.equals(preservedColor) )
            {
                return true;
            }
        }
        return false;
    }
    
}
