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
    
    /** 路径最末端的名称 */
    private String   simpleName;
    
    /** 路径类型 */
    private PathType pathType;
    
    
    
    public FTPPath()
    {
        
    }
    
    
    public FTPPath(String i_PathName ,String i_SimpleName ,PathType i_PathType)
    {
        this.pathName   = i_PathName;
        this.simpleName = i_SimpleName;
        this.pathType   = i_PathType;
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

    
    /**
     * 获取：路径最末端的名称
     */
    public String getSimpleName()
    {
        return simpleName;
    }

    
    /**
     * 设置：路径最末端的名称
     * 
     * @param i_SimpleName 路径最末端的名称
     */
    public void setSimpleName(String i_SimpleName)
    {
        this.simpleName = i_SimpleName;
    }


    @Override
    public String toString()
    {
        return this.pathName;
    }
    
}
