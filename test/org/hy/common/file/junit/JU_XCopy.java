package org.hy.common.file.junit;

import java.io.IOException;

import org.hy.common.file.FileHelp;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;




/**
 * 测试单元：模仿window的xcopy命令的功能
 *
 * @author      ZhengWei(HY)
 * @createDate  2015-01-21
 * @version     v1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class JU_XCopy
{
    
    @Test
    public void test_001() throws IOException
    {
        FileHelp v_FileHelp = new FileHelp();
        
        v_FileHelp.setOverWrite(true);
        
        v_FileHelp.xcopy("/Users/hy/WSS/WorkSpace_CW/部署环境/CW-Portal_外网服务" 
                        ,"/Users/hy/WSS/WorkSpace_CW/部署环境/CW-Portal_ServicePack_Internet" 
                        ,null
                        ,".DS_Store"    // 不在此文件
                        ,".*\\.class");    // 只要 *.js 文件
    } 
    
}
