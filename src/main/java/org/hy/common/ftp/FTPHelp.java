package org.hy.common.ftp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.net.ftp.FTPClient;

import org.hy.common.ByteHelp;
import org.hy.common.ExpireMap;
import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.file.FileDataPacket;
import org.hy.common.file.FileHelp;
import org.hy.common.ftp.event.DefaultFTPEvent;
import org.hy.common.ftp.event.FTPEvent;
import org.hy.common.ftp.event.FTPListener;





/**
 * FTP操作的辅助类
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-06-04
 *           V2.0  2020-05-20  添加：1. 支持文件流的上传
 *                             添加：2. 支持追加模式（断点续传）
 *                             添加：3. FileDataPacket 文件的数据包的上传（默认开启断点续传）
 *                             添加：4. 创建FTP目录。可连续创建多级目录
 *                             添加：5. 支持中文目录及中文文件
 *                                   
 */
public final class FTPHelp 
{
    /** 缓存大小 */
    private static final int $BufferSize     = 4 * 1024;
    
    /** 临时记录最新一次数据包信息 */
    private static final ExpireMap<String ,FileDataPacket> $DataPackets = new ExpireMap<String ,FileDataPacket>();
    
    
    
    private FTPInfo                    ftpInfo;
    
    private FTPClient                  ftpClient;
    
    /** 数据安全性。如果为真，将对上传的文件进行数据加密 */
    private boolean                    dataSafe;
    
    /** 自定义事件的监听器集合--文件拷贝 */
    private Collection<FTPListener>    ftpListeners;
    
    /** 数据包的超时时长（单位：秒） */
    private long                       dataPacketTimeOut = 10 * 60;
    
    
    
    /**
     * 构造器
     * 
     * @param i_FTPInfo
     */
    public FTPHelp(FTPInfo i_FTPInfo)
    {
        this.ftpInfo  = i_FTPInfo;
        this.dataSafe = false;
    }
    
    
    
    /**
     * 获取FTP基础信息（克隆的）
     * 
     * @return
     */
    public FTPInfo getFTPInfo()
    {
        return this.ftpInfo.clone();
    }
    
    
    
    /**
     * 连接 FTP 服务器。
     * 
     * @return  连接成功返回 null。 否则为异常信息。
     */
    public String connect()
    {
        if ( this.ftpInfo == null )
        {
            return "Fpt info is null.";
        }
        
        try 
        {
            if ( this.ftpClient != null )
            {
                this.close();
            }
            
            
            this.ftpClient = new FTPClient();
            
            
            this.ftpClient.setProxy(                       this.ftpInfo.getProxy());
            this.ftpClient.setDefaultTimeout(              this.ftpInfo.getDefaultTimeout());
            this.ftpClient.setConnectTimeout(              this.ftpInfo.getConnectTimeout());
            this.ftpClient.setControlKeepAliveReplyTimeout(this.ftpInfo.getControlKeepAliveReplyTimeout());
            this.ftpClient.setControlKeepAliveTimeout(     this.ftpInfo.getControlKeepAliveTimeout());
            this.ftpClient.setDataTimeout(                 this.ftpInfo.getDataTimeout());
            
            this.ftpClient.connect(this.ftpInfo.getIp()   ,this.ftpInfo.getPort());
            this.ftpClient.login(  this.ftpInfo.getUser() ,this.ftpInfo.getPassword());
            
            if ( this.ftpInfo.isLocalPassiveMode() )
            {
                this.ftpClient.enterLocalPassiveMode();
            }
            if ( this.ftpInfo.isRemotePassiveMode() )
            {
                this.ftpClient.enterRemotePassiveMode();
            }
            this.ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            
            this.ftpClient.setBufferSize(                  this.ftpInfo.getBufferSize());
            this.ftpClient.setReceiveBufferSize(           this.ftpInfo.getReceiveBufferSize());
            this.ftpClient.setReceieveDataSocketBufferSize(this.ftpInfo.getReceiveDataSocketBufferSize());
            this.ftpClient.setSendBufferSize(              this.ftpInfo.getSendBufferSize());
            this.ftpClient.setSendDataSocketBufferSize(    this.ftpInfo.getSendDataSocketBufferSize());
            
            
            this.ftpClient.setStrictMultilineParsing(      this.ftpInfo.isStrictMultilineParsing());
            this.ftpClient.setStrictReplyParsing(          this.ftpInfo.isStrictReplyParsing());
            this.ftpClient.setUseEPSVwithIPv4(             this.ftpInfo.isUseEPSVwithIPv4());
            this.ftpClient.setRemoteVerificationEnabled(   this.ftpInfo.isRemoteVerificationEnabled());
            
            this.ftpClient.setListHiddenFiles(             this.ftpInfo.getListHiddenFiles());
            this.ftpClient.setControlEncoding(             this.ftpInfo.getControlEncoding());
            this.ftpClient.setAutodetectUTF8(              this.ftpInfo.getAutodetectUTF8());
            this.ftpClient.setCharset(                     this.ftpInfo.getCharset());
            this.ftpClient.changeWorkingDirectory(         this.ftpInfo.getInitPath());
        } 
        catch (Exception exce) 
        {
            this.ftpClient = null;
            exce.printStackTrace();
            return exce.getMessage();
        }
        
        return null;
    }
    
    
    
    /**
     * 关闭与 FTP 服务间的连接服务
     */
    public void close()
    {
        if ( this.ftpClient != null )
        {
            try 
            {
                this.ftpClient.logout();
                this.ftpClient.disconnect();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
            finally
            {
                this.ftpClient = null;
            }
        }
    }
    
    
    
    /**
     * 下载文件
     * 
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_SaveFullName    保存文件的全路径(下载路径)
     * @return  下载成功返回 null 。否则返回异常信息
     */
    public String download(String i_RemoteFullName ,String i_SaveFullName)
    {
        return this.download(i_RemoteFullName ,i_SaveFullName ,0);
    }
    
    
    
    /**
     * 下载文件
     * 
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_SaveFullName    保存文件的全路径(下载路径)
     * @param i_FileSize        文件的大小（单位：Byte）。不自动获取远程文件的大小，而是由外在传参确定大小
     * @return  下载成功返回 null 。否则返回异常信息
     */
    public String download(String i_RemoteFullName ,String i_SaveFullName ,long i_FileSize)
    {
        if ( this.ftpClient == null )
        {
            return "Ftp Client is not connect.";
        }
        
        
        InputStream       v_Input          = null;
        DataInputStream   v_DataInput      = null;
        File              v_SaveFile       = null;
        FileOutputStream  v_SaveFileOutput = null;
        long              v_FTPingSize     = 0;
        DefaultFTPEvent   v_Event          = new DefaultFTPEvent(this ,i_FileSize);
        boolean           v_IsContinue     = true;
        
        v_Event.setActionType(2);
        
        try 
        {
            v_Input          = this.ftpClient.retrieveFileStream(new String(i_RemoteFullName.getBytes("GBK") ,"ISO-8859-1"));
            v_DataInput      = new DataInputStream(v_Input);
            v_SaveFile       = new File(i_SaveFullName);
            v_SaveFileOutput = new FileOutputStream(v_SaveFile);
            byte [] v_Buffer = new byte[$BufferSize];
            int     v_RSize  = 0;
            
            
            v_IsContinue = this.fireFTPBeforeListener(v_Event);
            
            if ( this.dataSafe )
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_SaveFileOutput.write(ByteHelp.xorMV(v_Buffer ,0 ,v_RSize) ,0 ,v_RSize);
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                } 
            }
            else
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_SaveFileOutput.write(v_Buffer ,0 ,v_RSize);
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                }   
            }
            
            v_SaveFileOutput.flush();
            v_SaveFileOutput.close();
            v_SaveFileOutput = null;
            
            // 此语句十分关键，如果没有此句，只能下载第一个文件，其后的所有文件都将失败
            this.ftpClient.completePendingCommand();
            
            v_Event.setSucceedFinish();
        } 
        catch (Exception e) 
        {
            v_Event.setEndTime();
            return e.toString();
        }
        finally
        {
            if ( v_SaveFileOutput != null )
            {
                try 
                {
                    v_SaveFileOutput.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_SaveFileOutput = null;
            }
            
            if ( v_DataInput != null )
            {
                try 
                {
                    v_DataInput.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_DataInput = null;
            }
            
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception e)
                {
                    // Nothing.
                }
                
                v_Input = null;
            }
            
            this.fireFTPAfterListener(v_Event);
        }
        
        return null;
    }
    
    
    
    /**
     * 下载文件(返回文件内容)
     * 
     * 即，加载文件内容在内存中
     * 
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_SaveFullName    保存文件的全路径(下载路径)
     * @return  下载成功返回文件内容
     */
    public String download(String i_RemoteFullName)
    {
        return this.download(i_RemoteFullName ,0);
    }
    
    
    
    /**
     * 下载文件(返回文件内容)
     * 
     * 即，加载文件内容在内存中
     * 
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_FileSize        文件的大小（单位：Byte）。不自动获取远程文件的大小，而是由外在传参确定大小
     * @return  下载成功返回文件内容
     */
    public String download(String i_RemoteFullName ,long i_FileSize)
    {
        if ( this.ftpClient == null )
        {
            return "Ftp Client is not connect.";
        }
        
        
        InputStream       v_Input          = null;
        DataInputStream   v_DataInput      = null;
        long              v_FTPingSize     = 0;
        StringBuilder     v_OutBuffer      = new StringBuilder();
        DefaultFTPEvent   v_Event          = new DefaultFTPEvent(this ,i_FileSize);
        boolean           v_IsContinue     = true;
        
        v_Event.setActionType(2);
        
        try 
        {
            v_Input          = this.ftpClient.retrieveFileStream(new String(i_RemoteFullName.getBytes("GBK") ,"ISO-8859-1"));
            v_DataInput      = new DataInputStream(v_Input);
            byte [] v_Buffer = new byte[$BufferSize];
            int     v_RSize  = 0;
            
            
            v_IsContinue = this.fireFTPBeforeListener(v_Event);
            
            if ( this.dataSafe )
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_OutBuffer.append(StringHelp.bytesToHex(ByteHelp.xorMV(v_Buffer ,0 ,v_RSize) ,0 ,v_RSize));
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                } 
            }
            else
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_OutBuffer.append(StringHelp.bytesToHex(v_Buffer ,0 ,v_RSize));
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                }   
            }
            
            // 此语句十分关键，如果没有此句，只能下载第一个文件，其后的所有文件都将失败
            this.ftpClient.completePendingCommand();
            
            v_Event.setSucceedFinish();
        } 
        catch (Exception e) 
        {
            v_Event.setEndTime();
            return e.toString();
        }
        finally
        {
            if ( v_DataInput != null )
            {
                try 
                {
                    v_DataInput.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_DataInput = null;
            }
            
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception e)
                {
                    // Nothing.
                }
                
                v_Input = null;
            }
            
            this.fireFTPAfterListener(v_Event);
        }
        
        return v_OutBuffer.toString();
    }
    
    
    
    /**
     * 上传文件
     * 
     * @param i_LocalFullName   本地文件的全路径
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_IsAppend        追加模式（断点续传）
     * @return  上传成功返回 null 。否则返回异常信息
     */
    public String upload(String i_LocalFullName ,String i_RemoteFullName)
    {
        return this.upload(i_LocalFullName ,i_RemoteFullName ,false);
    }
    
    
    
    /**
     * 上传文件
     * 
     * @param i_LocalFullName   本地文件的全路径
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_IsAppend        追加模式（断点续传）
     * @return  上传成功返回 null 。否则返回异常信息
     */
    public String upload(String i_LocalFullName ,String i_RemoteFullName ,boolean i_IsAppend)
    {
        File            v_File       = new File(i_LocalFullName);
        FileInputStream v_Input      = null;
        DataInputStream v_DataInput  = null;
        
        try 
        {
            v_Input     = new FileInputStream(i_LocalFullName);
            v_DataInput = new DataInputStream(v_Input);
            
            return this.upload(v_DataInput ,v_File.length() ,i_RemoteFullName ,i_IsAppend);
        } 
        catch (Exception e) 
        {
            return e.toString();
        }
        finally
        {
            if ( v_DataInput != null )
            {
                try 
                {
                    v_DataInput.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_DataInput = null;
            }
            
            if ( v_Input != null )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception e)
                {
                    // Nothing.
                }
                
                v_Input = null;
            }
        }
    }
    
    
    
    /**
     * 上传文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-20
     * @version     v1.0
     *
     * @param i_FileDataPacket  文件的数据包（默认开启断点续传）
     * @return                  本次数据包上传结果。请参考 FileHelp.$Upload_* 的系列说明 
     */
    public int upload(FileDataPacket i_FileDataPacket)
    {
        ByteArrayInputStream v_ByteInput = new ByteArrayInputStream(i_FileDataPacket.getDataByte());
        DataInputStream      v_DataInput = new DataInputStream(v_ByteInput);
        String               v_UploadRet = null;
        
        FileDataPacket v_Old = $DataPackets.get(i_FileDataPacket.getName());
        if ( v_Old != null )
        {
            if ( v_Old.getDataNo().intValue() >= i_FileDataPacket.getDataNo().intValue() )
            {
                return FileHelp.$Upload_GoOn;
            }
        }
        
        try 
        {
            v_UploadRet = this.upload(v_DataInput ,i_FileDataPacket.getDataByte().length ,i_FileDataPacket.getName() ,true);
        }
        finally
        {
            if ( v_DataInput != null )
            {
                try 
                {
                    v_DataInput.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_DataInput = null;
            }
            
            if ( v_ByteInput != null )
            {
                try
                {
                    v_ByteInput.close();
                }
                catch (Exception e)
                {
                    // Nothing.
                }
                
                v_ByteInput = null;
            }
        }
        
        if ( Help.isNull(v_UploadRet) )
        {
            if ( i_FileDataPacket.getDataCount().intValue() == i_FileDataPacket.getDataNo().intValue() )
            {
                $DataPackets.remove(i_FileDataPacket.getName());
                return FileHelp.$Upload_Finish;
            }
            else
            {
                $DataPackets.put(i_FileDataPacket.getName() ,i_FileDataPacket ,this.dataPacketTimeOut);
                return FileHelp.$Upload_GoOn;
            }
        }
        else
        {
            return FileHelp.$Upload_Error;
        }
    }
    
    
    
    /**
     * 上传文件
     * 
     * 1. 支持自动创建目录
     * 2. 支持中文目录
     * 
     * @param i_LocalDataInput  本地文件的流（方法内不关闭流）
     * @param i_LocalDataSize   本地文件流的大小
     * @param i_RemoteFullName  远程文件的全路径
     * @param i_IsAppend        追加模式（断点续传）
     * @return  上传成功返回 null 。否则返回异常信息
     */
    public String upload(DataInputStream i_LocalDataInput ,long i_LocalDataSize ,String i_RemoteFullName ,boolean i_IsAppend)
    {
        if ( this.ftpClient == null )
        {
            return "Ftp Client is not connect.";
        }
        
        
        DataInputStream v_DataInput  = null;
        OutputStream    v_Output     = null;
        long            v_FTPingSize = 0;
        DefaultFTPEvent v_Event      = new DefaultFTPEvent(this ,i_LocalDataSize);
        boolean         v_IsContinue = true;
        
        v_Event.setActionType(1);
        
        try 
        {
            if ( !Help.isNull(i_RemoteFullName) )
            {
                String [] v_RFNameArr   = i_RemoteFullName.split("/");
                String    v_DirFullName = StringHelp.replaceLast(i_RemoteFullName ,"/" + v_RFNameArr[v_RFNameArr.length - 1] ,"");
                
                this.makeDirectory(v_DirFullName);
            }
            
            v_DataInput      = i_LocalDataInput;
            byte [] v_Buffer = new byte[$BufferSize];
            int     v_RSize  = 0;
            
            if ( i_IsAppend )
            {                                              
                v_Output = this.ftpClient.appendFileStream(new String(i_RemoteFullName.getBytes("GBK") ,"ISO-8859-1"));
            }
            else
            {
                v_Output = this.ftpClient.storeFileStream(new String(i_RemoteFullName.getBytes("GBK") ,"ISO-8859-1"));
            }
            
            v_IsContinue = this.fireFTPBeforeListener(v_Event);
            
            if ( this.dataSafe )
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_Output.write(ByteHelp.xorMV(v_Buffer ,0 ,v_RSize) ,0 ,v_RSize);
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                } 
            }
            else
            {
                while ( v_IsContinue && (v_RSize = v_DataInput.read(v_Buffer)) >= 0 )
                {
                    v_Output.write(v_Buffer ,0 ,v_RSize);
                    
                    v_FTPingSize += v_RSize;
                    
                    v_Event.setCompleteSize(v_FTPingSize);
                    v_IsContinue = this.fireFTPingListener(v_Event);
                } 
            }
            
            v_Output.flush();
            v_Output.close();
            v_Output = null;
            
            // 此语句十分关键，如果没有此句，只能下载第一个文件，其后的所有文件都将失败
            this.ftpClient.completePendingCommand();
            
            v_Event.setSucceedFinish();
            
            
//          下面注解的代码，可直接上传文件，不用读写数据流。
//          boolean v_Ret = this.ftpClient.storeFile(i_RemoteFullName ,v_Input);
//          
//          if ( v_Ret )
//          {
//              System.out.println("upLoad is succeed.");
//          }
//          else
//          {
//              System.out.println("upload is faild.");
//          }
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            v_Event.setEndTime();
            return e.toString();
        }
        finally
        {
            if ( v_Output != null )
            {
                try 
                {
                    v_Output.close();
                } 
                catch (Exception e) 
                {
                    // Nothing.
                }
                
                v_Output = null;
            }
            
            this.fireFTPAfterListener(v_Event);
        }
        
        return null;
    }
    
    
    
    /**
     * 创建FTP目录。可连续创建多级目录
     * 
     * 1. 支持中文目录
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-20
     * @version     v1.0
     *
     * @param i_DirFullName   目录的全路径名称
     * @return                返回值表示创建的目录数量
     */
    public int makeDirectory(String i_DirFullName)
    {
        if ( Help.isNull(i_DirFullName) )
        {
            return 0;
        }
        
        if ( this.ftpClient == null )
        {
            return -99;
        }
        
        try
        {
            String []     v_DirFullNameArr = i_DirFullName.trim().split("/");
            StringBuilder v_DirBuffer      = new StringBuilder();
            int           v_CreateCount    = 0;
            
            for (String v_DirName : v_DirFullNameArr)
            {
                if ( Help.isNull(v_DirName) )
                {
                    continue;
                }
                
                v_DirBuffer.append("/").append(new String(v_DirName.getBytes("GBK") ,"ISO-8859-1"));
                
                if ( this.ftpClient.makeDirectory(v_DirBuffer.toString()) )
                {
                    v_CreateCount++;
                }
            }
            
            return v_CreateCount;
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        
        return -1;
    }
    
    
    
    /**
     * 删除文件
     * 
     * @param i_RemoteFullName  远程文件的全路径
     * @return  上传成功返回 null 。否则返回异常信息
     */
    public String deleteFile(String i_RemoteFullName)
    {
        if ( this.ftpClient == null )
        {
            return "Ftp Client is not connect.";
        }
        
        
        try
        {
            boolean v_Ret = this.ftpClient.deleteFile(new String(i_RemoteFullName.getBytes("GBK") ,"ISO-8859-1"));
            
            if ( !v_Ret )
            {
                return "Delete file is faild.";
            }
        }
        catch (Exception exce)
        {
            return exce.toString();
        }
        
        return null;
    }
    
    
    
    /**
     * 注册FTP文件事件
     * 
     * @param e
     */
    public void addFTPListener(FTPListener e)
    {
        if ( this.ftpListeners == null )
        {
            this.ftpListeners = new HashSet<FTPListener>();
        }
        
        this.ftpListeners.add(e);
    }
    
    
    
    /**
     * 移除FTP文件事件
     * 
     * @param e
     */
    public void removeFTPListener(FTPListener e)
    {
        if ( this.ftpListeners == null )
        {
            return;
        }
        
        this.ftpListeners.remove(e);
    }
    
    
    
    /**
     * 触发FTP传送文件之前的事件
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    protected boolean fireFTPBeforeListener(FTPEvent i_Event)
    {
        if ( this.ftpListeners == null )
        {
            return true;
        }
        
        return notifyFTPBeforeListeners(i_Event);
    }
    
    
    
    /**
     * 触发FTP传送文件事件
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    protected boolean fireFTPingListener(FTPEvent i_Event)
    {
        if ( this.ftpListeners == null )
        {
            return true;
        }
        
        return notifyFTPingListeners(i_Event);
    }
    
    
    
    /**
     * 触发FTP传送文件完成之后的事件
     * 
     * @param i_Event
     */
    protected void fireFTPAfterListener(FTPEvent i_Event)
    {
        if ( this.ftpListeners == null )
        {
            return;
        }
        
        notifyFTPAfterListeners(i_Event);
    }

    
    
    /**
     * 通知所有注册FTP传送文件之前的事件监听的对象
     * 
     * @param i_Event
     * @return   返回值表示是否继续
     */
    private boolean notifyFTPBeforeListeners(FTPEvent i_Event)
    {
        Iterator<FTPListener> v_Iter       = this.ftpListeners.iterator();
        boolean               v_IsContinue = true;

        while ( v_IsContinue && v_Iter.hasNext() ) 
        {
            v_IsContinue = v_Iter.next().ftpBefore(i_Event);
        }
        
        return v_IsContinue;
    }
    
    
    
    /**
     * 通知所有注册FTP传送文件事件监听的对象
     * 
     * @param i_Event
     */
    private boolean notifyFTPingListeners(FTPEvent i_Event)
    {
        Iterator<FTPListener> v_Iter       = this.ftpListeners.iterator();
        boolean               v_IsContinue = true; 

        while ( v_IsContinue && v_Iter.hasNext() ) 
        {
            v_IsContinue = v_Iter.next().ftpProcess(i_Event);
        }
        
        return v_IsContinue;
    }

    
    
    /**
     * 通知所有注册FTP传送完成之后的事件监听的对象
     * 
     * @param i_Event
     */
    private void notifyFTPAfterListeners(FTPEvent i_Event)
    {
        Iterator<FTPListener> v_Iter = this.ftpListeners.iterator();

        while ( v_Iter.hasNext() ) 
        {
            v_Iter.next().ftpAfter(i_Event);
        }
    }
    
    
    
    public boolean isDataSafe() 
    {
        return dataSafe;
    }



    public void setDataSafe(boolean dataSafe) 
    {
        this.dataSafe = dataSafe;
    }



    @Override
    protected void finalize() throws Throwable 
    {
        this.close();
        
        if ( this.ftpListeners != null )
        {
            this.ftpListeners.clear();
            this.ftpListeners = null;
        }
        
        super.finalize();
    }
    
}
