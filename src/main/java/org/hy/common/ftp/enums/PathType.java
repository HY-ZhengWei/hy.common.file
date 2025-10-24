package org.hy.common.ftp.enums;





/**
 * 路径类型的枚举
 *
 * @author      ZhengWei(HY)
 * @createDate  2025-10-24
 * @version     v1.0
 */
public enum PathType
{
    
    NotExist    (-1 ,"路径不存"),
    
    Directory   (1  ,"目录"),
    
    File        (2  ,"文件"),
    
    SymbolicLink(3 ,"符号链接"),
    
    ;
    
    
    
    /** 值 */
    private Integer value;
    
    /** 描述 */
    private String  comment;
    
    
    
    /**
     * 数值转为常量
     * 
     * @author      ZhengWei(HY)
     * @createDate  2025-02-11
     * @version     v1.0
     *
     * @param i_Value
     * @return
     */
    public static PathType get(Integer i_Value)
    {
        if ( i_Value == null )
        {
            return null;
        }
        
        Integer v_Value = i_Value;
        for (PathType v_Enum : PathType.values())
        {
            if ( v_Enum.value.equals(v_Value) )
            {
                return v_Enum;
            }
        }
        
        return null;
    }
    
    
    
    PathType(Integer i_Value ,String i_Comment)
    {
        this.value   = i_Value;
        this.comment = i_Comment;
    }

    
    
    public Integer getValue()
    {
        return this.value;
    }
    
    
    
    public String getComment()
    {
        return this.comment;
    }
    
    

    public String toString()
    {
        return this.value + "";
    }
    
}
