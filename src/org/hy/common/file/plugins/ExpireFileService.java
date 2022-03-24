package org.hy.common.file.plugins;

import java.io.File;

import org.hy.common.Date;
import org.hy.common.file.FileHelp;





/**
 * 过期文件的服务
 *
 * @author      ZhengWei(HY)
 * @createDate  2017-06-21
 * @version     v1.0
 */
public class ExpireFileService
{
    /** 目录全路径 */
    private String  folder;
    
    /** 过期天数（即，只保留多少天的文件） */
    private Integer expireDays;
    
    /** 是否递归的删除所有子目录中的过期文件 */
    private Boolean isDelChilds;
    
    /** 排除在外的文件是哪些，格式如："|文件名称|"。为NULL时表示不排除 */
    private String  excludeFiles;
    
    /** 排除在外的目录是哪些，格式如："|目录名称|"。为NULL时表示不排除 */
    private String  excludeFolders;
    
    
    
    public void delExpireFiles()
    {
        FileHelp v_FileHelp   = new FileHelp();
        Date     v_ExpireTime = Date.getNowTime().getDate(Math.abs(this.expireDays)  * -1);
        
        v_FileHelp.delFiles(new File(this.folder) ,v_ExpireTime ,this.isDelChilds ,this.excludeFiles ,this.excludeFolders);
    }

    
    /**
     * 获取：目录全路径
     */
    public String getFolder()
    {
        return folder;
    }

    
    /**
     * 设置：目录全路径
     * 
     * @param folder
     */
    public void setFolder(String folder)
    {
        this.folder = folder;
    }

    
    /**
     * 获取：过期天数（即，只保留多少天的文件）
     */
    public Integer getExpireDays()
    {
        return expireDays;
    }

    
    /**
     * 设置：过期天数（即，只保留多少天的文件）
     * 
     * @param expireDays
     */
    public void setExpireDays(Integer expireDays)
    {
        this.expireDays = expireDays;
    }


    /**
     * 获取：是否递归的删除所有子目录中的过期文件
     */
    public Boolean getIsDelChilds()
    {
        return isDelChilds;
    }


    /**
     * 设置：是否递归的删除所有子目录中的过期文件
     * 
     * @param isDelChilds
     */
    public void setIsDelChilds(Boolean isDelChilds)
    {
        this.isDelChilds = isDelChilds;
    }


    /**
     * 获取：排除在外的文件是哪些，格式如："|文件名称|"。为NULL时表示不排除
     */
    public String getExcludeFiles()
    {
        return excludeFiles;
    }


    /**
     * 设置：排除在外的文件是哪些，格式如："|文件名称|"。为NULL时表示不排除
     * 
     * @param excludeFiles
     */
    public void setExcludeFiles(String excludeFiles)
    {
        this.excludeFiles = excludeFiles;
    }


    /**
     * 获取：排除在外的目录是哪些，格式如："|目录名称|"。为NULL时表示不排除
     */
    public String getExcludeFolders()
    {
        return excludeFolders;
    }


    /**
     * 设置：排除在外的目录是哪些，格式如："|目录名称|"。为NULL时表示不排除
     * 
     * @param excludeFolders
     */
    public void setExcludeFolders(String excludeFolders)
    {
        this.excludeFolders = excludeFolders;
    }
    
}
