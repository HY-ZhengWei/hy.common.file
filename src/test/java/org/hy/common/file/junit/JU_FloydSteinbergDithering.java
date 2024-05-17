package org.hy.common.file.junit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;





/**
 * 抖动算法
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-04-12
 * @version     v1.0
 */
public class JU_FloydSteinbergDithering
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
        // 进行抖动处理
        BufferedImage outputImage = dither(inputImage ,7); // 在此处传入色彩深度，例如4。即保留几种颜色
        // 将处理后的图像保存到文件
        try
        {
            ImageIO.write(outputImage ,"png" ,new File("D:\\output.png"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }



    public static BufferedImage dither(BufferedImage inputImage ,int colorDepth)
    {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width ,height ,BufferedImage.TYPE_INT_RGB);
        // 定义Floyd-Steinberg误差扩散核
        int [] [] ditherMatrix = {{0 ,0 ,7} ,{3 ,5 ,1}};
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                Color oldColor = new Color(inputImage.getRGB(x ,y));
                Color newColor = findClosestColor(oldColor ,colorDepth);
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



    public static Color findClosestColor(Color color ,int colorDepth)
    {
        int rStep = 256 / colorDepth;
        int gStep = 256 / colorDepth;
        int bStep = 256 / colorDepth;
        int r = (color.getRed() / rStep) * rStep;
        int g = (color.getGreen() / gStep) * gStep;
        int b = (color.getBlue() / bStep) * bStep;
        return new Color(r ,g ,b);
    }
}
