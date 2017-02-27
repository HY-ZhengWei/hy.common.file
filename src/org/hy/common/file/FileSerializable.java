package org.hy.common.file;

import java.io.Serializable;





/**
 * 将对象简单序列化，并按顺序写入到文件中的接口。
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-04
 */
public interface FileSerializable extends Serializable 
{

	/**
	 * 获取属性的数量
	 * 
	 * @return
	 */
	public int gatPropertySize();
	
	
	
	/**
	 * 获取指定顺序上的属性名称
	 * 
	 * @param i_PropertyIndex  下标从0开始
	 * @return
	 */
	public String gatPropertyName(int i_PropertyIndex);
	
	
	
	/**
	 * 获取指定顺序上的属性值
	 * 
	 * @param i_PropertyIndex  下标从0开始
	 * @return
	 */
	public Object gatPropertyValue(int i_PropertyIndex);
	
	
	
	/**
	 * 释放资源
	 */
	public void freeResource();
	
}
