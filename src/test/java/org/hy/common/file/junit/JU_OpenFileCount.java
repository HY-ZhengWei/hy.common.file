package org.hy.common.file.junit;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

import org.junit.Test;





/**
 * 测试单元：打开文件的数量
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-01-17
 * @version     v1.0
 */
public class JU_OpenFileCount
{
    
    @Test
    public void test01() throws IOException
    {
        WatchService v_WatchService = FileSystems.getDefault().newWatchService();
        
        for (FileStore v_Item : FileSystems.getDefault().getFileStores())
        {
            System.out.println(v_Item);
        }
    }
    
}
