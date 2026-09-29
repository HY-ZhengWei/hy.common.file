package org.hy.common.xml.junit.log;

import org.hy.common.xml.log.Logger;
import org.junit.Test;





/**
 * 测试单元：递归中输出日志
 *
 * @author      ZhengWei(HY)
 * @createDate  2026-09-29
 * @version     v1.0
 */
public class JU_LogFor
{
    
    private static Logger $Logger = new Logger(JU_LogFor.class ,true);
    
    
    
    /** 计数器 */
    private int index;
    
    /** 计数器的最大值上限 */
    private int max;
    
    
    
    public JU_LogFor()
    {
        this.index = 0;
        this.max   = 100_000_000;
    }
    
    
    
    /**
     *                                   单线程 100线程用量 200线程用量 500线程用量 
     * 配置：-Xss1m， 可递归到   2K 层的深度   1mb   100mb     200mb     500mb
     * 配置：-Xss2m， 可递归到   6K 层的深度   2mb   200mb     400mb       1gb
     * 配置：-Xss4m， 可递归到   3W 层的深度   4mb   400mb     800mb       2gb
     * 配置：-Xss8m， 可递归到   8W 层的深度   8mb   800mb    1600mb       4gb
     * 配置：-Xss16m，可递归到  18W 层的深度  16mb   1.6gb     3.2gb       8gb
     * 配置：-Xss24m，可递归到  27W 层的深度  24mb   2.4gb     4.8gb      12gb
     * 配置：-Xss32m，可递归到  38W 层的深度  32mb   3.2gb     6.4gb      16gb
     * 配置：-Xss48m，可递归到  58W 层的深度  48mb   4.8gb     9.6gb      24gb
     * 配置：-Xss64m，可递归到  77W 层的深度  64mb   6.4gb    12.8gb      32gb
     * 配置：-Xss96m，可递归到 123W 层的深度  96mb   9.6gb    19.2gb      48gb
     */
    @Test
    public void forToMax()
    {
        this.execute();
    }
    
    
    
    public void execute()
    {
        this.index++;
        if ( this.index <= this.max )
        {
            $Logger.info(this.index + "：PLC Read 粘度检测XPLC_Viscosity.粘度检测：状态XDG_Viscosity_State");
            this.execute();
        }
    }
    
}
