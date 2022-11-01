package org.hy.common.file;





/**
 * 超级大数据源的文件存储器接口。
 * 
 * @author      ZhengWei(HY)
 * @version     v1.0  
 * @createDate  2012-04-12
 */
public interface FileBiggerMemory 
{

	/**
	 * 获取一行的数据信息
	 * 
	 * 其返回值的类型只能是如下几种，否则会抛异常
	 * 第一种：org.hy.common.file.FileSerializable
	 * 第二种：java.util.List
	 * 
	 * @param i_RowIndex  行号。下标从0开始
	 * @return
	 */
	public Object getRowInfo(long i_RowIndex) throws Exception;
	
	
	
	/**
	 * 获取总的行数
	 * 
	 * @return
	 */
	public long getRowSize();
	
}
