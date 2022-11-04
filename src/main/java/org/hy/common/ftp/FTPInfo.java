package org.hy.common.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.hy.common.Help;





/**
 * FTP信息
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-03-27
 *           V2.0  2020-05-28  添加：继承 FTPClient 父类
 */
public class FTPInfo extends FTPClient implements Cloneable
{
    
    /** 连接超时时长 */
    public static final int $ConnectTimeOut = 30 * 1000;
    
    /** 读取文件的超时时长 */
    public static final int $ReadTimeOut    = 2 * 60 * 1000; 
    
    
    
    /** 文件服务器IP */
    private String           ip;
    
    /** 文件服务器端口。默认是21 */
    private int              port;
    
    /** 登陆用户名称 */
    private String           user;
    
    /** 登陆密码 */
    private String           password;
    
    /** 连接成功后的初始化目录 */
    private String           initPath;
    
    /** 本地是否被动模式 */
    private boolean          isLocalPassiveMode; 
    
    /** 远程服务是否被动模式 */
    private boolean          isRemotePassiveMode;  
    
    /** 安全接口 */
    private FTPSecurity      security;
    
    /** 读取文件的超时时长 */
    private int              dataTimeout;
    
    
    
    public FTPInfo()
    {
        this("" ,21 ,"" ,"");
    }
    
    
    
    /**
     * 构造器
     * 
     * @param i_FTPIP         文件服务器IP
     * @param i_FTPPort       文件服务器端口。默认是21
     * @param i_FTPUser       登陆用户名称
     * @param i_FTPPassword   登陆密码
     */
    public FTPInfo(String i_IP ,int i_Port ,String i_User ,String i_Password)
    {
        this.setIp(      i_IP);
        this.setPort(    i_Port);
        this.setUser(    i_User);
        this.setPassword(i_Password);
        this.setControlEncoding("UTF-8");
        this.setLocalPassiveMode(false);
        this.setRemotePassiveMode(false);
        this.setDataTimeout($ReadTimeOut);
    }



    public String getIp() 
    {
        return ip;
    }



    public void setIp(String ip) 
    {
        this.ip = ip;
    }
    
    
    
    public String getPasswordValue()
    {
        return this.password;
    }
    
    
    
    public String getPassword() 
    {
        if ( this.security != null && !Help.isNull(this.user) )
        {
            if ( Help.isNull(this.password) )
            {
                return "";
            }
            else
            {
                return this.security.getDecrypt(this.password, this.user);
            }
        }
        else
        {
            return this.password;
        }
    }



    public void setPassword(String i_Password) 
    {
        if (  this.security != null && !Help.isNull(this.user) )
        {
            if ( i_Password != null )
            {
                if ( i_Password.trim().length() == this.security.getSecurityLen())
                {
                    this.password  = i_Password;
                }
                else
                {
                    this.password  = this.security.getEncrypt(i_Password ,this.user);
                }
            }
            else
            {
                this.password = i_Password;
            }
        }
        else
        {
            this.password = i_Password;
        }
    }



    public int getPort() 
    {
        return port;
    }



    public void setPort(int port) 
    {
        this.port = port;
    }



    public String getUser()
    {
        return user;
    }


    
    public void setUser(String user) 
    {
        this.user = user;
    }



    public String getInitPath() 
    {
        return Help.NVL(initPath ,"/");
    }
    
    
    
    public void setInitPath(String initPath) 
    {
        this.initPath = initPath;
    }


    
    public FTPSecurity getSecurity() 
    {
        return security;
    }
    
    
    
    public void setSecurity(FTPSecurity security) 
    {
        this.security = security;
    }
    
    
    
    /**
     * 获取：本地是否被动模式
     */
    public boolean isLocalPassiveMode()
    {
        return isLocalPassiveMode;
    }


    
    /**
     * 获取：远程服务是否被动模式
     */
    public boolean isRemotePassiveMode()
    {
        return isRemotePassiveMode;
    }


    
    /**
     * 设置：本地是否被动模式
     * 
     * @param isLocalPassiveMode 
     */
    public void setLocalPassiveMode(boolean isLocalPassiveMode)
    {
        this.isLocalPassiveMode = isLocalPassiveMode;
    }


    
    /**
     * 设置：远程服务是否被动模式
     * 
     * @param isRemotePassiveMode 
     */
    public void setRemotePassiveMode(boolean isRemotePassiveMode)
    {
        this.isRemotePassiveMode = isRemotePassiveMode;
    }


    
    /**
     * 获取：读取文件的超时时长
     */
    public int getDataTimeout()
    {
        return dataTimeout;
    }


    
    /**
     * 设置：读取文件的超时时长
     * 
     * @param dataTimeout 
     */
    public void setDataTimeout(int dataTimeout)
    {
        this.dataTimeout = dataTimeout;
        super.setDataTimeout(this.dataTimeout);
    }
    
    
    
    /**
     * Get the current receivedBuffer size
     * @return the size, or -1 if not initialised
     * @since 3.0
     */
    public int getReceiveBufferSize()
    {
        return super.getReceiveBufferSize();
    }
    
    
    
    /**
     * Get the current sendBuffer size
     * @return the size, or -1 if not initialised
     * @since 3.0
     */
    public int getSendBufferSize()
    {
        return super.getSendBufferSize();
    }



    /**
     * 克隆
     */
    public FTPInfo clone() 
    {
        FTPInfo v_Clone = new FTPInfo(this.ip ,this.port ,this.user ,this.password);
        
        v_Clone.setLocalPassiveMode( this.isLocalPassiveMode);
        v_Clone.setRemotePassiveMode(this.isRemotePassiveMode);
        v_Clone.setInitPath(this.initPath);
        v_Clone.setSecurity(this.security);
        v_Clone.setControlEncoding(this.getControlEncoding());
        
        return v_Clone;
    }
    
}
