package org.hy.common.file;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hy.common.ByteHelp;
import org.hy.common.Date;
import org.hy.common.ExpireMap;
import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.file.event.CreateCSVEvent;
import org.hy.common.file.event.CreateCSVListener;
import org.hy.common.file.event.CreateZipEvent;
import org.hy.common.file.event.CreateZipListener;
import org.hy.common.file.event.DefaultCreateCSVEvent;
import org.hy.common.file.event.DefaultCreateZipEvent;
import org.hy.common.file.event.DefaultFileCopyEvent;
import org.hy.common.file.event.DefaultFileReadEvent;
import org.hy.common.file.event.DefaultUnCompressZipEvent;
import org.hy.common.file.event.FileCopyEvent;
import org.hy.common.file.event.FileCopyListener;
import org.hy.common.file.event.FileReadEvent;
import org.hy.common.file.event.FileReadListener;
import org.hy.common.file.event.UnCompressZipEvent;
import org.hy.common.file.event.UnCompressZipListener;

import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;





/**
 * 文件操作的辅助类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-03
 *           V2.0  2015-01-21  1.添加：xcopy(...) 系列方法
 *           V2.1  2015-01-22  1.整理优化 getContent(...) 系列方法
 *                             2.添加：getContentByte(...) 系列方法
 *           V2.2  2016-02-20  1.getContent(...) 系统方法添加：生成的文件内容是否包含 “回车换行” 符功能
 *           V2.3  2017-09-07  1.添加：追加写入数据的模式 
 *                             2.添加：create(byte[])二进制数据的写入
 *           v3.0  2017-10-09  1.添加：断点上传的服务端功能
 *           v3.1  2017-10-11  1.添加：获取Http Post请求中的数据，getContent(...)
 *                             2.添加：Http Post请求返回数据的写入，writeHttp(...)
 *           v3.2  2018-01-29  1.添加：更加详细的日志内容
 *           v4.0  2018-03-13  1.添加：递归的获取目录所有文件及子目录，getFiles(...)
 *                             2.添加：递归的计算目录所有文件及子目录文件的合计大小。calcSize(...)
 *                             3.修复：UnCompressZip()解压时用出现解压不完整的问题。
 *           v5.0  2018-03-15  1.添加：读取文件内容getContent(...)的事件处理机制
 *                             2.添加：读取文件内容getContent(...)是否返回读取到文件内容isReturnContent。
 *                                    可支持超大文件的读取。
 *           v5.1  2018-03-16  1.修复：UnCompressZip()当Window环境下打包的压缩包，在Linux环境解压时，因路径符不同而造成错误。
 *           v6.0  2018-04-09  1.添加：Zip4j技术的压缩及解压的功能，支持加密功能。
 *           v7.0  2019-01-10  1.添加：缩放图片（限制最大宽度或最大高度）
 *                             2.添加：缩放图片（限制最小宽度或最小高度）
 *                             3.添加：缩放图片（按实际大小）
 *           v8.0  2019-02-08  1.添加：读取外部Jar包中的文件内容的方法getContent(JarFile ...)
 *                             2.修改：所有getContent()方法的默认文件编码读取方式由GBK改为UTF-8
 */
public final class FileHelp 
{
    
    /** 上传中 */
    public  static final int                               $Upload_GoOn   = 0;
    
    /** 上传全部完成 */
    public  static final int                               $Upload_Finish = 1;
    
    /** 上传异常 */
    public  static final int                               $Upload_Error  = -1;
    
    
    
    /** 临时记录最新一次数据包信息 */
    private static final ExpireMap<String ,FileDataPacket> $DataPackets   = new ExpireMap<String ,FileDataPacket>();
    
    
    
	/** 缓存大小 */
	private int                                  bufferSize    = 128 * 1024;
	
	/** 文件新行的标识 */
	private String                               newLine       = "\r\n";
	
	/** 是否覆盖 */
	private boolean                              isOverWrite   = false;
	
	/** 是否追加写入数据 */
	private boolean                              isAppend      = false;
	
	/** 编码类型 */
	private String                               charEncoding  = "UTF-8";
	
	/** Http内容类型 */
	private String                               contentType   = "application/json; charset=utf-8";
    
    /** CSV文件数据项加的前缀。如=号，可防止用Excel打开数字乱码 */
    private String                               csvDataPrefix = "=";
    
    /** 数据包的超时时长（单位：秒） */
    private long                                 dataPacketTimeOut = 10 * 60;
    
    /** 
     * 是否返回文件内容。
     * 
     * 用于getContent(...)系列方法，当isReturnContent为真时，getContent(...)方法将有返回值，否则返回值为null。
     * 
     * 一般isReturnContent为假时，与 readListener 监听器配合使用，可支持超大文件的读取。
     * 
     * 此为性能参数
     */
    private boolean                              isReturnContent = true;
    
    
	
	/** 自定义事件的监听器集合--文件拷贝 */
	private Collection<FileCopyListener>         fileCopyListeners;
	
	/** 自定义事件的监听器集合--创建CSV文件 */
	private Collection<CreateCSVListener>        createCSVListeners;
	
	/** 自定义事件的监听器集合--创建Zip文件 */
	private Collection<CreateZipListener>        createZipListeners;
	
	/** 自定义事件的监听器集合--解压缩Zip文件 */
	private Collection<UnCompressZipListener>    unCompressZipListeners;
	
	/** 自定义事件的监听器集合--读取文件内容 */
	private Collection<FileReadListener>         readListener;
	
	
	
	public FileHelp()
	{
		
	}
	
	
	
	/**
     * 注册读取文件内容事件
     * 
     * @param e
     */
    public void addReadListener(FileReadListener e)
    {
        if ( this.readListener == null )
        {
            this.readListener = new HashSet<FileReadListener>();
        }
        
        this.readListener.add(e);
    }
    
    
    
    /**
     * 移除读取文件内容事件
     * 
     * @param e
     */
    public void removeReadListener(FileReadListener e)
    {
        if ( this.readListener == null )
        {
            return;
        }
        
        this.readListener.remove(e);
    }
    
    
    
    /**
     * 触发读取文件内容之前的事件
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    protected boolean fireReadBeforeListener(FileReadEvent i_Event)
    {
        if ( this.readListener == null )
        {
            return true;
        }
        
        return notifyReadBeforeListeners(i_Event);
    }
    
    
    
    /**
     * 触发读取文件内容事件
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    protected boolean fireReadingListener(FileReadEvent i_Event)
    {
        if ( this.readListener == null )
        {
            return true;
        }
        
        return notifyReadingListeners(i_Event);
    }
    
    
    
    /**
     * 触发读取文件内容完成之后的事件
     * 
     * @param i_Event
     */
    protected void fireReadAfterListener(FileReadEvent i_Event)
    {
        if ( this.readListener == null )
        {
            return;
        }
        
        notifyReadAfterListeners(i_Event);
    }

    
    
    /**
     * 通知所有注册读取文件内容之前的事件监听的对象
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    private boolean notifyReadBeforeListeners(FileReadEvent i_Event)
    {
        Iterator<FileReadListener> v_Iter       = this.readListener.iterator();
        boolean                    v_IsContinue = true;

        while ( v_IsContinue && v_Iter.hasNext() ) 
        {
            v_IsContinue = v_Iter.next().readBefore(i_Event);
        }
        
        return v_IsContinue;
    }
    
    
    
    /**
     * 通知所有注册读取文件内容事件监听的对象
     * 
     * @param i_Event
     */
    private boolean notifyReadingListeners(FileReadEvent i_Event)
    {
        Iterator<FileReadListener> v_Iter       = this.readListener.iterator();
        boolean                    v_IsContinue = true;

        while ( v_IsContinue && v_Iter.hasNext() ) 
        {
            v_IsContinue = v_Iter.next().readProcess(i_Event);
        }
        
        return v_IsContinue;
    }

    
    
    /**
     * 通知所有注册读取文件内容完成之后的事件监听的对象
     * 
     * @param i_Event
     */
    private void notifyReadAfterListeners(FileReadEvent i_Event)
    {
        Iterator<FileReadListener> v_Iter = this.readListener.iterator();

        while ( v_Iter.hasNext() ) 
        {
            v_Iter.next().readAfter(i_Event);
        }
    }
	
	
	
	/**
	 * 注册拷贝文件事件
	 * 
	 * @param e
	 */
	public void addCopyListener(FileCopyListener e)
	{
		if ( this.fileCopyListeners == null )
		{
			this.fileCopyListeners = new HashSet<FileCopyListener>();
		}
		
		this.fileCopyListeners.add(e);
	}
	
	
	
	/**
	 * 移除拷贝事件
	 * 
	 * @param e
	 */
	public void removeCopyListener(FileCopyListener e)
	{
		if ( this.fileCopyListeners == null )
		{
			return;
		}
		
		this.fileCopyListeners.remove(e);
	}
	
	
	
	/**
	 * 触发拷贝文件之前的事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCopyBeforeListener(FileCopyEvent i_Event)
	{
		if ( this.fileCopyListeners == null )
		{
			return true;
		}
		
		return notifyCopyBeforeListeners(i_Event);
	}
	
	
	
	/**
	 * 触发拷贝文件事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCopyingListener(FileCopyEvent i_Event)
	{
		if ( this.fileCopyListeners == null )
		{
			return true;
		}
		
		return notifyCopyingListeners(i_Event);
	}
	
	
	
	/**
	 * 触发拷贝文件完成之后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireCopyAfterListener(FileCopyEvent i_Event)
	{
		if ( this.fileCopyListeners == null )
		{
			return;
		}
		
		notifyCopyAfterListeners(i_Event);
	}

	
	
	/**
	 * 通知所有注册拷贝文件之前的事件监听的对象
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	private boolean notifyCopyBeforeListeners(FileCopyEvent i_Event)
	{
		Iterator<FileCopyListener> v_Iter       = this.fileCopyListeners.iterator();
		boolean                    v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().copyBefore(i_Event);
		}
		
		return v_IsContinue;
	}
	
	
	
	/**
	 * 通知所有注册拷贝文件事件监听的对象
	 * 
	 * @param i_Event
	 */
	private boolean notifyCopyingListeners(FileCopyEvent i_Event)
	{
		Iterator<FileCopyListener> v_Iter       = this.fileCopyListeners.iterator();
		boolean                    v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().copyProcess(i_Event);
		}
		
		return v_IsContinue;
	}

	
	
	/**
	 * 通知所有注册拷贝文件完成之后的事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyCopyAfterListeners(FileCopyEvent i_Event)
	{
		Iterator<FileCopyListener> v_Iter = this.fileCopyListeners.iterator();

		while ( v_Iter.hasNext() ) 
		{
			v_Iter.next().copyAfter(i_Event);
		}
	}
	
	
	
	/**
	 * 注册创建CSV文件事件
	 * 
	 * @param e
	 */
	public void addCreateCSVListener(CreateCSVListener e)
	{
		if ( this.createCSVListeners == null )
		{
			this.createCSVListeners = new HashSet<CreateCSVListener>();
		}
		
		this.createCSVListeners.add(e);
	}
	
	
	
	/**
	 * 移除创建CSV文件事件
	 * 
	 * @param e
	 */
	public void removeCreateCSVListener(CreateCSVListener e)
	{
		if ( this.createCSVListeners == null )
		{
			return;
		}
		
		this.createCSVListeners.remove(e);
	}
	
	
	
	/**
	 * 触发创建CSV文件之前的事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCreateCSVBeforeListener(CreateCSVEvent i_Event)
	{
		if ( this.createCSVListeners == null )
		{
			return true;
		}
		
		return notifyCreateCSVBeforeListeners(i_Event);
	}
	
	
	
	/**
	 * 触发创建CSV文件事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCreatingCSVListener(CreateCSVEvent i_Event)
	{
		if ( this.createCSVListeners == null )
		{
			return true;
		}
		
		return notifyCreatingCSVListeners(i_Event);
	}
	
	
	
	/**
	 * 触发创建CSV文件完成之后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireCreateCSVAfterListener(CreateCSVEvent i_Event)
	{
		if ( this.createCSVListeners == null )
		{
			return;
		}
		
		notifyCreateCSVAfterListeners(i_Event);
	}

	
	
	/**
	 * 通知所有注册创建CSV文件之前的事件监听的对象
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	private boolean notifyCreateCSVBeforeListeners(CreateCSVEvent i_Event)
	{
		Iterator<CreateCSVListener> v_Iter       = this.createCSVListeners.iterator();
		boolean                     v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().createCSVBefore(i_Event);
		}
		
		return v_IsContinue;
	}
	
	
	
	/**
	 * 通知所有注册创建CSV文件事件监听的对象
	 * 
	 * @param i_Event
	 */
	private boolean notifyCreatingCSVListeners(CreateCSVEvent i_Event)
	{
		Iterator<CreateCSVListener> v_Iter       = this.createCSVListeners.iterator();
		boolean                     v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().createCSVProcess(i_Event);
		}
		
		return v_IsContinue;
	}

	
	
	/**
	 * 通知所有注册创建CSV文件完成之后的事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyCreateCSVAfterListeners(CreateCSVEvent i_Event)
	{
		Iterator<CreateCSVListener> v_Iter = this.createCSVListeners.iterator();

		while ( v_Iter.hasNext() ) 
		{
			v_Iter.next().createCSVAfter(i_Event);
		}
	}
	
	
	
	/**
	 * 注册创建Zip文件事件
	 * 
	 * @param e
	 */
	public void addCreateZipListener(CreateZipListener e)
	{
		if ( this.createZipListeners == null )
		{
			this.createZipListeners = new HashSet<CreateZipListener>();
		}
		
		this.createZipListeners.add(e);
	}
	
	
	
	/**
	 * 移除创建Zip文件事件
	 * 
	 * @param e
	 */
	public void removeCreateZipListener(CreateZipListener e)
	{
		if ( this.createZipListeners == null )
		{
			return;
		}
		
		this.createZipListeners.remove(e);
	}
	
	
	
	/**
	 * 触发创建Zip文件之前的事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCreateZipBeforeListener(CreateZipEvent i_Event)
	{
		if ( this.createZipListeners == null )
		{
			return true;
		}
		
		return notifyCreateZipBeforeListeners(i_Event);
	}
	
	
	
	/**
	 * 触发创建Zip文件事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireCreatingZipListener(CreateZipEvent i_Event)
	{
		if ( this.createZipListeners == null )
		{
			return true;
		}
		
		return notifyCreatingZipListeners(i_Event);
	}
	
	
	
	/**
	 * 触发创建Zip文件完成之后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireCreateZipAfterListener(CreateZipEvent i_Event)
	{
		if ( this.createZipListeners == null )
		{
			return;
		}
		
		notifyCreateZipAfterListeners(i_Event);
	}

	
	
	/**
	 * 通知所有注册创建Zip文件之前的事件监听的对象
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	private boolean notifyCreateZipBeforeListeners(CreateZipEvent i_Event)
	{
		Iterator<CreateZipListener> v_Iter       = this.createZipListeners.iterator();
		boolean                     v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().createZipBefore(i_Event);
		}
		
		return v_IsContinue;
	}
	
	
	
	/**
	 * 通知所有注册创建Zip文件事件监听的对象
	 * 
	 * @param i_Event
	 */
	private boolean notifyCreatingZipListeners(CreateZipEvent i_Event)
	{
		Iterator<CreateZipListener> v_Iter       = this.createZipListeners.iterator();
		boolean                     v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().createZipProcess(i_Event);
		}
		
		return v_IsContinue;
	}

	
	
	/**
	 * 通知所有注册创建Zip文件完成之后的事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyCreateZipAfterListeners(CreateZipEvent i_Event)
	{
		Iterator<CreateZipListener> v_Iter = this.createZipListeners.iterator();

		while ( v_Iter.hasNext() ) 
		{
			v_Iter.next().createZipAfter(i_Event);
		}
	}
	
	
	
	/**
	 * 注册解压缩Zip文件事件
	 * 
	 * @param e
	 */
	public void addUnCompressZipListener(UnCompressZipListener e)
	{
		if ( this.unCompressZipListeners == null )
		{
			this.unCompressZipListeners = new HashSet<UnCompressZipListener>();
		}
		
		this.unCompressZipListeners.add(e);
	}
	
	
	
	/**
	 * 移除解压缩Zip文件事件
	 * 
	 * @param e
	 */
	public void removeUnCompressZipListener(UnCompressZipListener e)
	{
		if ( this.unCompressZipListeners == null )
		{
			return;
		}
		
		this.unCompressZipListeners.remove(e);
	}
	
	
	
	/**
	 * 触发解压缩Zip文件之前的事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireUnCompressZipBeforeListener(UnCompressZipEvent i_Event)
	{
		if ( this.unCompressZipListeners == null )
		{
			return true;
		}
		
		return notifyUnCompressZipBeforeListeners(i_Event);
	}
	
	
	
	/**
	 * 触发解压缩Zip文件事件
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	protected boolean fireUnCompressZipListener(UnCompressZipEvent i_Event)
	{
		if ( this.unCompressZipListeners == null )
		{
			return true;
		}
		
		return notifyUnCompressZipListeners(i_Event);
	}
	
	
	
	/**
	 * 触发解压缩Zip文件完成之后的事件
	 * 
	 * @param i_Event
	 */
	protected void fireUnCompressZipAfterListener(UnCompressZipEvent i_Event)
	{
		if ( this.unCompressZipListeners == null )
		{
			return;
		}
		
		notifyUnCompressZipAfterListeners(i_Event);
	}

	
	
	/**
	 * 通知所有注册解压缩Zip文件之前的事件监听的对象
	 * 
	 * @param i_Event
	 * @return   返回值表示是否继续
	 */
	private boolean notifyUnCompressZipBeforeListeners(UnCompressZipEvent i_Event)
	{
		Iterator<UnCompressZipListener> v_Iter       = this.unCompressZipListeners.iterator();
		boolean                         v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().unCompressZipBefore(i_Event);
		}
		
		return v_IsContinue;
	}
	
	
	
	/**
	 * 通知所有注册解压缩Zip文件事件监听的对象
	 * 
	 * @param i_Event
	 */
	private boolean notifyUnCompressZipListeners(UnCompressZipEvent i_Event)
	{
		Iterator<UnCompressZipListener> v_Iter       = this.unCompressZipListeners.iterator();
		boolean                         v_IsContinue = true;

		while ( v_IsContinue && v_Iter.hasNext() ) 
		{
			v_IsContinue = v_Iter.next().unCompressZipProcess(i_Event);
		}
		
		return v_IsContinue;
	}

	
	
	/**
	 * 通知所有注册解压缩Zip文件完成之后的事件监听的对象
	 * 
	 * @param i_Event
	 */
	private void notifyUnCompressZipAfterListeners(UnCompressZipEvent i_Event)
	{
		Iterator<UnCompressZipListener> v_Iter = this.unCompressZipListeners.iterator();

		while ( v_Iter.hasNext() ) 
		{
			v_Iter.next().unCompressZipAfter(i_Event);
		}
	}
	
	
	
	/**
     * 缩放图片（限制最小宽度或最小高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MinWidth   最小宽度（小于0，表示不限制）
     * @param i_MinHeight  最小高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMin(String i_File ,boolean i_IsScale ,int i_MinWidth ,int i_MinHeight) throws IOException
    {
        if ( i_File.startsWith("file:") )
        {
            return resizeImageByMin(new URL(i_File) ,i_IsScale ,i_MinWidth ,i_MinHeight);
        }
        else
        {
            return resizeImageByMin(new File(i_File) ,i_IsScale ,i_MinWidth ,i_MinHeight);
        }
    }
    
    
    
    
    /**
     * 缩放图片（限制最小宽度或最小高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MinWidth   最小宽度（小于0，表示不限制）
     * @param i_MinHeight  最小高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMin(URL i_File ,boolean i_IsScale ,int i_MinWidth ,int i_MinHeight) throws IOException
    {
        return resizeImageByMin(ImageIO.read(i_File) ,i_IsScale ,i_MinWidth ,i_MinHeight);
    }
    
    
    
    /**
     * 缩放图片（限制最小宽度或最小高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MinWidth   最小宽度（小于0，表示不限制）
     * @param i_MinHeight  最小高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMin(File i_File ,boolean i_IsScale ,int i_MinWidth ,int i_MinHeight) throws IOException
    {
        return resizeImageByMin(ImageIO.read(i_File) ,i_IsScale ,i_MinWidth ,i_MinHeight);
    }
    
    
    
    /**
     * 缩放图片（限制最小宽度或最小高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MinWidth   最小宽度（小于0，表示不限制）
     * @param i_MinHeight  最小高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMin(BufferedImage i_Image ,boolean i_IsScale ,int i_MinWidth ,int i_MinHeight)
    {
        if ( i_MinWidth <= 0 && i_MinHeight <= 0 )
        {
            return i_Image;
        }
        
        int     v_NewWidth  = i_Image.getWidth();
        int     v_NewHeight = i_Image.getHeight();
        boolean v_IsResize  = false;
        
        // 计算高宽等比缩放的情况
        if ( i_IsScale )
        {
            double v_WidthZoomRate  = 1;
            double v_HeightZoomRate = 1;
            
            if ( i_MinWidth > 0 && v_NewWidth < i_MinWidth )
            {
                v_WidthZoomRate = Help.division(i_MinWidth ,v_NewWidth);
                v_IsResize      = true;
            }
            
            if ( i_MinHeight > 0 && v_NewHeight < i_MinHeight )
            {
                v_HeightZoomRate = Help.division(i_MinHeight ,v_NewHeight);
                v_IsResize       = true;
            }
            
            if ( v_IsResize )
            {
                double v_ZoomRate = Help.max(v_WidthZoomRate ,v_HeightZoomRate);
                v_NewWidth  = (int)Math.floor(Help.multiply(v_NewWidth  ,v_ZoomRate));
                v_NewHeight = (int)Math.floor(Help.multiply(v_NewHeight ,v_ZoomRate));
            }
        }
        // 计算非等比缩放的情况
        else
        {
            if ( i_MinWidth > 0 && v_NewWidth < i_MinWidth )
            {
                v_NewWidth = i_MinWidth;
                v_IsResize = true;
            }
            
            if ( i_MinHeight > 0 && v_NewHeight < i_MinHeight )
            {
                v_NewHeight = i_MinHeight;
                v_IsResize  = true;
            }
        }
        
        if ( v_IsResize )
        {
            return resizeImage(i_Image ,v_NewWidth ,v_NewHeight);
        }
        else
        {
            return i_Image;
        }
    }
	
	
	
	/**
     * 缩放图片（限制最大宽度或最大高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MaxWidth   最大宽度（小于0，表示不限制）
     * @param i_MaxHeight  最大高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMax(String i_File ,boolean i_IsScale ,int i_MaxWidth ,int i_MaxHeight) throws IOException
    {
        if ( i_File.startsWith("file:") )
        {
            return resizeImageByMax(new URL(i_File) ,i_IsScale ,i_MaxWidth ,i_MaxHeight);
        }
        else
        {
            return resizeImageByMax(new File(i_File) ,i_IsScale ,i_MaxWidth ,i_MaxHeight);
        }
    }
    
    
    
    
    /**
     * 缩放图片（限制最大宽度或最大高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MaxWidth   最大宽度（小于0，表示不限制）
     * @param i_MaxHeight  最大高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMax(URL i_File ,boolean i_IsScale ,int i_MaxWidth ,int i_MaxHeight) throws IOException
    {
        return resizeImageByMax(ImageIO.read(i_File) ,i_IsScale ,i_MaxWidth ,i_MaxHeight);
    }
    
    
    
    /**
     * 缩放图片（限制最大宽度或最大高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MaxWidth   最大宽度（小于0，表示不限制）
     * @param i_MaxHeight  最大高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMax(File i_File ,boolean i_IsScale ,int i_MaxWidth ,int i_MaxHeight) throws IOException
    {
        return resizeImageByMax(ImageIO.read(i_File) ,i_IsScale ,i_MaxWidth ,i_MaxHeight);
    }
    
    
    
    /**
     * 缩放图片（限制最大宽度或最大高度）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_MaxWidth   最大宽度（小于0，表示不限制）
     * @param i_MaxHeight  最大高度（小于0，表示不限制）
     * @param i_IsScale    当图片被缩小时，是否保持高宽等比缩放
     * @return
     */
    public BufferedImage resizeImageByMax(BufferedImage i_Image ,boolean i_IsScale ,int i_MaxWidth ,int i_MaxHeight)
    {
        if ( i_MaxWidth <= 0 && i_MaxHeight <= 0 )
        {
            return i_Image;
        }
        
        int     v_NewWidth  = i_Image.getWidth();
        int     v_NewHeight = i_Image.getHeight();
        boolean v_IsResize  = false;
        
        // 计算高宽等比缩放的情况
        if ( i_IsScale )
        {
            double v_WidthZoomRate  = 1;
            double v_HeightZoomRate = 1;
            
            if ( i_MaxWidth > 0 && v_NewWidth > i_MaxWidth )
            {
                v_WidthZoomRate = Help.division(i_MaxWidth ,v_NewWidth);
                v_IsResize      = true;
            }
            
            if ( i_MaxHeight > 0 && v_NewHeight > i_MaxHeight )
            {
                v_HeightZoomRate = Help.division(i_MaxHeight ,v_NewHeight);
                v_IsResize       = true;
            }
            
            if ( v_IsResize )
            {
                double v_ZoomRate = Help.min(v_WidthZoomRate ,v_HeightZoomRate);
                v_NewWidth  = (int)Math.floor(Help.multiply(v_NewWidth  ,v_ZoomRate));
                v_NewHeight = (int)Math.floor(Help.multiply(v_NewHeight ,v_ZoomRate));
            }
        }
        // 计算非等比缩放的情况
        else
        {
            if ( i_MaxWidth > 0 && v_NewWidth > i_MaxWidth )
            {
                v_NewWidth = i_MaxWidth;
                v_IsResize = true;
            }
            
            if ( i_MaxHeight > 0 && v_NewHeight > i_MaxHeight )
            {
                v_NewHeight = i_MaxHeight;
                v_IsResize  = true;
            }
        }
        
        if ( v_IsResize )
        {
            return resizeImage(i_Image ,v_NewWidth ,v_NewHeight);
        }
        else
        {
            return i_Image;
        }
    }
	
	
	
	
	/**
     * 缩放图片
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_NewWidth   新的宽度
     * @param i_NewHeight  新的高度
     * @return
     */
    public BufferedImage resizeImage(String i_File ,int i_NewWidth ,int i_NewHeight) throws IOException
    {
        if ( i_File.startsWith("file:") )
        {
            return resizeImage(new URL(i_File) ,i_NewWidth ,i_NewHeight);
        }
        else
        {
            return resizeImage(new File(i_File) ,i_NewWidth ,i_NewHeight);
        }
    }
	
	
	
	
	/**
     * 缩放图片
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_NewWidth   新的宽度
     * @param i_NewHeight  新的高度
     * @return
     */
	public BufferedImage resizeImage(URL i_File ,int i_NewWidth ,int i_NewHeight) throws IOException
	{
	    return resizeImage(ImageIO.read(i_File) ,i_NewWidth ,i_NewHeight);
	}
	
	
	
	/**
     * 缩放图片
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_NewWidth   新的宽度
     * @param i_NewHeight  新的高度
     * @return
     */
	public BufferedImage resizeImage(File i_File ,int i_NewWidth ,int i_NewHeight) throws IOException
    {
        return resizeImage(ImageIO.read(i_File) ,i_NewWidth ,i_NewHeight);
    }
	
	
	
	/**
     * 缩放图片
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-01-10
     * @version     v1.0
     *
     * @param i_Image
     * @param i_NewWidth   新的宽度
     * @param i_NewHeight  新的高度
     * @return
     */
    public BufferedImage resizeImage(BufferedImage i_Image ,int i_NewWidth ,int i_NewHeight)
    {
        Image         v_ScaleImage = i_Image.getScaledInstance(i_NewWidth ,i_NewHeight ,Image.SCALE_SMOOTH);
        BufferedImage v_NewImage   = new BufferedImage(i_NewWidth ,i_NewHeight, i_Image.getType());
        Graphics2D    v_Graphics   = v_NewImage.createGraphics();
        
        // 从原图上取颜色绘制新图
        v_Graphics.drawImage(v_ScaleImage ,0 ,0 ,i_NewWidth ,i_NewHeight ,null);
        v_Graphics.dispose();
        
        return v_NewImage;
    }
	
	
	
	/**
	 * 删除指定目录下过期的文件
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2017-06-21
	 * @version     v1.0
	 *
	 * @param i_Folder       被删除文件所在目录
	 * @param i_ExpireDays   过期天数（即，只保留多少天的文件）
	 * @param i_IsDelChilds  是否递归的删除所有子目录中的过期文件
	 */
	public void delFiles(String i_Folder ,int i_ExpireDays ,boolean i_IsDelChilds)
	{
	    this.delFiles(new File(i_Folder) ,i_ExpireDays ,i_IsDelChilds);
	}
	
	
	
	/**
     * 删除指定目录下过期的文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-21
     * @version     v1.0
     *
     * @param i_Folder       被删除文件所在目录
     * @param i_ExpireDays   过期天数（即，只保留多少天的文件）
     * @param i_IsDelChilds  是否递归的删除所有子目录中的过期文件
     */
	public void delFiles(File i_Folder ,int i_ExpireDays ,boolean i_IsDelChilds)
	{
	    Date v_ExpireTime = Date.getNowTime().getDate(Math.abs(i_ExpireDays)  * -1);
	    
	    this.delFiles(i_Folder ,v_ExpireTime ,i_IsDelChilds);
	}
	
	
	
	/**
     * 删除指定目录下过期的文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-21
     * @version     v1.0
     *
     * @param i_Folder       被删除文件所在目录
     * @param i_ExpireTime   过期时间。小于此时间点的文件将被删除
     * @param i_IsDelChilds  是否递归的删除所有子目录中的过期文件
     */
	public void delFiles(File i_Folder ,Date i_ExpireTime ,boolean i_IsDelChilds)
	{
	    if ( i_Folder == null )
	    {
	        return;
	    }
	    
	    if ( !i_Folder.exists() && !i_Folder.isDirectory() )
	    {
	        return;
	    }
	    
	    File [] v_Files = i_Folder.listFiles();
	    if ( Help.isNull(v_Files) )
	    {
	        return;
	    }
	    
	    for (File v_File : v_Files)
        {
            if ( v_File.isFile() )
            {
                if ( v_File.lastModified() < i_ExpireTime.getTime() )
                {
                    try
                    {
                        v_File.delete();
                    }
                    catch (Exception exce)
                    {
                        exce.printStackTrace();
                    }
                }
            }
            else if ( v_File.isDirectory() && i_IsDelChilds )
            {
                delFiles(v_File ,i_ExpireTime ,i_IsDelChilds);
                v_File.delete();
            }
        }
	}
	
	
	
	/**
     * 递归的计算目录所有文件及子目录文件的合计大小。
     * 
     * 此方法相对简单，未实现多线程等计算功能。
     * 
     * 适用于递归层级及文件不是十分多的情况。
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-03-14
     * @version     v1.0
     *
     * @param i_Folder  父目录
     * @return          返回合计大小
     */
    public long calcSize(File i_Folder)
    {
        long v_Size = 0L;
        
        if ( i_Folder == null )
        {
            return v_Size;
        }
        
        if ( !i_Folder.exists() && !i_Folder.isDirectory() )
        {
            return v_Size;
        }
        
        File [] v_Files = i_Folder.listFiles();
        if ( Help.isNull(v_Files) )
        {
            return v_Size;
        }
        
        for (File v_File : v_Files)
        {
            if ( v_File.isFile() )
            {
                v_Size += v_File.length();
            }
            else if ( v_File.isDirectory() )
            {
                v_Size += calcSize(v_File);
            }
        }
        
        return v_Size;
    }
    
    
    
    /**
     * 递归的获取目录所有文件及子目录中的文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-03-19
     * @version     v1.0
     *
     * @param i_Folder         父目录
     * @param i_HaveDirectory  是否包括目录
     * @return
     */
    public List<File> getFiles(String i_Folder ,boolean i_HaveDirectory)
    {
        return this.getFiles(new File(i_Folder) ,i_HaveDirectory);
    }
	
	
	
	/**
	 * 递归的获取目录所有文件及子目录中的文件
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2018-03-13
	 * @version     v1.0
	 *
	 * @param i_Folder         父目录
	 * @param i_HaveDirectory  是否包括目录
	 * @return
	 */
	public List<File> getFiles(File i_Folder ,boolean i_HaveDirectory)
    {
	    List<File> v_Ret = new ArrayList<File>();
	    
        if ( i_Folder == null )
        {
            return v_Ret;
        }
        
        if ( !i_Folder.exists() && !i_Folder.isDirectory() )
        {
            return v_Ret;
        }
        
        File [] v_Files = i_Folder.listFiles();
        if ( Help.isNull(v_Files) )
        {
            return v_Ret;
        }
        
        for (File v_File : v_Files)
        {
            if ( v_File.isFile() )
            {
                v_Ret.add(v_File);
            }
            else if ( v_File.isDirectory() )
            {
                if ( i_HaveDirectory )
                {
                    v_Ret.add(v_File);
                }
                
                v_Ret.addAll(getFiles(v_File ,i_HaveDirectory));
            }
        }
        
        return v_Ret;
    }
	
	
	
	/**
     * 读取外部Jar包中的文件内容
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-08
     * @version     v1.0
     *
     * @param i_Jar           Jar包文件的对象。
     *                            方法内部不会做关闭动作JarFile.close()，请在外部自行关。
     *                            不再内部自动关闭的好处是：外部方法可以多次调用此方法读取不同的文件内容。
     * @param i_FileName      要读取Jar包里的文件的名称。
     *                            1. 可以是全路径的(/为路径分隔符)。
     *                            2. 也可以只是文件名称 或 部分路径加文件名称
     * @return                为匹配到文件时，返回null
     * @throws IOException    读取异常时，抛异常
     */
    public String getContent(JarFile i_Jar ,String i_FileName) throws IOException
    {
        return this.getContent(i_Jar ,i_FileName ,this.charEncoding ,false);
    }
    
    
    
    /**
     * 读取外部Jar包中的文件内容
     * 
     * @author      ZhengWei(HY)
     * @createDate  2019-02-08
     * @version     v1.0
     *
     * @param i_Jar           Jar包文件的对象。
     *                            方法内部不会做关闭动作JarFile.close()，请在外部自行关。
     *                            不再内部自动关闭的好处是：外部方法可以多次调用此方法读取不同的文件内容。
     * @param i_FileName      要读取Jar包里的文件的名称。
     *                            1. 可以是全路径的(/为路径分隔符)。
     *                            2. 也可以只是文件名称 或 部分路径加文件名称
     * @param i_CharEncoding  文件的编码
     * @return                为匹配到文件时，返回null
     * @throws IOException    读取异常时，抛异常
     */
    public String getContent(JarFile i_Jar ,String i_FileName ,String i_CharEncoding) throws IOException
    {
        return this.getContent(i_Jar ,i_FileName ,i_CharEncoding ,false);
    }
	
	
	
	/**
	 * 读取外部Jar包中的文件内容
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2019-02-08
	 * @version     v1.0
	 *
	 * @param i_Jar           Jar包文件的对象。
	 *                            方法内部不会做关闭动作JarFile.close()，请在外部自行关。
	 *                            不再内部自动关闭的好处是：外部方法可以多次调用此方法读取不同的文件内容。
	 * @param i_FileName      要读取Jar包里的文件的名称。
	 *                            1. 可以是全路径的(/为路径分隔符)。
	 *                            2. 也可以只是文件名称 或 部分路径加文件名称
	 * @param i_CharEncoding  文件的编码
     * @param i_HaveNewLine   生成的文件内容是否包含 “回车换行” 符
	 * @return                为匹配到文件时，返回null
	 * @throws IOException    读取异常时，抛异常
	 */
	public String getContent(JarFile i_Jar ,String i_FileName ,String i_CharEncoding ,boolean i_HaveNewLine) throws IOException
	{
	    String                v_Content = null;
	    Enumeration<JarEntry> v_Entries = i_Jar.entries();
	    
        while ( v_Entries.hasMoreElements() )
        {
            JarEntry v_JarEntry = v_Entries.nextElement();
            String   v_Name     = v_JarEntry.getName();
            
            if ( v_Name.endsWith(i_FileName) )
            {
                v_Content = this.getContent(i_Jar.getInputStream(v_JarEntry) ,i_CharEncoding ,i_HaveNewLine);
                break;
            }
        }
        
	    return v_Content;
	}
	
	
	
	/**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * @param i_FileFullName  文件的全路径名称
     * @return
     * @throws ClassNotFoundException 
     */
    public String getContent(String i_FileFullName) throws IOException, ClassNotFoundException
    {
        return this.getContent(i_FileFullName ,this.charEncoding);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * 1. 如果i_FileName以点"."前头：表示获取当前目录下的文件。
     * 2. 如果i_FileName以CLASSS前头：并且格式为 CLASS:org.hy.common.xxx:FileName
     *    表示获取org.hy.common.xxx类下的资源FileName文件信息。
     * 
     * @param i_FileName      文件的名称(可动态的、可相对的)
     * @param i_CharEncoding  文件的编码
     * @return
     * @throws ClassNotFoundException 
     */
    public String getContent(String i_FileName ,String i_CharEncoding) throws IOException, ClassNotFoundException
    {
        return getContent(i_FileName ,i_CharEncoding ,false);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * 1. 如果i_FileName以点"."前头：表示获取当前目录下的文件。
     * 2. 如果i_FileName以CLASSS前头：并且格式为 CLASS:org.hy.common.xxx:FileName
     *    表示获取org.hy.common.xxx类下的资源FileName文件信息。
     * 
     * @param i_FileName      文件的名称(可动态的、可相对的)
     * @param i_CharEncoding  文件的编码
     * @param i_HaveNewLine   生成的文件内容是否包含 “回车换行” 符
     * @return
     * @throws ClassNotFoundException 
     */
    public String getContent(String i_FileName ,String i_CharEncoding ,boolean i_HaveNewLine) throws IOException, ClassNotFoundException
    {
        if ( Help.isNull(i_FileName) )
        {
            throw new NullPointerException("File name is null.");
        }
        
        String v_FileFullName = i_FileName.trim();
        
        if ( v_FileFullName.startsWith(".") )
        {
            v_FileFullName = Help.getSysCurrentPath() + v_FileFullName.substring(1);
        }
        else if ( "CLASS:".equals(v_FileFullName.substring(0 ,6).toUpperCase()) )
        {
            String [] v_FileInfoArr = v_FileFullName.split(":");
            
            if ( v_FileInfoArr.length >= 3 )
            {
                String v_ClassName  = v_FileInfoArr[1];
                v_FileFullName      = v_FileInfoArr[2];
                Class<?> v_ClassObj = Help.forName(v_ClassName);
                
                return getContent(v_ClassObj.getResourceAsStream(v_FileFullName) ,i_CharEncoding ,i_HaveNewLine);
            }
        }
        
        return getContent(new File(v_FileFullName) ,i_CharEncoding ,i_HaveNewLine);
    }
	
	
	
	/**
	 * 获取文件内容。
	 * 
	 * 主要针对小文件。文件内容为正常文字的文件。
	 * 
	 * @param i_SourceFile    文件对象
	 */
	public String getContent(File i_SourceFile) throws IOException
	{
		return this.getContent(i_SourceFile ,this.charEncoding);
	}
	
	
	
	/**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * @param i_SourceFile    文件对象
     * @param i_CharEncoding  文件的编码
     */
    public String getContent(File i_SourceFile ,String i_CharEncoding) throws IOException
    {
        return this.getContent(i_SourceFile ,i_CharEncoding ,false);
    }
	
	
	
	/**
	 * 获取文件内容。
	 * 
	 * 主要针对小文件。文件内容为正常文字的文件。
	 * 
	 * @param i_SourceFile    文件对象
	 * @param i_CharEncoding  文件的编码
	 * @param i_HaveNewLine   生成的文件内容是否包含 “回车换行” 符
	 */
	public String getContent(File i_SourceFile ,String i_CharEncoding ,boolean i_HaveNewLine) throws IOException
	{
        if ( i_SourceFile == null )
        {
            throw new NullPointerException("Source file is null.");
        }
        
        if ( !i_SourceFile.exists() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] is not exists.");
        }
        
        if ( !i_SourceFile.isFile() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] is not file.");
        }
        
        if ( !i_SourceFile.canRead() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] can not read.");
        }
        
        
        FileInputStream v_InputStream = null;
        String          v_Ret         = "";
        
        try
        {
            // v_InputStream 已由 this.getContent(InputStream ,...) 方法内部关闭了
            v_InputStream = new FileInputStream(i_SourceFile);
            v_Ret = this.getContent(v_InputStream ,i_CharEncoding ,i_HaveNewLine);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
	}
	
	
	
	/**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * @param i_FileURL       文件的URL路径
     * @param i_CharEncoding  文件的编码
     * @return
     */
    public String getContent(URL i_FileURL) throws IOException
    {
        return this.getContent(i_FileURL ,this.charEncoding);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * @param i_FileURL       文件的URL路径
     * @param i_CharEncoding  文件的编码
     * @return
     */
    public String getContent(URL i_FileURL ,String i_CharEncoding) throws IOException
    {
        return this.getContent(i_FileURL ,i_CharEncoding ,false);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * @param i_FileURL       文件的URL路径
     * @param i_CharEncoding  文件的编码
     * @param i_HaveNewLine   生成的文件内容是否包含 “回车换行” 符
     * @return
     */
    public String getContent(URL i_FileURL ,String i_CharEncoding ,boolean i_HaveNewLine) throws IOException
    {
        if ( i_FileURL == null )
        {
            throw new NullPointerException("File rul is null.");
        }
        
        
        File v_SourceFile = null;
        try
        {
            v_SourceFile = new File(i_FileURL.toURI());
        }
        catch (Exception exce)
        {
            v_SourceFile = null;
            throw new IOException(exce.getMessage());
        }
        if ( !v_SourceFile.exists() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] is not exists.");
        }
        
        if ( !v_SourceFile.isFile() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] is not file.");
        }
        
        if ( !v_SourceFile.canRead() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] can not read.");
        }
        
        
        FileInputStream v_InputStream = null;
        String          v_Ret         = "";
        
        try
        {
            // v_InputStream 已由 this.getContent(InputStream ,...) 方法内部关闭了
            v_InputStream = new FileInputStream(v_SourceFile);
            v_Ret = this.getContent(v_InputStream ,i_CharEncoding ,i_HaveNewLine);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * 执行完成(或异常)后会关闭i_SourceInput输入流
     * 
     * @param i_SourceInput   文件的输入流
     */
    public String getContent(InputStream i_SourceInput) throws IOException
    {
        return getContent(i_SourceInput ,this.charEncoding);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * 执行完成(或异常)后会关闭i_SourceInput输入流
     * 
     * @param i_SourceInput   文件的输入流
     * @param i_CharEncoding  文件的编码
     */
    public String getContent(InputStream i_SourceInput ,String i_CharEncoding) throws IOException
    {
        return getContent(i_SourceInput ,i_CharEncoding ,false);
    }
    
    
    
    /**
     * 获取文件内容。
     * 
     * 主要针对小文件。文件内容为正常文字的文件。
     * 
     * 执行完成(或异常)后会关闭i_SourceInput输入流
     * 
     * 2018-03-15  添加：事件处理机制
     *             添加：是否返回文件内容的功能 -- isReturnContent
     * 
     * @param i_SourceInput   文件的输入流
     * @param i_CharEncoding  文件的编码
     * @param i_HaveNewLine   生成的文件内容是否包含 “回车换行” 符
     */
    public String getContent(InputStream i_SourceInput ,String i_CharEncoding ,boolean i_HaveNewLine) throws IOException
    {
        if ( i_SourceInput == null )
        {
            throw new NullPointerException("Source inputstream is null.");
        }
        
        InputStreamReader    v_ReaderIO   = null;
        BufferedReader       v_Reader     = null;
        StringBuilder        v_Buffer     = new StringBuilder();
        String               v_LineData   = null;
        long                 v_ReadIndex  = 0;
        DefaultFileReadEvent v_Event      = new DefaultFileReadEvent(this);
        boolean              v_IsContinue = true;
        
        try
        {
            v_ReaderIO   = new InputStreamReader(i_SourceInput ,i_CharEncoding);
            v_Reader     = new BufferedReader(v_ReaderIO);
            v_IsContinue = this.fireReadBeforeListener(v_Event);
            
            if ( i_HaveNewLine )
            {
                if ( this.isReturnContent )
                {
                    while ( v_IsContinue && (v_LineData = v_Reader.readLine()) != null )
                    {
                        v_Buffer.append(v_LineData).append("\r\n");
                        
                        v_ReadIndex += v_LineData.length();
                        v_Event.setDataString(v_LineData + "\r\n");
                        v_Event.setCompleteSize(v_ReadIndex);
                        v_IsContinue = this.fireReadingListener(v_Event);
                    }
                }
                else
                {
                    while ( v_IsContinue && (v_LineData = v_Reader.readLine()) != null )
                    {
                        v_ReadIndex += v_LineData.length();
                        v_Event.setDataString(v_LineData + "\r\n");
                        v_Event.setCompleteSize(v_ReadIndex);
                        v_IsContinue = this.fireReadingListener(v_Event);
                    }
                }
            }
            else
            {
                if ( this.isReturnContent )
                {
                    while ( v_IsContinue && (v_LineData = v_Reader.readLine()) != null )
                    {
                        v_Buffer.append(v_LineData);
                        
                        v_ReadIndex += v_LineData.length();
                        v_Event.setDataString(v_LineData);
                        v_Event.setCompleteSize(v_ReadIndex);
                        v_IsContinue = this.fireReadingListener(v_Event);
                    }
                }
                else
                {
                    while ( v_IsContinue && (v_LineData = v_Reader.readLine()) != null )
                    {
                        v_ReadIndex += v_LineData.length();
                        v_Event.setDataString(v_LineData);
                        v_Event.setCompleteSize(v_ReadIndex);
                        v_IsContinue = this.fireReadingListener(v_Event);
                    }
                }
            }
            
            v_Event.setSucceedFinish();
        }
        catch (Exception exce)
        {
            v_Event.setEndTime();
            throw new IOException(exce.getMessage());
        }
        finally
        {
            if ( v_Reader != null )
            {
                try
                {
                    v_Reader.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            v_Reader = null;
            
            
            if ( v_ReaderIO != null )
            {
                try
                {
                    v_ReaderIO.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            v_ReaderIO = null;
            
            
            if ( i_SourceInput != null )
            {
                try
                {
                    i_SourceInput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            i_SourceInput = null;
            
            this.fireReadAfterListener(v_Event);
        }
        
        return v_Buffer.toString();
    }
    
    
    
    /**
     * 获取文件的二进制内容。
     * 
     * 主要针对小文件。文件内容为二进制的文件。
     * 
     * 1. 如果i_FileName以点"."前头：表示获取当前目录下的文件。
     * 2. 如果i_FileName以CLASSS前头：并且格式为 CLASS:org.hy.common.xxx:FileName
     *    表示获取org.hy.common.xxx类下的资源FileName文件信息。
     * 
     * @param i_FileName      文件的名称(可动态的、可相对的)
     * @return
     * @throws ClassNotFoundException 
     */
    public byte [] getContentByte(String i_FileName) throws IOException, ClassNotFoundException
    {
        if ( Help.isNull(i_FileName) )
        {
            throw new NullPointerException("File name is null.");
        }
        
        String v_FileFullName = i_FileName.trim();
        
        if ( v_FileFullName.startsWith(".") )
        {
            v_FileFullName = Help.getSysCurrentPath() + v_FileFullName.substring(1);
        }
        else if ( "CLASS:".equals(v_FileFullName.substring(0 ,6).toUpperCase()) )
        {
            String [] v_FileInfoArr = v_FileFullName.split(":");
            
            if ( v_FileInfoArr.length >= 3 )
            {
                String v_ClassName  = v_FileInfoArr[1];
                v_FileFullName      = v_FileInfoArr[2];
                Class<?> v_ClassObj = Help.forName(v_ClassName);
                
                return getContentByte(v_ClassObj.getResourceAsStream(v_FileFullName));
            }
        }
        
        return getContentByte(new File(v_FileFullName));
    }
    
    
    
    /**
     * 获取文件的二进制内容。
     * 
     * 主要针对小文件。文件内容为二进制的文件。
     * 
     * @param i_SourceFile    文件对象
     */
    public byte [] getContentByte(File i_SourceFile) throws IOException
    {
        if ( i_SourceFile == null )
        {
            throw new NullPointerException("Source file is null.");
        }
        
        if ( !i_SourceFile.exists() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] is not exists.");
        }
        
        if ( !i_SourceFile.isFile() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] is not file.");
        }
        
        if ( !i_SourceFile.canRead() )
        {
            throw new VerifyError("Source file[" + i_SourceFile.toString() + "] can not read.");
        }
        
        
        FileInputStream v_InputStream = null;
        byte []         v_Ret         = new byte [0];
        
        try
        {
            // v_InputStream 已由 this.getContentByte(InputStream) 方法内部关闭了
            v_InputStream = new FileInputStream(i_SourceFile);
            v_Ret = this.getContentByte(v_InputStream);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 获取文件的二进制内容。
     * 
     * 主要针对小文件。文件内容为二进制的文件。
     * 
     * @param i_FileURL       文件的URL路径
     * @return
     */
    public byte [] getContentByte(URL i_FileURL) throws IOException
    {
        if ( i_FileURL == null )
        {
            throw new NullPointerException("File rul is null.");
        }
        
        
        File v_SourceFile = null;
        try
        {
            v_SourceFile = new File(i_FileURL.toURI());
        }
        catch (Exception exce)
        {
            v_SourceFile = null;
            throw new IOException(exce.getMessage());
        }
        if ( !v_SourceFile.exists() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] is not exists.");
        }
        
        if ( !v_SourceFile.isFile() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] is not file.");
        }
        
        if ( !v_SourceFile.canRead() )
        {
            throw new VerifyError("File[" + v_SourceFile.toString() + "] can not read.");
        }
        
        
        FileInputStream v_InputStream = null;
        byte []         v_Ret         = new byte [0];
        
        try
        {
            // v_InputStream 已由 this.getContentByte(InputStream) 方法内部关闭了
            v_InputStream = new FileInputStream(v_SourceFile);
            v_Ret = this.getContentByte(v_InputStream);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 获取文件的二进制内容。
     * 
     * 主要针对小文件。文件内容为二进制的文件。
     * 
     * 执行完成(或异常)后会关闭i_SourceInput输入流
     * 
     * 2018-03-15  添加：事件处理机制
     *             添加：是否返回文件内容的功能 -- isReturnContent
     * 
     * @param i_SourceInput   文件的输入流
     */
    public byte [] getContentByte(InputStream i_SourceInput) throws IOException
    {
        if ( i_SourceInput == null )
        {
            throw new NullPointerException("Source inputstream is null.");
        }
        
        byte []               v_ReadBuffer = new byte[this.bufferSize];
        int                   v_ReadSize   = 0;
        long                  v_ReadIndex  = 0;
        ByteArrayOutputStream v_Output     = new ByteArrayOutputStream();
        DefaultFileReadEvent  v_Event      = new DefaultFileReadEvent(this);
        boolean               v_IsContinue = true;
        
        try
        {
            v_IsContinue = this.fireReadBeforeListener(v_Event);
            
            if ( isReturnContent )
            {
                while ( v_IsContinue && (v_ReadSize = i_SourceInput.read(v_ReadBuffer ,0 ,this.bufferSize)) > 0 )
                {
                    v_Output.write(v_ReadBuffer ,0 ,v_ReadSize);
                    
                    v_ReadIndex += v_ReadSize;
                    v_Event.setDataByte(ByteHelp.substr(v_ReadBuffer ,0 ,v_ReadSize));
                    v_Event.setCompleteSize(v_ReadIndex);
                    v_IsContinue = this.fireReadingListener(v_Event);
                }
            }
            else
            {
                while ( v_IsContinue && (v_ReadSize = i_SourceInput.read(v_ReadBuffer ,0 ,this.bufferSize)) > 0 )
                {
                    v_ReadIndex += v_ReadSize;
                    v_Event.setDataByte(ByteHelp.substr(v_ReadBuffer ,0 ,v_ReadSize));
                    v_Event.setCompleteSize(v_ReadIndex);
                    v_IsContinue = this.fireReadingListener(v_Event);
                }
            }
            
            v_Event.setSucceedFinish();
        }
        catch (Exception exce)
        {
            v_Event.setEndTime();
            throw new IOException(exce.getMessage());
        }
        finally
        {
            if ( i_SourceInput != null )
            {
                try
                {
                    i_SourceInput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
            i_SourceInput = null;
            
            this.fireReadAfterListener(v_Event);
        }
        
        return v_Output.toByteArray();
    }
	
    
    
    /**
     * 模仿window的xcopy命令的功能 (过滤方式：排除在外的  ，即：不想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolderFullName  源目录
     * @param i_TargetFolderFullName  目标目录
     * @param i_ModifiedTime          文件的最后修改时间
     * @param i_Exclusion             过滤方式：排除在外的  ，即：不想要哪些
     * @return                        返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopy(String i_SourceFolderFullName ,String i_TargetFolderFullName ,Long i_ModifiedTime ,String i_Exclusion) throws IOException
    {
        return this.xcopy(i_SourceFolderFullName ,i_TargetFolderFullName ,i_ModifiedTime ,i_Exclusion ,null);
    }
    
    
    
    /**
     * 模仿window的xcopy命令的功能 (过滤方式：排除在外的  ，即：不想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolderFullName  源目录
     * @param i_TargetFolderFullName  目标目录
     * @param i_ModifiedTime          文件的最后修改时间
     * @param i_Exclusion             过滤方式：排除在外的  ，即：不想要哪些
     * @return                        返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopyExclusion(String i_SourceFolderFullName ,String i_TargetFolderFullName ,Long i_ModifiedTime ,String i_Exclusion) throws IOException
    {
        return this.xcopy(i_SourceFolderFullName ,i_TargetFolderFullName ,i_ModifiedTime ,i_Exclusion ,null);
    }
    
    
    
    /**
     * 模仿window的xcopy命令的功能 (过滤方式：在此范围内的，即：想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolderFullName  源目录
     * @param i_TargetFolderFullName  目标目录
     * @param i_ModifiedTime          文件的最后修改时间
     * @param i_Accept                过滤方式：在此范围内的，即：想要哪些
     * @return                        返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopyAccept(String i_SourceFolderFullName ,String i_TargetFolderFullName ,Long i_ModifiedTime ,String i_Accept) throws IOException
    {
        return this.xcopy(i_SourceFolderFullName ,i_TargetFolderFullName ,i_ModifiedTime ,null ,i_Accept);
    }
    
    
    
    /**
     * 模仿window的xcopy命令的功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolderFullName  源目录的全路径
     * @param i_TargetFolderFullName  目标目录的全路径
     * @param i_ModifiedTime          文件的最后修改时间
     * @return                        返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopy(String i_SourceFolderFullName ,String i_TargetFolderFullName ,long i_ModifiedTime) throws IOException
    {
        return this.xcopy(i_SourceFolderFullName ,i_TargetFolderFullName ,i_ModifiedTime ,null ,null);
    }
	
	
	
	/**
	 * 模仿window的xcopy命令的功能
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2015-01-21
	 * @version     v1.0
	 *
	 * @param i_SourceFolderFullName  源目录的全路径
	 * @param i_TargetFolderFullName  目标目录的全路径
	 * @param i_ModifiedTime          文件的最后修改时间
     * @param i_Exclusion             过滤方式：排除在外的  ，即：不想要哪些
     * @param i_Accept                过滤方式：在此范围内的，即：想要哪些
     * @return                        返回实际拷贝的文件数量
	 * @throws IOException
	 */
	public int xcopy(String i_SourceFolderFullName ,String i_TargetFolderFullName ,Long i_ModifiedTime ,String i_Exclusion ,String i_Accept) throws IOException
	{
	    if ( Help.isNull(i_SourceFolderFullName) )
        {
            throw new NullPointerException("Source full name is null.");
        }
        
        if ( Help.isNull(i_TargetFolderFullName) )
        {
            throw new NullPointerException("Target full name is null.");
        }
        
        
        Pattern v_Exclusion = null;
        Pattern v_Accept    = null;
        
        if ( !Help.isNull(i_Exclusion) )
        {
            v_Exclusion = Pattern.compile(i_Exclusion);
        }
        
        if ( !Help.isNull(i_Accept) )
        {
            v_Accept = Pattern.compile(i_Accept);
        }
        
        return this.xcopy(new File(i_SourceFolderFullName) ,new File(i_TargetFolderFullName) ,i_ModifiedTime ,v_Exclusion ,v_Accept);
	}
	
	
	
	/**
     * 模仿window的xcopy命令的功能 (过滤方式：排除在外的  ，即：不想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolder  源目录
     * @param i_TargetFolder  目标目录
     * @param i_ModifiedTime  文件的最后修改时间
     * @param i_Exclusion     过滤方式：排除在外的  ，即：不想要哪些
     * @return                返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopy(File i_SourceFolder ,File i_TargetFolder ,Long i_ModifiedTime ,Pattern i_Exclusion) throws IOException
    {
        return this.xcopy(i_SourceFolder ,i_TargetFolder ,i_ModifiedTime ,i_Exclusion ,null);
    }
	
	
	
	/**
     * 模仿window的xcopy命令的功能 (过滤方式：排除在外的  ，即：不想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolder  源目录
     * @param i_TargetFolder  目标目录
     * @param i_ModifiedTime  文件的最后修改时间
     * @param i_Exclusion     过滤方式：排除在外的  ，即：不想要哪些
     * @return                返回实际拷贝的文件数量
     * @throws IOException
     */
	public int xcopyExclusion(File i_SourceFolder ,File i_TargetFolder ,Long i_ModifiedTime ,Pattern i_Exclusion) throws IOException
    {
        return this.xcopy(i_SourceFolder ,i_TargetFolder ,i_ModifiedTime ,i_Exclusion ,null);
    }
	
	
	
	/**
     * 模仿window的xcopy命令的功能 (过滤方式：在此范围内的，即：想要哪些)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolder  源目录
     * @param i_TargetFolder  目标目录
     * @param i_ModifiedTime  文件的最后修改时间
     * @param i_Accept        过滤方式：在此范围内的，即：想要哪些
     * @return                返回实际拷贝的文件数量
     * @throws IOException
     */
	public int xcopyAccept(File i_SourceFolder ,File i_TargetFolder ,Long i_ModifiedTime ,Pattern i_Accept) throws IOException
    {
        return this.xcopy(i_SourceFolder ,i_TargetFolder ,i_ModifiedTime ,null ,i_Accept);
    }
	
	
	
	/**
     * 模仿window的xcopy命令的功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2015-01-21
     * @version     v1.0
     *
     * @param i_SourceFolder  源目录
     * @param i_TargetFolder  目标目录
     * @param i_ModifiedTime  文件的最后修改时间
     * @param i_Exclusion     过滤方式：排除在外的  ，即：不想要哪些
     * @param i_Accept        过滤方式：在此范围内的，即：想要哪些
     * @return                返回实际拷贝的文件数量
     * @throws IOException
     */
    public int xcopy(File i_SourceFolder ,File i_TargetFolder ,Long i_ModifiedTime) throws IOException
    {
        return this.xcopy(i_SourceFolder ,i_TargetFolder ,i_ModifiedTime ,null ,null);
    }
	
	
	
	/**
	 * 模仿window的xcopy命令的功能
	 * 
     * 1. 递归操作
     * 2. 生成目标文件夹的结构(目录层次)与源文件夹的结构一样
     * 3. 不创建多余的空目录
     * 4. 只拷贝大于 i_ModifiedTime 时间的文件。当 i_ModifiedTime == null 时就不再用时间过滤了
     * 5. 不要求目标文件夹一定在存在
     * 6. 目录文件已存时，可通过 this.isOverWrite 属性控制
	 * 
	 * @author      ZhengWei(HY)
	 * @createDate  2015-01-21
	 * @version     v1.0
	 *
	 * @param i_SourceFolder  源目录
	 * @param i_TargetFolder  目标目录
	 * @param i_ModifiedTime  文件的最后修改时间
	 * @param i_Exclusion     过滤方式：排除在外的  ，即：不想要哪些
	 * @param i_Accept        过滤方式：在此范围内的，即：想要哪些
	 * @return                返回实际拷贝的文件数量
	 * @throws IOException
	 */
	public int xcopy(File i_SourceFolder ,File i_TargetFolder ,Long i_ModifiedTime ,Pattern i_Exclusion ,Pattern i_Accept) throws IOException
    {
        if ( !i_SourceFolder.exists() )
        {
            throw new VerifyError("Source folder[" + i_SourceFolder.toString() + "] is not exists.");
        }
        
        if ( !i_SourceFolder.isDirectory() )
        {
            throw new VerifyError("Source[" + i_SourceFolder.toString() + "] is not directory.");
        }
        
        if ( !i_SourceFolder.canRead() )
        {
            throw new VerifyError("Source[" + i_SourceFolder.toString() + "] can not read.");
        }
        
        
        boolean v_TargetFolderIsExists = false;
        if ( i_TargetFolder.exists() )
        {
            if ( !i_TargetFolder.isDirectory() )
            {
                throw new VerifyError("Target[" + i_TargetFolder.toString() + "] is not directory.");
            }
            
            v_TargetFolderIsExists = true;
        }
        
        
        int     v_CopyFileCount = 0;
        File [] v_Files         = i_SourceFolder.listFiles();
        if ( Help.isNull(v_Files) )
        {
            return v_CopyFileCount;
        }
        
        for (File v_File : v_Files)
        {
            if ( v_File.isFile() )
            {
                if ( i_ModifiedTime == null || v_File.lastModified() >= i_ModifiedTime.longValue() )
                {
                    if ( i_Exclusion == null || !i_Exclusion.matcher(v_File.getName()).matches() )
                    {
                        if ( i_Accept == null || i_Accept.matcher(v_File.getName()).matches() )
                        {
                            if ( !v_TargetFolderIsExists )
                            {
                                try
                                {
                                    i_TargetFolder.mkdirs();
                                }
                                catch (Exception exce)
                                {
                                    throw new IOException("mkdirs[" + i_TargetFolder.getAbsolutePath() + "] is error. " + exce.getMessage());
                                }
                            }
                            
                            this.copyFile(v_File ,new File(i_TargetFolder.getAbsolutePath() + Help.getSysPathSeparator() + v_File.getName()));
                            v_CopyFileCount++;
                        }
                    }
                }
            }
            else if ( v_File.isDirectory() )
            {
                v_CopyFileCount += this.xcopy(v_File 
                                             ,new File(i_TargetFolder.getAbsolutePath() + Help.getSysPathSeparator() + v_File.getName()) 
                                             ,i_ModifiedTime
                                             ,i_Exclusion
                                             ,i_Accept);
            }
        }
        
        return v_CopyFileCount;
    }
	
	
	
	/**
     * 拷贝文件
     * 
     * @param i_SourceFullName  原文件的全路径
     * @param i_TargetFullName  目标文件的全路径
     * @throws NullPointerException
     * @throws IOException
     */
    public void copyFile(String i_SourceFullName ,String i_TargetFullName) throws NullPointerException, IOException
    {
        if ( Help.isNull(i_SourceFullName) )
        {
            throw new NullPointerException("Source full name is null.");
        }
        
        if ( Help.isNull(i_TargetFullName) )
        {
            throw new NullPointerException("Target full name is null.");
        }
        
        this.copyFile(new File(i_SourceFullName) ,new File(i_TargetFullName));
    }
	
	
	
	/**
	 * 拷贝文件
	 * 
     * @param i_SourceFile  原文件
     * @param i_TargetFile  目标文件
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public void copyFile(File i_SourceFile ,File i_TargetFile) throws NullPointerException, IOException
	{
		if ( !i_SourceFile.exists() )
		{
			throw new IOException("Source file[" + i_SourceFile.toString() + "] is not exists.");
		}
		
		if ( !i_SourceFile.isFile() )
		{
			throw new IOException("Source[" + i_SourceFile.toString() + "] is not file.");
		}
		
		if ( !i_SourceFile.canRead() )
		{
			throw new IOException("Source[" + i_SourceFile.toString() + "] can not read.");
		}
		
		if ( i_TargetFile.exists() )
		{
			if ( this.isOverWrite )
			{
				boolean v_Result = i_TargetFile.delete();
				
				if ( !v_Result )
				{
					throw new IOException("Delete target file[" + i_TargetFile.toString() + "] exception.");
				}
			}
			else
			{
				throw new IOException("Target[" + i_TargetFile.toString() + "] is exists.");
			}
		}
		
		
		FileInputStream      v_SourceInput  = new FileInputStream( i_SourceFile);
		FileOutputStream     v_TargetOutput = new FileOutputStream(i_TargetFile);
		long                 v_SourceLen    = i_SourceFile.length();
		long                 v_WriteIndex   = 0;
		byte []              v_ByteBuffer   = new byte[bufferSize];
		DefaultFileCopyEvent v_Event        = new DefaultFileCopyEvent(this ,v_SourceLen);
		boolean              v_IsContinue   = true;
		
		try
		{
		    v_Event.setInfo(i_TargetFile.getAbsolutePath());
			v_IsContinue = this.fireCopyBeforeListener(v_Event);
			
			while ( v_IsContinue )
			{
				if ( v_WriteIndex + bufferSize <= v_SourceLen )
				{
					v_SourceInput.read(  v_ByteBuffer);
					v_TargetOutput.write(v_ByteBuffer);
					v_TargetOutput.flush();
					
					v_WriteIndex += bufferSize;
					
					v_Event.setCompleteSize(v_WriteIndex);
					v_IsContinue = this.fireCopyingListener(v_Event);
				}
				else
				{
					v_ByteBuffer = new byte[(int)(v_SourceLen - v_WriteIndex)];
					
					v_SourceInput.read(  v_ByteBuffer);
					v_TargetOutput.write(v_ByteBuffer);
					v_TargetOutput.flush();
					
					v_Event.setSucceedFinish();
					this.fireCopyingListener(v_Event);
					break;
				}
			}
		}
		catch (Exception exce)
		{
			v_Event.setEndTime();
			throw new IOException(exce.getMessage());
		}
		finally
		{
			try
			{
				v_SourceInput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			try
			{
				v_TargetOutput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			v_SourceInput  = null;
			v_TargetOutput = null;
			
			this.fireCopyAfterListener(v_Event);
		}
		
	}
	
	
	
	/**
	 * 拷贝文件
	 * 
	 * 不支持进度事件
	 * 
	 * @param i_SourceInput     原文件的文件流
	 * @param i_TargetFullName  目标文件的全路径
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public void copyFile(InputStream i_SourceInput ,String i_TargetFullName) throws NullPointerException, IOException
	{
		if ( i_SourceInput == null )
		{
			throw new NullPointerException("Source InputStream is null.");
		}
		
		if ( Help.isNull(i_TargetFullName) )
		{
			throw new NullPointerException("Target full name is null.");
		}
		
		
		copyFile(i_SourceInput ,new File(i_TargetFullName));
	}
	
	
	
	/**
	 * 拷贝文件
	 * 
	 * 不支持进度事件
	 * 
	 * @param i_SourceInput     原文件的文件流
	 * @param i_TargetFile      目标文件
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public void copyFile(InputStream i_SourceInput ,File i_TargetFile) throws NullPointerException, IOException
	{
		if ( i_SourceInput == null )
		{
			throw new NullPointerException("Source InputStream is null.");
		}
		
		if ( i_TargetFile == null )
		{
			throw new NullPointerException("Target file is null.");
		}
		
		
		File v_TargetFile = i_TargetFile;
		if ( v_TargetFile.exists() )
		{
			if ( this.isOverWrite )
			{
				boolean v_Result = v_TargetFile.delete();
				
				if ( !v_Result )
				{
					throw new IOException("Delete target file[" + i_TargetFile.toString() + "] exception.");
				}
			}
			else
			{
				throw new IOException("Target[" + i_TargetFile.toString() + "] is exists.");
			}
		}
		
		
		InputStream          v_SourceInput  = i_SourceInput;
		FileOutputStream     v_TargetOutput = new FileOutputStream(v_TargetFile);
		int                  v_ReadLen      = 0;
		byte []              v_ByteBuffer   = new byte[bufferSize];
		
		try
		{
			while ( (v_ReadLen = v_SourceInput.read(v_ByteBuffer)) > 0 )
			{
				v_TargetOutput.write(v_ByteBuffer ,0 ,v_ReadLen);
				v_TargetOutput.flush();
			}
		}
		catch (Exception exce)
		{
			throw new IOException(exce.getMessage());
		}
		finally
		{
			try
			{
				v_SourceInput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			try
			{
				v_TargetOutput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			v_SourceInput  = null;
			v_TargetOutput = null;
			v_TargetFile   = null;
		}
		
	}
	
	
	
    /**
     * 下载文件
     * 
     * @param i_URL           要下载的目标文件
     * @param i_SaveFullName  保存文件的全路径
     */
    public void download(String i_URL ,String i_SaveFullName)
    {
        if ( Help.isNull(i_SaveFullName) )
        {
            throw new NullPointerException("SaveFullName is null.");
        }
        
        this.download(i_URL ,new File(i_SaveFullName));
    }
    
    
    
    /**
     * 下载文件
     * 
     * @param i_URL           要下载的目标文件
     * @param i_SaveFile      保存文件的对象信息
     */
    public void download(String i_URL ,File i_SaveFile)
    {
        if ( Help.isNull(i_URL) )
        {
            throw new NullPointerException("URL is null.");
        }
        
        try
        {
            URL               v_URL   = new URL(i_URL);
            HttpURLConnection v_Conn  = (HttpURLConnection)v_URL.openConnection();
            
            v_Conn.setDoInput(true);
            v_Conn.connect();
            
            this.copyFile(v_Conn.getInputStream() ,i_SaveFile);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        } 
    }
    
    
    
    /**
     * 下载文件
     * 
     * @param i_URL           要下载的目标文件
     * @param i_SaveFullName  保存文件的全路径
     */
    public void download(URL i_URL ,String i_SaveFullName)
    {
        if ( Help.isNull(i_SaveFullName) )
        {
            throw new NullPointerException("SaveFullName is null.");
        }
        
        this.download(i_URL ,new File(i_SaveFullName));
    }
    
    
    
    /**
     * 下载文件
     * 
     * @param i_URL           要下载的目标文件
     * @param i_SaveFile      保存文件的对象信息
     */
    public void download(URL i_URL ,File i_SaveFile)
    {
        if ( i_URL == null )
        {
            throw new NullPointerException("URL is null.");
        }
        
        try
        {
            HttpURLConnection v_Conn = (HttpURLConnection)i_URL.openConnection();
            
            v_Conn.setDoInput(true);
            v_Conn.connect();
            
            this.copyFile(v_Conn.getInputStream() ,i_SaveFile);
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        } 
    }
    
    
    
    /**
     * Http 提供下载服务
     * 
     * 注：没有断点续传功能
     * 
     * 未判断入参的合法性
     * 
     * @param i_Response    
     * @param i_ContentType  编码方式
     * @param i_File         提供下载的文件
     * @throws Exception 
     */
    public void downloadServer(HttpServletResponse i_Response ,String i_ContentType ,File i_File) throws Exception
    {
        downloadServer(i_Response ,i_ContentType ,i_File ,null);
    }
    
    
    
    /**
     * Http 提供下载服务
     * 
     * 注：没有断点续传功能
     * 
     * 未判断入参的合法性
     * 
     * @param i_Response    
     * @param i_ContentType  编码方式
     * @param i_File         提供下载的文件
     * @param i_FileName     显示的文件名称（为空则取i_File.getName()的值）
     * @throws Exception 
     */
    public void downloadServer(HttpServletResponse i_Response ,String i_ContentType ,File i_File ,String i_FileName) throws Exception
    {
        FileInputStream     v_Input       = null;
        BufferedInputStream v_InputBuffer = null;
        OutputStream        v_Output      = null;
        
        try
        {
            // 非常重要
            i_Response.reset();  
            // 设置编码方式
            i_Response.setContentType(i_ContentType);
            // 写明要下载的文件的大小
            i_Response.setHeader("Content-Length" ,new Long(i_File.length()).toString());
            // 设置附加文件名。转码是为了防止文件名称中文时乱码的情况
            i_Response.setHeader("Content-Disposition" ,"attachment;filename=" + StringHelp.toCharEncoding(Help.NVL(i_FileName ,i_File.getName()) ,i_ContentType ,"ISO-8859-1"));  
            i_Response.setStatus(HttpServletResponse.SC_OK);

            
            v_Input       = new FileInputStream(i_File);
            v_InputBuffer = new BufferedInputStream(v_Input);
            v_Output      = i_Response.getOutputStream();
            
            // 小于 10m 的文件
            if ( i_File.length() <= 1024 * 1024 * 10 )
            {
                byte [] v_ReadBuffer = new byte[v_InputBuffer.available()];
                v_InputBuffer.read(v_ReadBuffer);   // 将数据一次性读取
                v_Output.write(v_ReadBuffer);       // 将数据一次性写到客户端的内存
            }
            else
            {
                byte [] v_ReadBuffer = new byte[this.bufferSize];
                int     v_ReadSize   = 0;
                while ( (v_ReadSize = v_InputBuffer.read(v_ReadBuffer ,0 ,this.bufferSize)) > 0 )
                {
                    v_Output.write(v_ReadBuffer ,0 ,v_ReadSize);
                }
            }
            
            v_Output.flush();
        }
        catch (Exception exce)
        {
            throw exce;
        }
        finally
        {
            if ( v_Output != null )
            {
                try
                {
                    v_Output.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_Output = null;
            }
            
            if ( v_InputBuffer != null )
            {
                try
                {
                    v_InputBuffer.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_InputBuffer = null;
            }
            
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_Input = null;
            }
        }
    }
    
    
    
    /**
     * Http 提供下载服务
     * 
     * 注：支持断点续传功能
     * 
     * 未判断入参的合法性
     * 
     * @param i_Request
     * @param i_Response    
     * @param i_ContentType  编码方式
     * @param i_File         提供下载的文件
     * @throws Exception 
     */
    public void downloadServer(HttpServletRequest i_Request ,HttpServletResponse i_Response ,String i_ContentType ,File i_File) throws Exception
    {
        downloadServer(i_Request ,i_Response ,i_ContentType ,i_File ,null);
    }
    
    
    
    /**
     * Http 提供下载服务
     * 
     * 注：支持断点续传功能
     * 
     * 未判断入参的合法性
     * 
     * @param i_Request
     * @param i_Response    
     * @param i_ContentType  编码方式
     * @param i_File         提供下载的文件
     * @param i_FileName     显示的文件名称（为空则取i_File.getName()的值）
     * @throws Exception 
     */
    public void downloadServer(HttpServletRequest i_Request ,HttpServletResponse i_Response ,String i_ContentType ,File i_File ,String i_FileName) throws Exception
    {
        FileInputStream     v_Input       = null;
        BufferedInputStream v_InputBuffer = null;
        OutputStream        v_Output      = null;
        
        try
        {
            v_Input       = new FileInputStream(i_File);
            v_InputBuffer = new BufferedInputStream(v_Input);
            v_Output      = i_Response.getOutputStream();
            
            
            // 非常重要
            i_Response.reset();
            // 设置编码方式
            i_Response.setContentType(i_ContentType);
            // 设置附加文件名 
            i_Response.setHeader("Content-Disposition" ,"attachment;filename=" + StringHelp.toCharEncoding(Help.NVL(i_FileName ,i_File.getName()) ,i_ContentType ,"ISO-8859-1"));  
            
            
            String  v_Range       = i_Request.getHeader("Range");
            boolean v_IsRange     = false;                          // 标记是否断点续传
            long    v_FileLength  = i_File.length();
            long    v_ContentLen  = 0;
            long    v_Place_Being = 0;
            long    v_Place_End   = 0;
            
            // 判断客户端是否请求续传
            if ( v_Range != null && v_Range.trim().length() > 0 && !"null".equals(v_Range) )
            {
                v_IsRange = true;
                i_Response.setHeader("Accept-Ranges" ,"bytes");                   // 同意客户端的续传请求
                i_Response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                
                String v_RangeBytes = v_Range.replaceAll("bytes=" ,"");
                
                if ( v_RangeBytes.startsWith("-") )
                {
                    // 表示最后500个字节：Range: bytes=-500
 
                    v_Place_End   = Long.parseLong(v_RangeBytes.substring(v_RangeBytes.indexOf("-") + 1));
                    v_Place_Being = v_FileLength - v_Place_End - 1;
                }
                else if ( v_RangeBytes.endsWith("-") )
                {
                    // 表示500字节以后的范围：Range: bytes=500-
                    v_Place_Being = Long.parseLong(v_RangeBytes.substring(0 ,v_RangeBytes.indexOf("-")));
                    v_Place_End   = v_FileLength - v_Place_Being - 1;
                }
                else
                {
                    // 表示头500个字节：  Range: bytes=0-499
                    // 表示第二个500字节：Range: bytes=500-999
                    v_Place_Being = Long.parseLong(v_RangeBytes.substring(0 ,v_RangeBytes.indexOf("-")));
                    v_Place_End   = Long.parseLong(v_RangeBytes.substring(v_RangeBytes.indexOf("-") + 1));
                }
            }
            // 没有续传的情况
            else
            {
                v_Place_Being = 0;
                v_Place_End   = v_FileLength - 1;
            }
            
            
            v_ContentLen = v_Place_End - v_Place_Being + 1;
            i_Response.setHeader("Content-Length" ,v_ContentLen + "");
            
            
            if ( v_IsRange )
            {
                // 断点开始，响应的格式是: Content-Range: bytes [文件块的开始字节]-[文件块的结束字节]/[文件的总大小]
                String v_ContentRange = new StringBuilder("bytes ").append(v_Place_Being).append("-").append(v_Place_End).append("/").append(v_FileLength).toString();
                i_Response.setHeader("Content-Range" ,v_ContentRange);
                
                if ( v_Place_Being > 0 )
                {
                    @SuppressWarnings("unused")
                    long v_Ret = v_InputBuffer.skip(v_Place_Being);
                }
            }
            
            
            byte [] v_ReadBuffer = new byte[this.bufferSize];
            int     v_ReadSize   = 0;
            long    v_ReadTotal  = 0;
            
            
            while ( v_ReadTotal <= v_ContentLen - this.bufferSize )
            {
                v_ReadSize = v_InputBuffer.read(v_ReadBuffer , 0 ,this.bufferSize);
                v_ReadTotal += v_ReadSize;
                v_Output.write(v_ReadBuffer ,0 ,v_ReadSize);
            }
            
            if ( v_ReadTotal <= v_ContentLen )
            {
                v_ReadSize = v_InputBuffer.read(v_ReadBuffer ,0 ,this.bufferSize);
                v_Output.write(v_ReadBuffer ,0 ,v_ReadSize);
            }
            
            // 将写入到客户端的内存的数据,刷新到磁盘
            v_Output.flush();
        }
        catch (Exception exce)
        {
            throw exce;
        }
        finally
        {
            if ( v_Output != null )
            {
                try
                {
                    v_Output.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_Output = null;
            }
            
            if ( v_InputBuffer != null )
            {
                try
                {
                    v_InputBuffer.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_InputBuffer = null;
            }
            
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_Input = null;
            }
        }
    }
    
    
    
    /**
     * Http 提供上传服务
     * 
     * 注：支持断点续传功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-09
     * @version     v1.0
     *
     * @param i_Dir         保存文件的目录
     * @param i_DataPacket  数据包
     * @return              本次数据包上传结果
     * @throws Exception
     */
    public int uploadServer(String i_Dir ,FileDataPacket i_DataPacket) throws Exception
    {
        return uploadServer(new File(i_Dir) ,i_DataPacket);
    }
    
    
    
    /**
     * Http 提供上传服务
     * 
     * 注：支持断点续传功能
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-09
     * @version     v1.0
     *
     * @param i_Dir         保存文件的目录
     * @param i_DataPacket  数据包
     * @return              本次数据包上传结果
     * @throws Exception
     */
    public int uploadServer(File i_Dir ,FileDataPacket i_DataPacket) throws Exception
    {
        if ( null == i_Dir
          || null == i_DataPacket
          || null == i_DataPacket.getDataNo()
          || null == i_DataPacket.getDataCount()
          || Help.isNull(i_DataPacket.getName()) )
        {
            return $Upload_Error;
        }
        
        if ( !i_Dir.exists() )
        {
            try
            {
                i_Dir.mkdirs();
            }
            catch (Exception exce)
            {
                throw exce;
            }
        }
        else if ( !i_Dir.isDirectory() )
        {
            return $Upload_Error;
        }
        
        try
        {
            FileDataPacket v_Old = $DataPackets.get(i_DataPacket.getName());
            if ( v_Old != null )
            {
                if ( v_Old.getDataNo().intValue() >= i_DataPacket.getDataNo().intValue() )
                {
                    return $Upload_GoOn;
                }
            }
            
            this.append(i_Dir.getAbsolutePath() + Help.getSysPathSeparator() + i_DataPacket.getName() ,i_DataPacket.getDataByte());
            
            if ( i_DataPacket.getDataCount().intValue() == i_DataPacket.getDataNo().intValue() )
            {
                $DataPackets.remove(i_DataPacket.getName());
                return $Upload_Finish;
            }
            else
            {
                $DataPackets.put(i_DataPacket.getName() ,i_DataPacket ,this.dataPacketTimeOut);
                return $Upload_GoOn;
            }
        }
        catch (Exception exce)
        {
            throw exce;
        }
    }
    
    
    
    /**
     * 获取Http Post请求中的数据。默认为:UTF-8
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-09
     * @version     v1.0
     *
     * @param i_Request
     * @return
     */
    public static String getContent(ServletRequest i_Request)
    {
        return getContent(i_Request ,"UTF-8");
    }
    
    
    
    /**
     * 获取Http Post请求中的数据
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-09
     * @version     v1.0
     *
     * @param i_Request
     * @param i_CharacterEncoding
     * @return
     */
    public static String getContent(ServletRequest i_Request ,String i_CharacterEncoding)
    {
        BufferedReader v_Input = null;
        try
        {
            StringBuilder v_Buffer = new StringBuilder();
            String        v_Line   = "";
            v_Input  = new BufferedReader(new InputStreamReader(i_Request.getInputStream() ,i_CharacterEncoding));
            while ( (v_Line = v_Input.readLine())!= null )
            {
                v_Buffer.append(v_Line);
            }
            
            return v_Buffer.toString();
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception exce)
                {
                    exce.printStackTrace();
                }
            }
            v_Input = null;
        }
        
        return null;
    }
    
    
    
    /**
     * Http Post请求返回数据的写入
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-11
     * @version     v1.0
     *
     * @param i_Datas              返回的数据
     * @param i_Response           响应对象
     */
    public void writeHttp(String i_Datas ,ServletResponse i_Response) throws IOException
    {
        writeHttp(i_Datas ,i_Response ,this.charEncoding ,this.contentType);
    }
    
    
    
    /**
     * Http Post请求返回数据的写入
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-10-11
     * @version     v1.0
     *
     * @param i_Datas              返回的数据
     * @param i_Response           响应对象
     * @param i_CharacterEncoding  字符编码
     * @param i_ContentType        内容类型
     */
    public static void writeHttp(String i_Datas ,ServletResponse i_Response ,String i_CharacterEncoding ,String i_ContentType) throws IOException
    {
        i_Response.setCharacterEncoding(i_CharacterEncoding);
        i_Response.setContentType(i_ContentType);
        
        PrintWriter v_Out = null;
        try
        {
            v_Out = i_Response.getWriter();
            v_Out.write(i_Datas);
        }
        catch (IOException e)
        {
            throw e;
        }
        finally
        {
            if ( v_Out != null )
            {
                v_Out.close();
            }
        }
    }
    
    
    
    /**
     * 打印请求报文头信息。主用于调试
     * 
     * @param i_Request
     */
    public static void printlnRequestHeader(HttpServletRequest i_Request)
    {
        if ( i_Request != null )
        {
            Enumeration<?> v_HeaderNames = i_Request.getHeaderNames();
            
            while ( v_HeaderNames.hasMoreElements() )
            {
                String v_HeaderName  = v_HeaderNames.nextElement().toString();
                String v_HeaderValue = i_Request.getHeader(v_HeaderName);
                
                System.out.println("-- " + v_HeaderName + "=" + v_HeaderValue);
            }
        }
    }
    
    
    
    /**
     * 追加写入文件(编码UTF-8)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-09-07
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @throws IOException
     */
    public void append(String i_SaveFileFullName ,String i_Contents) throws IOException
    {
        this.append(i_SaveFileFullName ,i_Contents ,this.charEncoding);
    }
    
    
    
    /**
     * 追加写入文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-09-07
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @param i_CharEncoding      文件编码
     * @throws IOException
     */
    public void append(String i_SaveFileFullName ,String i_Contents ,String i_CharEncoding) throws IOException
    {
        this.isAppend = true;
        this.create(i_SaveFileFullName ,i_Contents ,i_CharEncoding);
    }
    
    
    
    /**
     * 追加写入文件（二进制）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-09-07
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @throws IOException
     */
    public void append(String i_SaveFileFullName ,byte [] i_Contents) throws IOException
    {
        this.isAppend = true;
        this.create(i_SaveFileFullName ,i_Contents);
    }
    
    
    
    /**
     * 创建并写入文件(编码UTF-8)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-05-11
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @throws IOException
     */
    public void create(String i_SaveFileFullName ,String i_Contents) throws IOException
    {
        this.create(i_SaveFileFullName ,i_Contents ,this.charEncoding);
    }
    
    
    
    /**
     * 创建并写入文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-05-11
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @param i_CharEncoding      文件编码
     * @throws IOException
     */
    public void create(String i_SaveFileFullName ,String i_Contents ,String i_CharEncoding) throws IOException
    {
        if ( Help.isNull(i_SaveFileFullName) )
        {
            throw new NullPointerException("Save full name is null.");
        }
        
        File v_SaveFile = new File(i_SaveFileFullName);
        if ( v_SaveFile.exists() )
        {
            if ( !this.isAppend )
            {
                if ( this.isOverWrite )
                {
                    boolean v_Result = v_SaveFile.delete();
                    
                    if ( !v_Result )
                    {
                        v_SaveFile = null;
                        throw new IOException("Delete target file[" + i_SaveFileFullName + "] exception.");
                    }
                }
                else
                {
                    v_SaveFile = null;
                    throw new IOException("Target[" + i_SaveFileFullName + "] is exists.");
                }
            }
        }
        
        
        FileOutputStream   v_SaveOutput = new FileOutputStream(v_SaveFile ,this.isAppend);
        OutputStreamWriter v_SaveWriter = new OutputStreamWriter(v_SaveOutput ,i_CharEncoding); 

        try
        {
            v_SaveWriter.write(i_Contents);
            v_SaveWriter.flush();
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
        finally
        {
            try
            {
                v_SaveWriter.flush();
                v_SaveWriter.close();
            }
            catch (Exception exce)
            {
                // Nothing.
            }
            
            try
            {
                v_SaveOutput.close();
            }
            catch (Exception exce)
            {
                // Nothing.
            }
            
            v_SaveWriter = null;
            v_SaveOutput = null;
            v_SaveFile   = null;
        }
    }
    
    
    
    /**
     * 创建并写入文件（二进制）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-09-07
     * @version     v1.0
     *
     * @param i_SaveFileFullName  保存文件的全名称
     * @param i_Contents          文件内容
     * @throws IOException
     */
    public void create(String i_SaveFileFullName ,byte [] i_Contents) throws IOException
    {
        if ( Help.isNull(i_SaveFileFullName) )
        {
            throw new NullPointerException("Save full name is null.");
        }
        
        File v_SaveFile = new File(i_SaveFileFullName);
        if ( v_SaveFile.exists() )
        {
            if ( !this.isAppend )
            {
                if ( this.isOverWrite )
                {
                    boolean v_Result = v_SaveFile.delete();
                    
                    if ( !v_Result )
                    {
                        v_SaveFile = null;
                        throw new IOException("Delete target file[" + i_SaveFileFullName + "] exception.");
                    }
                }
                else
                {
                    v_SaveFile = null;
                    throw new IOException("Target[" + i_SaveFileFullName + "] is exists.");
                }
            }
        }
        
        
        FileOutputStream v_SaveOutput = new FileOutputStream(v_SaveFile ,this.isAppend);

        try
        {
            v_SaveOutput.write(i_Contents == null ? new byte[0] : i_Contents);
            v_SaveOutput.flush();
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
        finally
        {
            try
            {
                v_SaveOutput.close();
            }
            catch (Exception exce)
            {
                // Nothing.
            }
            
            v_SaveOutput = null;
            v_SaveFile   = null;
        }
    }
	
	
	
	/**
	 * 创建CSV格式的文件(编码UTF-8)
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_Contents          文件内容。集合元素须实现 com.huoyu.common.file.FileSerializable 接口
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,List<?> i_Contents) throws IOException
	{
		this.createCSV(i_SaveFileFullName ,null ,i_Contents ,this.charEncoding);
	}
	
	
	
	/**
	 * 创建CSV格式的文件(编码UTF-8)
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_Titles            标题信息
	 * @param i_Contents          文件内容。集合元素须实现 com.huoyu.common.file.FileSerializable 接口
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,List<?> i_Titles ,List<?> i_Contents) throws IOException
	{
		this.createCSV(i_SaveFileFullName ,i_Titles ,i_Contents ,this.charEncoding);
	}
	
	
	
	/**
	 * 创建CSV格式的文件(编码UTF-8) -- 支持超大数据源。
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_FileBiggerMemory  超大数据源存储器 com.huoyu.common.file.FileBiggerMemory 接口
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,FileBiggerMemory i_FileBiggerMemory) throws IOException
	{
		this.createCSV(i_SaveFileFullName ,null ,i_FileBiggerMemory ,this.charEncoding);
	}
	
	
	
	/**
	 * 创建CSV格式的文件(编码UTF-8) -- 支持超大数据源。
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_Titles            标题信息
	 * @param i_FileBiggerMemory  超大数据源存储器 com.huoyu.common.file.FileBiggerMemory 接口
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,List<?> i_Titles ,FileBiggerMemory i_FileBiggerMemory) throws IOException
	{
		this.createCSV(i_SaveFileFullName ,i_Titles ,i_FileBiggerMemory ,this.charEncoding);
	}
	
	
	
	/**
	 * 创建CSV格式的文件
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_Titles            标题信息
	 * @param i_Contents          文件内容。集合元素须实现 com.huoyu.common.file.FileSerializable 接口
	 * @param i_CharEncoding      文件的编码
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,List<?> i_Titles ,List<?> i_Contents ,String i_CharEncoding) throws IOException
	{
		if ( Help.isNull(i_SaveFileFullName) )
		{
			throw new NullPointerException("Save full name is null.");
		}
		
		if ( i_Contents == null || i_Contents.size() <= 0 )
		{
			throw new NullPointerException("File[" + i_SaveFileFullName + "] contents is null.");
		}
		
		
		File v_SaveFile = new File(i_SaveFileFullName);
		if ( v_SaveFile.exists() )
		{
		    if ( !this.isAppend )
		    {
    			if ( this.isOverWrite )
    			{
    				boolean v_Result = v_SaveFile.delete();
    				
    				if ( !v_Result )
    				{
    					v_SaveFile = null;
    					throw new IOException("Delete target file[" + i_SaveFileFullName + "] exception.");
    				}
    			}
    			else
    			{
    				v_SaveFile = null;
    				throw new IOException("Target[" + i_SaveFileFullName + "] is exists.");
    			}
		    }
		}
		
		
		long                  v_RowSize    = i_Contents.size();
		FileOutputStream      v_SaveOutput = new FileOutputStream(v_SaveFile ,this.isAppend);
		OutputStreamWriter    v_SaveWriter = new OutputStreamWriter(v_SaveOutput ,i_CharEncoding); 
		DefaultCreateCSVEvent v_Event      = new DefaultCreateCSVEvent(this ,v_RowSize);
		boolean               v_IsContinue = true;

		try
		{
			v_IsContinue = this.fireCreateCSVBeforeListener(v_Event);
			
			if ( v_IsContinue )
			{
				// 行级对象的Java类型。
				// 1: FileSerializable
				// 2: List
				int    v_RowInfoType  = 0;
				int    v_PropertySize = 0;
				Object v_TitleData    = i_Contents.get(0);
				 
				 
				if ( v_TitleData instanceof FileSerializable )
				{
					v_PropertySize = ((FileSerializable)v_TitleData).gatPropertySize();
					v_RowInfoType  = 1;
				}
				else if ( v_TitleData instanceof List )
				{
					v_PropertySize = ((List<?>)v_TitleData).size();
					v_RowInfoType  = 2;
				}
				else
				{
					throw new ClassCastException("Row info type is only FileSerializable or List.");
				}
				
				
				if ( v_PropertySize > 0 )
				{
					if ( v_RowInfoType == 1 )
					{
						// 写入标题
						if ( i_Titles == null )
						{
							this.createCSV_WriteTitle(v_SaveWriter ,(FileSerializable)v_TitleData ,v_PropertySize);
						}
						else
						{
							this.createCSV_WriteTitle(v_SaveWriter ,i_Titles ,v_PropertySize);
						}
					
						// 写入首行数据
						this.createCSV_WriteRowData(v_SaveWriter ,(FileSerializable)v_TitleData ,v_PropertySize);
						
						((FileSerializable)v_TitleData).freeResource();
					}
					else if ( v_RowInfoType == 2 )
					{
						// 写入标题
						if ( i_Titles != null )
						{
							this.createCSV_WriteTitle(v_SaveWriter ,i_Titles ,v_PropertySize);
						}
						
						// 写入首行数据
						this.createCSV_WriteRowData(v_SaveWriter ,(List<?>)v_TitleData ,v_PropertySize);
						
						((List<?>)v_TitleData).clear();
					}
					
					v_TitleData = null;
					
					
					// 写入内容
					if ( v_RowInfoType == 1 )
					{
						for (long v_RowIndex=1; v_IsContinue && v_RowIndex<v_RowSize; v_RowIndex++)
						{
							FileSerializable v_RowData = (FileSerializable)i_Contents.get((int)v_RowIndex);
							
							this.createCSV_WriteRowData(v_SaveWriter ,v_RowData , v_PropertySize);
							
							v_Event.setCompleteSize(v_RowIndex);
							v_IsContinue = this.fireCreatingCSVListener(v_Event);
						}
					}
					else if ( v_RowInfoType == 2 )
					{
						for (long v_RowIndex=1; v_IsContinue && v_RowIndex<v_RowSize; v_RowIndex++)
						{
							List<?> v_RowData = (List<?>)i_Contents.get((int)v_RowIndex);
							
							this.createCSV_WriteRowData(v_SaveWriter ,v_RowData , v_PropertySize);
							
							v_Event.setCompleteSize(v_RowIndex);
							v_IsContinue = this.fireCreatingCSVListener(v_Event);
						}
					}
				}
			}
			
			v_Event.setSucceedFinish();
		}
		catch (Exception exce)
		{
			v_Event.setEndTime();
			throw new IOException(exce.getMessage());
		}
		finally
		{
			try
			{
				v_SaveWriter.flush();
				v_SaveWriter.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			try
			{
				v_SaveOutput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			v_SaveWriter = null;
			v_SaveOutput = null;
			v_SaveFile   = null;
			
			this.fireCreateCSVAfterListener(v_Event);
		}
		
	}
	
	
	
	/**
	 * 创建CSV格式的文件 -- 支持超大数据源。
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_Titles            标题信息
	 * @param i_FileBiggerMemory  超大数据源存储器 com.huoyu.common.file.FileBiggerMemory 接口
	 * @param i_CharEncoding      文件的编码
	 * @throws IOException 
	 */
	public void createCSV(String i_SaveFileFullName ,List<?> i_Titles ,FileBiggerMemory i_FileBiggerMemory ,String i_CharEncoding) throws IOException
	{
		if ( Help.isNull(i_SaveFileFullName) )
		{
			throw new NullPointerException("Save full name is null.");
		}
		
		if ( i_FileBiggerMemory == null || i_FileBiggerMemory.getRowSize() <= 0 )
		{
			throw new NullPointerException("File[" + i_SaveFileFullName + "] bigger memory is null.");
		}
		
		
		File v_SaveFile = new File(i_SaveFileFullName);
		if ( v_SaveFile.exists() )
		{
		    if ( !this.isAppend )
		    {
    			if ( this.isOverWrite )
    			{
    				boolean v_Result = v_SaveFile.delete();
    				
    				if ( !v_Result )
    				{
    					v_SaveFile = null;
    					throw new IOException("Delete target file[" + i_SaveFileFullName + "] exception.");
    				}
    			}
    			else
    			{
    				v_SaveFile = null;
    				throw new IOException("Target[" + i_SaveFileFullName + "] is exists.");
    			}
		    }
		}
		
		
		long                  v_RowSize    = i_FileBiggerMemory.getRowSize();
		FileOutputStream      v_SaveOutput = new FileOutputStream(v_SaveFile ,this.isAppend);
		OutputStreamWriter    v_SaveWriter = new OutputStreamWriter(v_SaveOutput ,i_CharEncoding); 
		DefaultCreateCSVEvent v_Event      = new DefaultCreateCSVEvent(this ,v_RowSize);
		boolean               v_IsContinue = true;

		try
		{
			v_IsContinue = this.fireCreateCSVBeforeListener(v_Event);
			
			if ( v_IsContinue )
			{
				// 行级对象的Java类型。
				// 1: FileSerializable
				// 2: List
				int    v_RowInfoType  = 0;
				int    v_PropertySize = 0;
				Object v_TitleData    = i_FileBiggerMemory.getRowInfo(0);
				 
				 
				if ( v_TitleData instanceof FileSerializable )
				{
					v_PropertySize = ((FileSerializable)v_TitleData).gatPropertySize();
					v_RowInfoType  = 1;
				}
				else if ( v_TitleData instanceof List )
				{
					v_PropertySize = ((List<?>)v_TitleData).size();
					v_RowInfoType  = 2;
				}
				else
				{
					throw new ClassCastException("Row info type is only FileSerializable or List.");
				}
				 
				
				if ( v_PropertySize > 0 )
				{
					if ( v_RowInfoType == 1 )
					{
						// 写入标题
						if ( i_Titles == null )
						{
							this.createCSV_WriteTitle(v_SaveWriter ,(FileSerializable)v_TitleData ,v_PropertySize);
						}
						else
						{
							this.createCSV_WriteTitle(v_SaveWriter ,i_Titles ,v_PropertySize);
						}
					
						// 写入首行数据
						this.createCSV_WriteRowData(v_SaveWriter ,(FileSerializable)v_TitleData ,v_PropertySize);
						
						((FileSerializable)v_TitleData).freeResource();
					}
					else if ( v_RowInfoType == 2 )
					{
						// 写入标题
						if ( i_Titles != null )
						{
							this.createCSV_WriteTitle(v_SaveWriter ,i_Titles ,v_PropertySize);
						}
						
						// 写入首行数据
						this.createCSV_WriteRowData(v_SaveWriter ,(List<?>)v_TitleData ,v_PropertySize);
						
						((List<?>)v_TitleData).clear();
					}
					
					v_TitleData = null;
					
					
					// 写入内容
					if ( v_RowInfoType == 1 )
					{
						for (long v_RowIndex=1; v_IsContinue && v_RowIndex<v_RowSize; v_RowIndex++)
						{
							FileSerializable v_RowData = (FileSerializable)i_FileBiggerMemory.getRowInfo(v_RowIndex);
							
							this.createCSV_WriteRowData(v_SaveWriter ,v_RowData , v_PropertySize);
							
							v_RowData.freeResource();
							v_RowData = null;
							
							v_Event.setCompleteSize(v_RowIndex);
							v_IsContinue = this.fireCreatingCSVListener(v_Event);
						}
					}
					else if ( v_RowInfoType == 2 )
					{
						for (long v_RowIndex=1; v_IsContinue && v_RowIndex<v_RowSize; v_RowIndex++)
						{
							List<?> v_RowData = (List<?>)i_FileBiggerMemory.getRowInfo(v_RowIndex);
							
							this.createCSV_WriteRowData(v_SaveWriter ,v_RowData , v_PropertySize);
							
							v_RowData.clear();
							v_RowData = null;
							
							v_Event.setCompleteSize(v_RowIndex);
							v_IsContinue = this.fireCreatingCSVListener(v_Event);
						}
					}
				}
			}
			
			v_Event.setSucceedFinish();
		}
		catch (Exception exce)
		{
			v_Event.setEndTime();
			throw new IOException(exce.getMessage());
		}
		finally
		{
			try
			{
				v_SaveWriter.flush();
				v_SaveWriter.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			try
			{
				v_SaveOutput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			v_SaveWriter = null;
			v_SaveOutput = null;
			v_SaveFile   = null;
			
			this.fireCreateCSVAfterListener(v_Event);
		}
		
	}
	
	
	
	/**
	 * 创建CSV格式的文件 -- 支持超大数据源。
	 * 
	 * 主要用于性能对比测试。
	 * 
	 * @param i_SaveFileFullName  保存文件的全名称
	 * @param i_ResultSet         数据库结果集
	 * @param i_CharEncoding      文件的编码
	 * @throws IOException 
	 */
	@SuppressWarnings("unused")
	private void createCSV(String i_SaveFileFullName ,ResultSet i_ResultSet ,String i_CharEncoding) throws IOException
	{
		if ( Help.isNull(i_SaveFileFullName) )
		{
			throw new NullPointerException("Save full name is null.");
		}
		
		if ( i_ResultSet == null )
		{
			throw new NullPointerException("File[" + i_SaveFileFullName + "] bigger memory is null.");
		}
		
		
		File v_SaveFile = new File(i_SaveFileFullName);
		if ( v_SaveFile.exists() )
		{
		    if ( !this.isAppend )
		    {
    			if ( this.isOverWrite )
    			{
    				boolean v_Result = v_SaveFile.delete();
    				
    				if ( !v_Result )
    				{
    					v_SaveFile = null;
    					throw new IOException("Delete target file[" + i_SaveFileFullName + "] exception.");
    				}
    			}
    			else
    			{
    				v_SaveFile = null;
    				throw new IOException("Target[" + i_SaveFileFullName + "] is exists.");
    			}
		    }
		}
		
		
		// long                  v_RowSize    = -1;
		FileOutputStream      v_SaveOutput = new FileOutputStream(v_SaveFile ,this.isAppend);
		OutputStreamWriter    v_SaveWriter = new OutputStreamWriter(v_SaveOutput ,i_CharEncoding); 
		// DefaultCreateCSVEvent v_Event      = new DefaultCreateCSVEvent(this ,v_RowSize);
		boolean               v_IsContinue = true;

		try
		{
			// v_IsContinue = this.fireCreateCSVBeforeListener(v_Event);
			
			if ( v_IsContinue )
			{
				int v_PropertySize = i_ResultSet.getMetaData().getColumnCount();
				
				while ( i_ResultSet.next() )
				{
					// 写入内容
					for (int v_PropertyIndex=1; v_PropertyIndex<=v_PropertySize; v_PropertyIndex++)
					{
						String v_Value = i_ResultSet.getString(v_PropertyIndex);
						
						if ( v_Value == null )
						{
							v_SaveWriter.write("\"\"");
						}
						else
						{
							v_SaveWriter.write("\"");
							v_SaveWriter.write(v_Value.replaceAll("\"", "\"\""));
							v_SaveWriter.write("\"");
						}
						
						if ( v_PropertyIndex < v_PropertySize - 1 )
						{
							v_SaveWriter.write(",");
						}
					}
					
					v_SaveWriter.write(this.getNewLine());
					v_SaveWriter.flush();
					
					
					// v_Event.setCompleteSize(v_RowIndex);
					// v_IsContinue = this.fireCreatingCSVListener(v_Event);
				}
			}
			
			// v_Event.setCompleteSize(v_Event.getSize());
			// v_Event.setEndTime(new Date());
			// this.fireCreateCSVAfterListener(v_Event);
		}
		catch (Exception exce)
		{
			throw new IOException(exce.getMessage());
		}
		finally
		{
			try
			{
				v_SaveWriter.flush();
				v_SaveWriter.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			try
			{
				v_SaveOutput.close();
			}
			catch (Exception exce)
			{
				// Nothing.
			}
			
			v_SaveWriter = null;
			v_SaveOutput = null;
			v_SaveFile   = null;
		}
		
	}
	
	
	
	/**
	 * 创建CSV文件时，写入标题的方法
	 * 
	 * @param i_Write         数据流
	 * @param i_RowData       行数据
	 * @param i_PropertySize  属性数量
	 * @throws IOException
	 */
	private void createCSV_WriteTitle(OutputStreamWriter i_Write ,FileSerializable i_RowData ,int i_PropertySize) throws IOException
	{
	    StringBuilder v_Buffer = new StringBuilder();
		
		for (int v_PropertyIndex=0; v_PropertyIndex<i_PropertySize; v_PropertyIndex++)
		{
			v_Buffer.append("\"");
			
			Object v_Value = i_RowData.gatPropertyName(v_PropertyIndex);
			if ( v_Value != null )
			{
				v_Buffer.append(v_Value.toString().replaceAll("\"", "\"\""));
			}
			
			v_Buffer.append("\"");
			
			if ( v_PropertyIndex < i_PropertySize - 1 )
			{
				v_Buffer.append(",");
			}
		}
		
		v_Buffer.append(this.getNewLine());
		
		i_Write.write(v_Buffer.toString());
		i_Write.flush();
	}
	
	
	
	/**
	 * 创建CSV文件时，写入标题的方法
	 * 
	 * @param i_Write         数据流
	 * @param i_RowData       行数据
	 * @param i_PropertySize  属性数量
	 * @throws IOException
	 */
	private void createCSV_WriteTitle(OutputStreamWriter i_Write ,List<?> i_Titals ,int i_PropertySize) throws IOException
	{
	    StringBuilder v_Buffer = new StringBuilder();
		
		for (int v_PropertyIndex=0; v_PropertyIndex<i_PropertySize; v_PropertyIndex++)
		{
			v_Buffer.append("\"");
			
			Object v_Value = i_Titals.get(v_PropertyIndex);
			if ( v_Value != null )
			{
				v_Buffer.append(v_Value.toString().replaceAll("\"", "\"\""));
			}
			
			v_Buffer.append("\"");
			
			if ( v_PropertyIndex < i_PropertySize - 1 )
			{
				v_Buffer.append(",");
			}
		}
		
		v_Buffer.append(this.getNewLine());
		
		i_Write.write(v_Buffer.toString());
		i_Write.flush();
	}
	
	
	
	/**
	 * 创建CSV文件时，写入一行数据的方法
	 * 
	 * @param i_Write         数据流
	 * @param i_RowData       行数据
	 * @param i_PropertySize  属性数量
	 * @throws IOException
	 */
	private void createCSV_WriteRowData(OutputStreamWriter i_Write ,FileSerializable i_RowData ,int i_PropertySize) throws IOException
	{
	    StringBuilder v_Buffer = new StringBuilder();
		
		for (int v_PropertyIndex=0; v_PropertyIndex<i_PropertySize; v_PropertyIndex++)
		{
            v_Buffer.append(this.getCsvDataPrefix());
			v_Buffer.append("\"");
			
			Object v_Value = i_RowData.gatPropertyValue(v_PropertyIndex);
			if ( v_Value != null )
			{
				v_Buffer.append(v_Value.toString().replaceAll("\"", "\"\""));
			}
			
			v_Buffer.append("\"");
			
			if ( v_PropertyIndex < i_PropertySize - 1 )
			{
				v_Buffer.append(",");
			}
		}
		
		v_Buffer.append(this.getNewLine());
		
		i_Write.write(v_Buffer.toString());
		i_Write.flush();
	}
	
	
	
	/**
	 * 创建CSV文件时，写入一行数据的方法
	 * 
	 * @param i_Write         数据流
	 * @param i_RowData       行数据
	 * @param i_PropertySize  属性数量
	 * @throws IOException
	 */
	private void createCSV_WriteRowData(OutputStreamWriter i_Write ,List<?> i_RowData ,int i_PropertySize) throws IOException
	{
	    StringBuilder v_Buffer = new StringBuilder();
		
		for (int v_PropertyIndex=0; v_PropertyIndex<i_PropertySize; v_PropertyIndex++)
		{
            v_Buffer.append(this.getCsvDataPrefix());
			v_Buffer.append("\"");
			
			Object v_Value = i_RowData.get(v_PropertyIndex);
			if ( v_Value != null )
			{
				v_Buffer.append(v_Value.toString().replaceAll("\"", "\"\""));
			}
			
			v_Buffer.append("\"");
			
			if ( v_PropertyIndex < i_PropertySize - 1 )
			{
				v_Buffer.append(",");
			}
		}
		
		v_Buffer.append(this.getNewLine());
		
		i_Write.write(v_Buffer.toString());
		i_Write.flush();
	}
	
	
	
	/**
	 * 创建Zip的压缩文件(不忽略原文件异常)
     * 
     * 创建的Zip文件没有目录结构
	 * 
	 * @param i_SaveZipFullName        保存Zip压缩文件全名称
	 * @param i_SourceLists            要压缩的原文件列表
	 * @throws IOException
	 */
	public void createZip(String i_SaveZipFullName ,Collection<?> i_SourceLists) throws IOException
	{
		this.createZip(i_SaveZipFullName ,null ,i_SourceLists ,false);
	}
    
    
    
    /**
     * 创建Zip的压缩文件(不忽略原文件异常)
     * 
     * 创建的Zip文件没有目录结构
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipRootPath            创建Zip目录结构的根目录（当为空时，创建的Zip文件没有目录结构）
     * @param i_SourceLists            要压缩的原文件列表
     * @throws IOException
     */
    public void createZip(String i_SaveZipFullName ,String i_ZipRootPath ,Collection<?> i_SourceLists) throws IOException
    {
        this.createZip(i_SaveZipFullName ,i_ZipRootPath ,i_SourceLists ,false);
    }
	
	
	
	/**
	 * 创建Zip的压缩文件
	 * 
	 * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipRootPath            创建Zip目录结构的根目录（当为空时，创建的Zip文件没有目录结构）
	 * @param i_SourceLists            要压缩的原文件列表
	 * @param i_IgnoreSourceFileError  是否忽略原文件异常(如原文件不存、或不可读等异常)
	 * @throws IOException
	 */
	public void createZip(String i_SaveZipFullName ,String i_ZipRootPath ,Collection<?> i_SourceLists ,boolean i_IgnoreSourceFileError) throws IOException
	{
		if ( Help.isNull(i_SaveZipFullName) )
		{
			throw new NullPointerException("Save full name is null.");
		}
		
		if ( i_SourceLists == null || i_SourceLists.size() <= 0 )
		{
			throw new NullPointerException("Zip source files[" + i_SaveZipFullName + "] list is null.");
		}
		
		
		File v_SaveZipFile = new File(i_SaveZipFullName);
		if ( v_SaveZipFile.exists() )
		{
			if ( this.isOverWrite )
			{
				boolean v_Result = v_SaveZipFile.delete();
				
				if ( !v_Result )
				{
					v_SaveZipFile = null;
					throw new IOException("Delete target file[" + i_SaveZipFullName + "] exception.");
				}
			}
			else
			{
				v_SaveZipFile = null;
				throw new IOException("Target[" + i_SaveZipFullName + "] is exists.");
			}
		}
		
		
		FileInputStream       v_SourceInput = null;
		FileOutputStream      v_ZipOutput   = new FileOutputStream(v_SaveZipFile);
		ZipOutputStream       v_ZipWriter   = new ZipOutputStream(v_ZipOutput ,Charset.forName(this.charEncoding));
		Iterator<?>           v_IterSources = i_SourceLists.iterator();
		DefaultCreateZipEvent v_Event       = new DefaultCreateZipEvent(this ,i_SourceLists.size());
		boolean               v_IsContinue  = true;
        String                v_ZipRootPath = Help.NVL(i_ZipRootPath).trim().toLowerCase();
        int                   v_ReadIndex   = 0;
        
        if ( !v_ZipRootPath.endsWith(Help.getSysPathSeparator()) )
        {
            v_ZipRootPath = v_ZipRootPath + Help.getSysPathSeparator();
        }
        
		try
		{
			v_IsContinue = this.fireCreateZipBeforeListener(v_Event);
			
			while ( v_IsContinue && v_IterSources.hasNext() )
			{
				String v_SourceFileFullName = v_IterSources.next().toString();
				File   v_SourceFile         = new File(v_SourceFileFullName);
				
				if ( !v_SourceFile.exists() )
				{
					if ( !i_IgnoreSourceFileError )
					{
						throw new VerifyError(v_SourceFileFullName + " is not exists.");
					}
					else
					{
						// 某一个原文件不存时，同时忽略原文件错误时，进行下一个原文件的压缩
						v_Event.setIgnoreErrorSize(v_Event.getIgnoreErrorSize() + 1);
						continue;
					}
				}
				
				if ( !v_SourceFile.canRead() )
				{
					if ( !i_IgnoreSourceFileError )
					{
						throw new VerifyError(v_SourceFileFullName + " can not read.");
					}
					else
					{
						// 某一个原文件不可读取时，同时忽略原文件错误时，进行下一个原文件的压缩
						v_Event.setIgnoreErrorSize(v_Event.getIgnoreErrorSize() + 1);
						continue;
					}
				}
				
				
				// 压缩某一个具体文件
				long                  v_SourceLen   = v_SourceFile.length();
				long                  v_WriteIndex  = 0;
				byte []               v_ByteBuffer  = new byte[bufferSize];
				DefaultCreateZipEvent v_PerSource   = new DefaultCreateZipEvent(this ,v_SourceLen);
				ZipEntry              v_ZipEntry    = null;
				String                v_DirFlag     = v_SourceFile.isDirectory() ? "/" : "";
				
				v_Event.setPerSource(v_PerSource);
				v_IsContinue = this.fireCreatingZipListener(v_Event);
				
                if ( Help.isNull(i_ZipRootPath) )
                {
                    v_ZipEntry = new ZipEntry(v_SourceFile.getName() + v_DirFlag);
                }
                else
                {
                    String v_LowerFullName = v_SourceFile.getAbsolutePath().toLowerCase();
                    if ( v_LowerFullName.startsWith(v_ZipRootPath) )
                    {
                        v_ZipEntry = new ZipEntry(v_SourceFile.getAbsolutePath().substring(v_ZipRootPath.length()) + v_DirFlag);
                    }
                    else
                    {
                        v_ZipEntry = new ZipEntry(v_SourceFile.getAbsolutePath() + v_DirFlag);
                    }
                }
                
                v_ZipEntry.setTime(v_SourceFile.lastModified()); // 保留文件或目录的修改时间
                v_ZipWriter.putNextEntry(v_ZipEntry);
                
                if ( !v_SourceFile.isDirectory() )
                {
                    v_SourceInput = new FileInputStream(v_SourceFile);
                    v_ReadIndex   = v_SourceInput.read(v_ByteBuffer);
                    
                    while ( v_IsContinue && v_ReadIndex >= 1 )
                    {
                        v_ZipWriter.write(v_ByteBuffer ,0 ,v_ReadIndex);
                        v_ZipWriter.flush();
                        
                        v_WriteIndex += v_ReadIndex;
                        
                        v_PerSource.setCompleteSize(v_WriteIndex);
                        v_Event.setPerSource(v_PerSource);
                        v_IsContinue = this.fireCreatingZipListener(v_Event);
                        v_ReadIndex  = v_SourceInput.read(v_ByteBuffer);
                    }
                    
                    // 释放每个压缩过的资源
                    try
                    {
                        v_SourceInput.close();
                    }
                    catch (Exception exce)
                    {
                        // Nothing.
                    }
                    
                    v_SourceInput = null;
    				v_SourceFile  = null;
                }
                else
                {
                    v_ZipWriter.closeEntry();
                }
			}
			
			v_Event.setSucceedFinish();
		}
		catch (Exception exce)
		{
			v_Event.setEndTime();
			throw new IOException(exce.getMessage());
		}
		finally
		{
		    if ( v_SourceInput != null )
            {
    		    try
                {
                    v_SourceInput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_SourceInput = null;
            }
            
		    if ( v_ZipWriter != null )
		    {
    			try
    			{
    				v_ZipWriter.close();
    			}
    			catch (Exception exce)
    			{
    				// Nothing.
    			}
    			
    			v_ZipWriter = null;
		    }
		    
		    if ( v_ZipOutput != null )
            {
                try
                {
                    v_ZipOutput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_ZipOutput = null;
            }
		    
			this.fireCreateZipAfterListener(v_Event);
		}

	}
	
	
	
	
	/**
	 * 解压缩
	 * 
	 * 2018-03-13  修复：解压时用出现解压不完整的问题。
	 * 2018-03-16  修复：当Window环境下打包的压缩包，在Linux环境解压时，因路径符不同而造成错误。
	 * 2018-04-09  添加：从压缩包中读取文件的最后修改时间
	 * 
	 * @param i_ZipFullName       压缩包文件
	 * @param i_SaveDir           保存解压目录
	 * @param i_IgnoreUnZipError  是否忽略解压缩过程中的异常(如解压包中的每个文件已存在在保存目录中: 1.无法删除的情况; 2.不允许重复的情况。)  
	 * @throws IOException
	 */
	public void UnCompressZip(String i_ZipFullName ,String i_SaveDir ,boolean i_IgnoreUnZipError) throws IOException
	{
		if ( Help.isNull(i_ZipFullName) )
		{
			throw new NullPointerException("Zip full name is null.");
		}
		
		if ( Help.isNull(i_SaveDir) )
		{
			throw new NullPointerException("Zip save directory is null.");
		}
		
		
		File v_ZipFile = new File(i_ZipFullName);
		if ( !v_ZipFile.exists() )
		{
			v_ZipFile = null;
			throw new IOException("Zip file[" + i_ZipFullName + "] is not exists.");
		}
		
		if ( !v_ZipFile.isFile() )
		{
			v_ZipFile = null;
			throw new IOException("Zip[" + i_ZipFullName + "] is not file.");
		}
		
		if ( !v_ZipFile.canRead() )
		{
			v_ZipFile = null;
			throw new IOException("Zip[" + i_ZipFullName + "] can not read.");
		}
		
		
		File v_SaveDirFile = new File(i_SaveDir);
		if ( !v_SaveDirFile.exists() )
		{
			boolean v_Result = v_SaveDirFile.mkdirs();
			
			if ( !v_Result )
			{
				v_SaveDirFile = null;
				throw new IOException("Create save dir[" + i_SaveDir + "] exception.");
			}
		}
		
		
		@SuppressWarnings("resource")
        ZipFile                   v_ZipFileObject = new ZipFile(v_ZipFile);
		Enumeration<?>            v_ZipEntries    = v_ZipFileObject.entries();
		boolean                   v_IsContinue    = true;
		long                      v_TotalSize     = v_ZipFile.length();
		DefaultUnCompressZipEvent v_Event         = new DefaultUnCompressZipEvent(this ,v_TotalSize);
		
		v_IsContinue = this.fireUnCompressZipBeforeListener(v_Event);
		
		for ( ;v_IsContinue && v_ZipEntries.hasMoreElements(); )
		{
			ZipEntry v_ZipEntry       = (ZipEntry)v_ZipEntries.nextElement();
			// 2018-03-16  修复：当Window环境下打包的压缩包，在Linux环境解压时，因路径符不同而造成错误。
			String   v_TargetFullName = v_SaveDirFile.getAbsolutePath() + Help.getSysPathSeparator() + StringHelp.replaceAll(v_ZipEntry.getName() ,new String[]{"\\" ,"/"} ,new String[]{Help.getSysPathSeparator()}); 
			File     v_TargetFile     = new File(v_TargetFullName);
			
			if ( v_TargetFile.exists() )
			{
				// 允许重写则删除
				if ( this.isOverWrite() )
				{
					if ( !v_TargetFile.delete() )
					{
						if ( i_IgnoreUnZipError )
						{
							continue;
						}
						else
						{
							v_Event.setEndTime();
							this.fireUnCompressZipAfterListener(v_Event);
							throw new IOException("Delete target file[" + v_TargetFullName + "] exception.");
						}
					}
				}
				// 不允许重写的情况
				else
				{
					if ( i_IgnoreUnZipError )
					{
						continue;
					}
					else
					{
						v_Event.setEndTime();
						this.fireUnCompressZipAfterListener(v_Event);
						throw new IOException("Target file[" + v_TargetFullName + "] is exists.");
					}
				}
			}
			else
			{
			    if ( v_ZipEntry.isDirectory() )
			    {
			        if ( !v_TargetFile.exists() )
                    {
                        boolean v_Result = v_TargetFile.mkdirs();
                        
                        if ( !v_Result )
                        {
                            v_SaveDirFile = null;
                            throw new IOException("Create save dir[" + v_TargetFile.toString() + "] exception.");
                        }
                    }
			        
			        continue;
			    }
			    else
			    {
    			    if ( !v_TargetFile.getParentFile().exists() )
    			    {
    			        boolean v_Result = v_TargetFile.getParentFile().mkdirs();
    		            
    		            if ( !v_Result )
    		            {
    		                v_SaveDirFile = null;
    		                throw new IOException("Create save dir[" + v_TargetFile.getParent() + "] exception.");
    		            }
    			    }
			    }
			}
			
			InputStream               v_SourceInput  = v_ZipFileObject.getInputStream(v_ZipEntry);
			OutputStream              v_TargetOutput = new FileOutputStream(v_TargetFile);
			long                      v_SourceLen    = v_ZipEntry.getSize();
			long                      v_WriteIndex   = 0;
			byte []                   v_ByteBuffer   = new byte[bufferSize];
			DefaultUnCompressZipEvent v_PerSource    = new DefaultUnCompressZipEvent(this ,v_SourceLen);
			
			try
			{
				v_Event.setPerSource(v_PerSource);
				v_IsContinue = this.fireUnCompressZipListener(v_Event);
				
				while ( v_IsContinue )
				{
				    // 2018-03-13 修复：解压时用出现解压不完整的问题。
				    int v_ReadSize = v_SourceInput.read(v_ByteBuffer);
				    if ( v_ReadSize <= 0  )
				    {
				        v_PerSource.setSucceedFinish();
                        v_Event.setPerSource(v_PerSource);
                        v_IsContinue = this.fireUnCompressZipListener(v_Event);
				        break;
				    }
				    
				    v_TargetOutput.write(v_ByteBuffer ,0 ,v_ReadSize);
                    v_TargetOutput.flush();
                    
                    v_WriteIndex += v_ReadSize;
				    
                    v_PerSource.setCompleteSize(v_WriteIndex);
                    v_Event.setPerSource(v_PerSource);
                    v_IsContinue = this.fireUnCompressZipListener(v_Event);
				}
			}
			catch (Exception exce)
			{
				v_PerSource.setEndTime();
				throw new IOException(exce.getMessage());
			}
			finally
			{
				try
				{
					v_SourceInput.close();
				}
				catch (Exception exce)
				{
					// Nothing.
				}
				
				try
				{
					v_TargetOutput.close();
				}
				catch (Exception exce)
				{
					// Nothing.
				}
				
				// 设置文件最后时间
				v_TargetFile.setLastModified(v_ZipEntry.getTime());
				v_SourceInput  = null;
				v_TargetOutput = null;
				v_TargetFile   = null;
			}
		}
		
		v_Event.setSucceedFinish();
		this.fireUnCompressZipAfterListener(v_Event);
		
		v_ZipEntries = null;
		v_ZipFileObject.close();
		v_ZipFileObject = null;
		v_SaveDirFile   = null;
		v_ZipFile       = null;
	}
	
	
	
	/**
	 * 解压缩。并文件内容以字符串返回
	 * 
	 * 没有解压进度事件
	 * 
	 * @param i_ZipURL        压缩包文件
	 * @param i_ReadFileName  读取压缩包中的哪个文件(不区分大小匹配)
	 * @param i_CharEncoding  文件的编码
	 * @throws IOException
	 */
	public String UnCompressZip(URL i_ZipURL ,String i_ReadFileName ,String i_CharEncoding) throws Exception
	{
		if ( i_ZipURL == null )
		{
			throw new NullPointerException("Zip URL is null.");
		}
		
		File v_File = new File(i_ZipURL.toString());
		
		if ( !v_File.exists() )
		{
			v_File = new File(i_ZipURL.getFile());
		}
		
		return UnCompressZip(v_File ,i_ReadFileName ,i_CharEncoding);
	}
	
	
	
	/**
	 * 解压缩。并文件内容以字符串返回
	 * 
	 * 没有解压进度事件
	 * 
	 * @param i_ZipFile       压缩包文件
	 * @param i_ReadFileName  读取压缩包中的哪个文件(不区分大小匹配)
	 * @param i_CharEncoding  文件的编码
	 * @throws IOException
	 */
	public String UnCompressZip(File i_ZipFile ,String i_ReadFileName ,String i_CharEncoding) throws Exception
	{
		if ( i_ZipFile == null )
		{
			throw new NullPointerException("Zip file is null.");
		}
		
		File v_ZipFile = i_ZipFile;
		if ( !v_ZipFile.exists() )
		{
			v_ZipFile = null;
			throw new IOException("Zip file[" + i_ZipFile.toString() + "] is not exists.");
		}
		
		if ( !v_ZipFile.isFile() )
		{
			v_ZipFile = null;
			throw new IOException("Zip[" + i_ZipFile.toString() + "] is not file.");
		}
		
		if ( !v_ZipFile.canRead() )
		{
			v_ZipFile = null;
			throw new IOException("Zip[" + i_ZipFile.toString() + "] can not read.");
		}
		
		
		
		ZipFile        v_ZipFileObject = new ZipFile(v_ZipFile);
		Enumeration<?> v_ZipEntries    = v_ZipFileObject.entries();
		StringBuilder  v_Buffer        = new StringBuilder();
		
		
		for ( ;v_ZipEntries.hasMoreElements(); )
		{
			ZipEntry v_ZipEntry = (ZipEntry)v_ZipEntries.nextElement();
			
			if ( !v_ZipEntry.isDirectory() && i_ReadFileName.equalsIgnoreCase(v_ZipEntry.getName()) && v_ZipEntry.getSize() > 0 )
			{
				BufferedReader v_SourceInput = null;
				try
				{
					v_SourceInput = new BufferedReader(new InputStreamReader(v_ZipFileObject.getInputStream(v_ZipEntry) ,i_CharEncoding));
					String v_Line = "";
					while ( (v_Line = v_SourceInput.readLine()) != null )
					{
						v_Buffer.append(v_Line).append(Help.getSysLineSeparator());
					}
				}
				catch (Exception exce)
				{
					throw new IOException(exce.getMessage());
				}
				finally
				{
					try
					{
						v_SourceInput.close();
					}
					catch (Exception exce)
					{
						// Nothing.
					}
					
					v_SourceInput  = null;
				}
			}
		}
		
		v_ZipEntries = null;
		v_ZipFileObject.close();
		v_ZipFileObject = null;
		v_ZipFile       = null;
		
		return v_Buffer.toString();
	}
	
	
	
	
	/**
     * 创建Zip的压缩文件 (压缩级别为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,true);
    }
    
    
    
    /**
     * 创建Zip的压缩文件 (压缩级别为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_HaveRootName           是否须要根目录节点
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,boolean i_HaveRootName) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,i_HaveRootName ,null ,Zip4jConstants.DEFLATE_LEVEL_NORMAL);
    }
    
    
    
    /**
     * 创建Zip的压缩文件 (压缩级别为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_Password               加密密码
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,String i_Password) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,true ,i_Password);
    }
    
    
    
    /**
     * 创建Zip的压缩文件 (压缩级别为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_HaveRootName           是否须要根目录节点
     * @param i_Password               加密密码
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,boolean i_HaveRootName ,String i_Password) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,i_HaveRootName ,i_Password ,Zip4jConstants.DEFLATE_LEVEL_NORMAL);
    }
    
    
    
    /**
     * 创建Zip的压缩文件 
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_CompressionLevel       压缩级别(默认为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,int i_CompressionLevel) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,true ,i_CompressionLevel);
    }
    
    
    
    /**
     * 创建Zip的压缩文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_HaveRootName           是否须要根目录节点
     * @param i_CompressionLevel       压缩级别(默认为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,boolean i_HaveRootName ,int i_CompressionLevel) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,i_HaveRootName ,null ,i_CompressionLevel);
    }
    
    
    
    /**
     * 创建Zip的压缩文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_Password               加密密码
     * @param i_CompressionLevel       压缩级别(默认为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,String i_Password ,int i_CompressionLevel) throws IOException
    {
        this.createZip4j(i_SaveZipFullName ,i_ZipFile ,true ,i_Password ,i_CompressionLevel);
    }
    
    
    
    /**
     * 创建Zip的压缩文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFullName        保存Zip压缩文件全名称
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_HaveRootName           是否须要根目录节点
     * @param i_Password               加密密码
     * @param i_CompressionLevel       压缩级别(默认为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * @throws IOException
     */
    public void createZip4j(String i_SaveZipFullName ,String i_ZipFile ,boolean i_HaveRootName ,String i_Password ,int i_CompressionLevel) throws IOException
    {
        if ( Help.isNull(i_ZipFile) )
        {
            throw new NullPointerException("ZipFile is null.");
        }
        
        if ( Help.isNull(i_SaveZipFullName) )
        {
            throw new NullPointerException("Save full name is null.");
        }
        
        this.createZip4j(new File(i_SaveZipFullName) ,new File(i_ZipFile) ,i_HaveRootName ,i_Password ,i_CompressionLevel);
    }
    
    
    
    /**
     * 创建Zip的压缩文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     * 
     * @param i_SaveZipFile            保存Zip压缩文件
     * @param i_ZipFile                要压缩的原文件或目录
     * @param i_HaveRootName           是否须要根目录节点
     * @param i_Password               加密密码
     * @param i_CompressionLevel       压缩级别(默认为：Zip4jConstants.DEFLATE_LEVEL_NORMAL)
     * @throws IOException
     */
    public void createZip4j(File i_SaveZipFile ,File i_ZipFile ,boolean i_HaveRootName ,String i_Password ,int i_CompressionLevel) throws IOException
    {
        if ( !i_ZipFile.exists() )
        {
            throw new IOException("ZipFile[" + i_ZipFile + "] is not exists.");
        }
        
        if ( i_SaveZipFile.exists() )
        {
            if ( this.isOverWrite )
            {
                boolean v_Result = i_SaveZipFile.delete();
                
                if ( !v_Result )
                {
                    throw new IOException("Delete target file[" + i_SaveZipFile.toString() + "] exception.");
                }
            }
            else
            {
                throw new IOException("Target[" + i_SaveZipFile.toString() + "] is exists.");
            }
        }
        
        
        ZipParameters v_ZipParams = new ZipParameters(); 
        v_ZipParams.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);           // 压缩方式  
        v_ZipParams.setCompressionLevel( i_CompressionLevel);                    // 压缩级别  
        if ( i_Password != null && !"".equals(i_Password) ) 
        {  
            v_ZipParams.setEncryptFiles(true);  
            v_ZipParams.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD); // 加密方式  
            v_ZipParams.setPassword(i_Password.toCharArray());  
        }
        
        
        try
        {
            net.lingala.zip4j.core.ZipFile v_ZipWriter = new net.lingala.zip4j.core.ZipFile(i_SaveZipFile);  
            v_ZipWriter.setFileNameCharset(this.charEncoding.toUpperCase().replace("UTF-8" ,"UTF8"));
            
            if ( i_ZipFile.isDirectory() )
            {
                if ( i_HaveRootName )
                {
                    v_ZipWriter.addFolder(i_ZipFile ,v_ZipParams);
                }
                else
                {
                    File [] v_ChildFiles = i_ZipFile.listFiles();
                    if ( !Help.isNull(v_ChildFiles) )
                    {
                        for (File v_ChildFile : v_ChildFiles)
                        {
                            if ( v_ChildFile.isDirectory() )
                            {
                                v_ZipWriter.addFolder(v_ChildFile ,v_ZipParams);
                            }
                            else
                            {
                                v_ZipWriter.addFile  (v_ChildFile ,v_ZipParams);
                            }
                        }
                    }
                }
            }
            else
            {
                v_ZipWriter.addFile(  i_ZipFile ,v_ZipParams);
            }
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
    }
    
    
    
    /**
     * 解压缩
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     *
     * @param i_ZipFile        压缩包文件
     * @param i_UnCompressDir  解压目录。当不存在时，自动创建
     * @throws IOException
     */
    public void UnCompressZip4j(String i_ZipFile ,String i_UnCompressDir) throws IOException 
    {
        this.UnCompressZip4j(i_ZipFile ,i_UnCompressDir ,null);
    }
    
    
    
    /**
     * 解压缩
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     *
     * @param i_ZipFile        压缩包文件
     * @param i_UnCompressDir  解压目录。当不存在时，自动创建
     * @param i_Passwd         加密密码
     * @throws IOException
     */
    public void UnCompressZip4j(String i_ZipFile ,String i_UnCompressDir ,String i_Passwd) throws IOException 
    {
        if ( Help.isNull(i_ZipFile) )
        {
            throw new NullPointerException("ZipFile is null.");
        }
        
        this.UnCompressZip4j(new File(i_ZipFile) ,i_UnCompressDir ,i_Passwd);
    }
    
    
    
    /**
     * 解压缩
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     *
     * @param i_ZipFile        压缩包文件
     * @param i_UnCompressDir  解压目录。当不存在时，自动创建
     * @throws IOException
     */
    public void UnCompressZip4j(File i_ZipFile ,String i_UnCompressDir) throws IOException 
    {
        this.UnCompressZip4j(i_ZipFile ,i_UnCompressDir ,null);
    }
    
    
    
    /**
     * 解压缩
     * 
     * @author      ZhengWei(HY)
     * @createDate  2018-04-09
     * @version     v1.0
     *
     * @param i_ZipFile        压缩包文件
     * @param i_UnCompressDir  解压目录。当不存在时，自动创建
     * @param i_Passwd         加密密码
     * @throws IOException
     */
    public void UnCompressZip4j(File i_ZipFile ,String i_UnCompressDir ,String i_Passwd) throws IOException 
    {
        if ( Help.isNull(i_UnCompressDir) )
        {
            throw new NullPointerException("UnCompressDir is null.");
        }
        
        File v_UnCompressDir = new File(i_UnCompressDir.trim());
        if ( !v_UnCompressDir.exists() || !v_UnCompressDir.isDirectory() ) 
        {    
            v_UnCompressDir.mkdirs();    
        }
        
        
        net.lingala.zip4j.core.ZipFile v_ZipFile = null;
        try
        {
            v_ZipFile = new net.lingala.zip4j.core.ZipFile(i_ZipFile); 
            v_ZipFile.setFileNameCharset(this.charEncoding.toUpperCase().replace("UTF-8" ,"UTF8"));
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
        
        if (!v_ZipFile.isValidZipFile() ) 
        {   
            // 验证文件是否合法，是否存在、是否为zip文件、是否被损坏等    
            throw new IOException("ZipFile[" + i_ZipFile.toString() + "] is not valid ,may be damage.");
        }
        
        try
        {
            if ( v_ZipFile.isEncrypted() ) 
            {    
                if ( i_Passwd == null || "".equals(i_Passwd) )
                {
                    throw new IOException("ZipFile[" + i_ZipFile.toString() + "] is the password required for encryption.");
                }
                else
                {
                    // 设置密码    
                    v_ZipFile.setPassword(i_Passwd.toCharArray());  
                }
            }
            
            UnzipParameters v_UnzipParameters = new UnzipParameters();
            
            v_UnzipParameters.setIgnoreAllFileAttributes(    false);
            v_UnzipParameters.setIgnoreDateTimeAttributes(   false);
            v_UnzipParameters.setIgnoreArchiveFileAttribute( false);
            v_UnzipParameters.setIgnoreHiddenFileAttribute(  false);
            v_UnzipParameters.setIgnoreReadOnlyFileAttribute(false);
            v_UnzipParameters.setIgnoreSystemFileAttribute(  false);
            
            v_ZipFile.extractAll(i_UnCompressDir.trim() ,v_UnzipParameters); 
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
    }
    
    
    
    /**
     * 创建XD文件
     * 
     * 注：与创建Zip文件一样。不同是：对每个文件都进行了加密(转码)。
     * 
     * @param i_SaveXDFullName         保存XD文件全名称
     * @param i_XDRootPath             创建XD目录结构的根目录（当为空时，创建的XD文件没有目录结构）
     * @param i_SourceLists            要压缩的原文件列表
     * @param i_IgnoreSourceFileError  是否忽略原文件异常(如原文件不存、或不可读等异常)
     * @throws IOException
     */
    public void createXD(String i_SaveXDFullName ,String i_XDRootPath ,Collection<?> i_SourceLists ,boolean i_IgnoreSourceFileError) throws IOException
    {
        if ( Help.isNull(i_SaveXDFullName) )
        {
            throw new NullPointerException("Save full name is null.");
        }
        
        if ( i_SourceLists == null || i_SourceLists.size() <= 0 )
        {
            throw new NullPointerException("source files[" + i_SaveXDFullName + "] list is null.");
        }
        
        
        File v_SaveXDFile = new File(i_SaveXDFullName);
        if ( v_SaveXDFile.exists() )
        {
            if ( this.isOverWrite )
            {
                boolean v_Result = v_SaveXDFile.delete();
                
                if ( !v_Result )
                {
                    v_SaveXDFile = null;
                    throw new IOException("Delete target file[" + i_SaveXDFullName + "] exception.");
                }
            }
            else
            {
                v_SaveXDFile = null;
                throw new IOException("Target[" + i_SaveXDFullName + "] is exists.");
            }
        }
        
        
        FileOutputStream      v_XDOutput    = new FileOutputStream(v_SaveXDFile);
        ZipOutputStream       v_XDWriter    = new ZipOutputStream(v_XDOutput);
        Iterator<?>           v_IterSources = i_SourceLists.iterator();
        String                v_XDRootPath  = Help.NVL(i_XDRootPath).trim().toLowerCase();
        
        if ( !v_XDRootPath.endsWith(Help.getSysPathSeparator()) )
        {
            v_XDRootPath = v_XDRootPath + Help.getSysPathSeparator();
        }
        
        try
        {
            while ( v_IterSources.hasNext() )
            {
                String v_SourceFileFullName = v_IterSources.next().toString();
                File   v_SourceFile         = new File(v_SourceFileFullName);
                
                if ( !v_SourceFile.exists() )
                {
                    if ( !i_IgnoreSourceFileError )
                    {
                        throw new VerifyError(v_SourceFileFullName + " is not exists.");
                    }
                    else
                    {
                        // 某一个原文件不存时，同时忽略原文件错误时，进行下一个原文件的压缩
                        continue;
                    }
                }
                
                if ( !v_SourceFile.canRead() )
                {
                    if ( !i_IgnoreSourceFileError )
                    {
                        throw new VerifyError(v_SourceFileFullName + " can not read.");
                    }
                    else
                    {
                        // 某一个原文件不可读取时，同时忽略原文件错误时，进行下一个原文件的压缩
                        continue;
                    }
                }
                
                
                // 压缩某一个具体文件
                FileInputStream       v_SourceInput = new FileInputStream(v_SourceFile);
                long                  v_SourceLen   = v_SourceFile.length();
                long                  v_WriteIndex  = 0;
                byte []               v_ByteBuffer  = new byte[bufferSize];
                
                
                if ( Help.isNull(i_XDRootPath) )
                {
                    v_XDWriter.putNextEntry(new ZipEntry(v_SourceFile.getName()));
                }
                else
                {
                    String v_LowerFullName = v_SourceFile.getAbsolutePath().toLowerCase();
                    if ( v_LowerFullName.startsWith(v_XDRootPath) )
                    {
                        v_XDWriter.putNextEntry(new ZipEntry(v_SourceFile.getAbsolutePath().substring(v_XDRootPath.length())));
                    }
                    else
                    {
                        v_XDWriter.putNextEntry(new ZipEntry(v_SourceFile.getAbsolutePath()));
                    }
                }
                
                while ( true )
                {
                    if ( v_WriteIndex + bufferSize <= v_SourceLen )
                    {
                        v_SourceInput.read(v_ByteBuffer);
                        v_XDWriter.write(ByteHelp.xorMV(v_ByteBuffer));
                        v_XDWriter.flush();
                        
                        v_WriteIndex += bufferSize;
                    }
                    else
                    {
                        v_ByteBuffer = new byte[(int)(v_SourceLen - v_WriteIndex)];
                        
                        v_SourceInput.read(v_ByteBuffer);
                        v_XDWriter.write(ByteHelp.xorMV(v_ByteBuffer));
                        v_XDWriter.flush();
                        
                        break;
                    }
                }
                
                
                // 释放每个压缩过的资源
                try
                {
                    v_SourceInput.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_SourceInput = null;
                v_SourceFile  = null;
            
            }
        }
        catch (Exception exce)
        {
            throw new IOException(exce.getMessage());
        }
        finally
        {
            try
            {
                v_XDWriter.close();
            }
            catch (Exception exce)
            {
                // Nothing.
            }
            
            v_XDWriter = null;
            v_XDOutput = null;
        }

    }
    
    
    
    /**
     * 释放XD资源。并文件内容以字符串返回
     * 
     * 注：与解压Zip文件一样。不同是：对每个文件都进行了加密(转码)。
     * 
     * @param i_XDInput       XD资源文件流
     * @param i_ReadName      读取XD资源中的哪个文件(不区分大小匹配)
     * @param i_CharEncoding  文件的编码
     * @throws IOException
     */
    public String getXD(InputStream i_XDInput,String i_ReadName ,String i_CharEncoding) throws Exception
    {
        File v_DatFile = new File(Help.getSysTempPath() + Help.getSysPathSeparator() + Date.getNowTime().getFullMilli_ID());
        
        try
        {
            if ( i_XDInput == null )
            {
                throw new NullPointerException("XD Input is null.");
            }
            
            FileHelp v_FH = new FileHelp();
            v_FH.setOverWrite(true);
            
            v_FH.copyFile(i_XDInput ,v_DatFile);
            
            return getXD(v_DatFile ,i_ReadName ,i_CharEncoding);
        }
        catch (Exception exce)
        {
            throw exce;
        }
        finally
        {
            if ( v_DatFile != null && v_DatFile.exists() )
            {
                try
                {
                    v_DatFile.delete();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
        }
    }
    
    
    
    /**
     * 释放XD资源。并文件内容以字符串返回
     * 
     * 注：与解压Zip文件一样。不同是：对每个文件都进行了加密(转码)。
     * 
     * @param i_XDURL         XD资源文件
     * @param i_ReadName      读取XD资源中的哪个文件(不区分大小匹配)
     * @param i_CharEncoding  文件的编码
     * @throws IOException
     */
    public String getXD(URL i_XDURL ,String i_ReadName ,String i_CharEncoding) throws Exception
    {
        if ( i_XDURL == null )
        {
            throw new NullPointerException("XD URL is null.");
        }
        
        File v_File = new File(i_XDURL.toString());
        
        if ( !v_File.exists() )
        {
            v_File = new File(i_XDURL.getFile());
        }
        
        return getXD(v_File ,i_ReadName ,i_CharEncoding);
    }
    
    
    
    /**
     * 释放XD资源。并文件内容以字符串返回
     * 
     * 注：与解压Zip文件一样。不同是：对每个文件都进行了加密(转码)。
     * 
     * @param i_XDFullName    XD资源文件
     * @param i_ReadName      读取XD资源中的哪个文件(不区分大小匹配)
     * @param i_CharEncoding  文件的编码
     * @throws IOException
     */
    public String getXD(String i_XDFullName ,String i_ReadName ,String i_CharEncoding) throws Exception
    {
        if ( i_XDFullName == null )
        {
            throw new NullPointerException("XD full name is null.");
        }
        
        return getXD(new File(i_XDFullName) ,i_ReadName ,i_CharEncoding);
    }
    
    
    
    /**
     * 释放XD资源。并文件内容以字符串返回
     * 
     * 注：与解压Zip文件一样。不同是：对每个文件都进行了加密(转码)。
     * 
     * @param i_XDFile        XD资源文件
     * @param i_ReadName      读取XD资源中的哪个文件(不区分大小匹配)
     * @param i_CharEncoding  文件的编码
     * @throws IOException
     */
    public String getXD(File i_XDFile ,String i_ReadName ,String i_CharEncoding) throws Exception
    {
        if ( i_XDFile == null )
        {
            throw new NullPointerException("XD file is null.");
        }
        
        File v_XDFile = i_XDFile;
        if ( !v_XDFile.exists() )
        {
            v_XDFile = null;
            throw new IOException("XD file[" + i_XDFile.toString() + "] is not exists.");
        }
        
        if ( !v_XDFile.isFile() )
        {
            v_XDFile = null;
            throw new IOException("XD[" + i_XDFile.toString() + "] is not file.");
        }
        
        if ( !v_XDFile.canRead() )
        {
            v_XDFile = null;
            throw new IOException("XD[" + i_XDFile.toString() + "] can not read.");
        }
        
        
        
        ZipFile        v_XDFileObject  = new ZipFile(v_XDFile);
        Enumeration<?> v_XDEntries     = v_XDFileObject.entries();
        StringBuilder  v_Buffer        = new StringBuilder();
        
        
        for ( ;v_XDEntries.hasMoreElements(); )
        {
            ZipEntry v_XDEntry = (ZipEntry)v_XDEntries.nextElement();
            
            if ( !v_XDEntry.isDirectory() && i_ReadName.equalsIgnoreCase(v_XDEntry.getName()) && v_XDEntry.getSize() > 0 )
            {
                InputStream v_SourceInput = v_XDFileObject.getInputStream(v_XDEntry);
                byte []     v_ByteBuffer  = new byte[bufferSize];
                long        v_MaxLen      = v_XDEntry.getSize();
                long        v_ReadIndex   = 0;
                
                try
                {
                    while ( true )
                    {
                        if ( v_ReadIndex + bufferSize <= v_MaxLen )
                        {
                            v_SourceInput.read(v_ByteBuffer);
                            v_Buffer.append(new String(ByteHelp.xorMV(v_ByteBuffer) ,i_CharEncoding));
                            
                            v_ReadIndex += bufferSize;
                        }
                        else
                        {
                            v_ByteBuffer = new byte[(int)(v_MaxLen - v_ReadIndex)];
                            
                            v_SourceInput.read(v_ByteBuffer);
                            v_Buffer.append(new String(ByteHelp.xorMV(v_ByteBuffer) ,i_CharEncoding));
                            
                            break;
                        }
                    }
                }
                catch (Exception exce)
                {
                    throw new IOException(exce.getMessage());
                }
                finally
                {
                    try
                    {
                        v_SourceInput.close();
                    }
                    catch (Exception exce)
                    {
                        // Nothing.
                    }
                    
                    v_SourceInput  = null;
                }
            }
        }
        
        v_XDEntries    = null;
        v_XDFileObject.close();
        v_XDFileObject = null;
        v_XDFile       = null;
        
        return v_Buffer.toString();
    }
	
	
	
	public int getBufferSize() 
	{
		return bufferSize;
	}



	public void setBufferSize(int bufferSize) 
	{
		this.bufferSize = bufferSize;
	}



	public String getNewLine() 
	{
		return newLine;
	}

	

	public void setNewLine(String newLine)
	{
		this.newLine = newLine;
	}


	
	public boolean isOverWrite() 
	{
		return isOverWrite;
	}


	
	public void setOverWrite(boolean isOverWrite) 
	{
		this.isOverWrite = isOverWrite;
	}
	
    
	
    /**
     * 获取：是否追加写入数据
     */
    public boolean isAppend()
    {
        return isAppend;
    }
    

    
    /**
     * 设置：是否追加写入数据
     * 
     * @param isAppend 
     */
    public void setAppend(boolean isAppend)
    {
        this.isAppend = isAppend;
    }
    


    public String getCsvDataPrefix()
    {
        return Help.NVL(csvDataPrefix ,"");
    }
    
    
    
    public void setCsvDataPrefix(String csvDataPrefix)
    {
        this.csvDataPrefix = csvDataPrefix;
    }
    
    
    
    /**
     * 获取：数据包的超时时长（单位：秒）
     */
    public long getDataPacketTimeOut()
    {
        return dataPacketTimeOut;
    }
    

    
    /**
     * 设置：数据包的超时时长（单位：秒）
     * 
     * @param dataPacketTimeOut 
     */
    public void setDataPacketTimeOut(long dataPacketTimeOut)
    {
        this.dataPacketTimeOut = dataPacketTimeOut;
    }
    

    
    /**
     * 获取：编码类型
     */
    public String getCharEncoding()
    {
        return charEncoding;
    }
    

    
    /**
     * 设置：编码类型
     * 
     * @param charEncoding 
     */
    public void setCharEncoding(String charEncoding)
    {
        this.charEncoding = charEncoding;
    }


    
    /**
     * 获取：Http内容类型
     */
    public String getContentType()
    {
        return contentType;
    }
    

    
    /**
     * 设置：Http内容类型
     * 
     * @param contentType 
     */
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    
    
    
    /**
     * 获取：是否返回文件内容。
     * 
     * 用于getContent(...)系列方法，当isReturnContent为真时，getContent(...)方法将有返回值，否则返回值为null。
     * 
     * 一般isReturnContent为假时，与 readListener 监听器配合使用，可支持超大文件的读取。
     * 
     * 此为性能参数
     */
    public boolean isReturnContent()
    {
        return isReturnContent;
    }
    

    
    /**
     * 设置：是否返回文件内容。
     * 
     * 用于getContent(...)系列方法，当isReturnContent为真时，getContent(...)方法将有返回值，否则返回值为null。
     * 
     * 一般isReturnContent为假时，与 readListener 监听器配合使用，可支持超大文件的读取。
     * 
     * 此为性能参数
     * 
     * @param isReturnContent 
     */
    public void setReturnContent(boolean isReturnContent)
    {
        this.isReturnContent = isReturnContent;
    }
    


    protected void finalize() throws Throwable 
	{
		if ( this.fileCopyListeners != null )
		{
			this.fileCopyListeners.clear();
			this.fileCopyListeners = null;
		}
		
		if ( this.createCSVListeners != null )
		{
			this.createCSVListeners.clear();
			this.createCSVListeners = null;
		}
		
		if ( this.createZipListeners != null )
		{
			this.createZipListeners.clear();
			this.createZipListeners = null;
		}
		
		if ( this.unCompressZipListeners != null )
		{
			this.unCompressZipListeners.clear();
			this.unCompressZipListeners = null;
		}
		
		super.finalize();
	}
	
}
