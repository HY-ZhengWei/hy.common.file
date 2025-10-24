package org.hy.common.ftp;

import java.io.Serializable;

import org.hy.common.ftp.enums.PathType;





/**
 * FTP路径信息
 *
 * @author      ZhengWei(HY)
 * @createDate  2025-10-24
 * @version     v1.0
 */
public class FTPPath implements Serializable
{

    private static final long serialVersionUID = 5248544379188216787L;
    
    
    
    /** 路径名称（全路径） */
    private String   pathName;
    
    /** 路径类型 */
    private PathType pathType;
    
    
    
    public FTPPath()
    {
        
    }
    
    
    public FTPPath(String i_PathName ,PathType i_PathType)
    {
        this.pathName = i_PathName;
        this.pathType = i_PathType;
    }

    
    /**
     * 获取：路径名称（全路径）
     */
    public String getPathName()
    {
        return pathName;
    }

    
    /**
     * 设置：路径名称（全路径）
     * 
     * @param i_PathName 路径名称（全路径）
     */
    public void setPathName(String i_PathName)
    {
        this.pathName = i_PathName;
    }

    
    /**
     * 获取：路径类型
     */
    public PathType getPathType()
    {
        return pathType;
    }

    
    /**
     * 设置：路径类型
     * 
     * @param i_PathType 路径类型
     */
    public void setPathType(PathType i_PathType)
    {
        this.pathType = i_PathType;
    }


    @Override
    public String toString()
    {
        return this.pathName;
    }
    
}
