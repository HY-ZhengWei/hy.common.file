package org.hy.common.minio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.ftp.enums.PathType;
import org.hy.common.xml.log.Logger;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListBucketsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.credentials.Credentials;
import io.minio.credentials.Provider;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;





/**
 * Minio的帮助类
 *
 * @author      ZhengWei(HY)
 * @createDate  2026-07-14
 * @version     v1.0
 */
public class MinioHelp
{
    
    private static final Logger $Logger = new Logger(MinioHelp.class);
    
    
    
    /** 缓存大小 */
    private int         bufferSize = 128 * 1024;
    
    /** 桶前缀名称 */
    private String      bucketPrefixName;
    
    /** Minio客户端对象 */
    private MinioClient minio;
    
    
    
    public MinioHelp()
    {
        
    }
    
    
    
    public MinioHelp(MinioClient i_Minio)
    {
        this(i_Minio ,null);
    }
    
    
    
    public MinioHelp(MinioClient i_Minio ,String i_BuckePrefixtName)
    {
        this.setMinio(i_Minio);
        this.setBucketPrefixName(i_BuckePrefixtName);
    }
    
    
    
    /**
     * 获取Minio用户账号
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @return
     */
    public String getAccessKey()
    {
        try
        {
            // 获取 MinioAsyncClient 实例
            Field v_CredentialsField = MinioClient.class.getDeclaredField("asyncClient");
            v_CredentialsField.setAccessible(true);
            MinioAsyncClient v_AsyncClient = (MinioAsyncClient) v_CredentialsField.get(this.minio);
            
            // 获取 Provider 实例
            Field v_ProviderField = v_AsyncClient.getClass().getSuperclass().getDeclaredField("provider");
            v_ProviderField.setAccessible(true);
            Provider v_Provider = (Provider) v_ProviderField.get(v_AsyncClient);
            
            // 获取 Credentials 实例
            Credentials v_Credentials = v_Provider.fetch();
            return v_Credentials.accessKey();
        }
        catch (Exception exce)
        {
            $Logger.error("获取Minio用户账号异常" ,exce);
            return null;
        }
    }
    
    
    
    /**
     * 查询所有桶名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @return
     */
    public List<String> queryBucketNames()  
    {
        try
        {
            List<Bucket> v_Buckets = this.minio.listBuckets(ListBucketsArgs.builder().build());
            return v_Buckets.stream().map(Bucket::name).toList();
        }
        catch (Exception exce)
        {
            $Logger.error("查询查询所有桶名称异常" ,exce);
            return null;
        }
    }
    
    
    
    /**
     * 按用户工号生成桶名称
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo  用户工号
     * @return
     */
    public String makeBucketName(String i_UserNo)
    {
        if ( Help.isNull(this.bucketPrefixName) )
        {
            return i_UserNo;
        }
        else
        {
            return this.bucketPrefixName + "-" + i_UserNo;
        }
    }
    
    
    
    /**
     * 创建桶
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo  用户工号
     * @return          返回是否创建桶成功
     */
    public boolean createBucket(String i_UserNo)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        try
        {
            boolean v_Exists = this.minio.bucketExists(BucketExistsArgs.builder().bucket(v_BucketName).build());
            if ( !v_Exists )
            {
                this.minio.makeBucket(MakeBucketArgs.builder().bucket(v_BucketName).build());
                $Logger.info("创建Minio桶[{}]成功" ,v_BucketName);
            }
            
            return true;
        }
        catch (Exception exce)
        {
            $Logger.error("创建Minio桶[{}]异常" ,v_BucketName ,exce);
            return false;
        }
    }
    
    
    
    /**
     * 遍历目录中的文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo      用户工号
     * @param i_Dir         目录路径
     * @param i_HaveChilds  是否递归所有子目录
     * @return              返回 null 表示发生异常
     */
    public List<MinioPath> getFiles(String i_UserNo ,String i_Dir ,boolean i_HaveChilds)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        // 构建查询参数
        ListObjectsArgs.Builder v_QueryBuilder = ListObjectsArgs.builder()
                                                                .bucket(v_BucketName)
                                                                .recursive(true); // true：递归所有子目录；false：只展示一级
        String v_Dir = Help.NVL(i_Dir);
        if ( !Help.isNull(v_Dir) )
        {
            if ( v_Dir.endsWith("/") )
            {
                v_Dir = v_Dir.substring(0 ,v_Dir.length() - 1);
            }
            v_QueryBuilder.prefix(v_Dir);
        }
        
        ListObjectsArgs     v_QueryArgs    = v_QueryBuilder.build();
        List<MinioPath>     v_Ret          = null;
        Map<String ,String> v_FilterDouble = null;   // 防止重复添加，仅在 i_HaveChilds = false 时生效
        
        try
        {
            Iterable<Result<Item>> v_Files = this.minio.listObjects(v_QueryArgs);
            v_Ret = new ArrayList<MinioPath>();
            if ( v_Files == null )
            {
                return v_Ret;
            }
            
            if ( !i_HaveChilds )
            {
                v_FilterDouble = new HashMap<String ,String>();
            }
            
            int v_DirLen = v_Dir.length();
            for (Result<Item> v_Item : v_Files)
            {
                Item   v_Object     = v_Item.get();
                String v_ObjectName = v_Object.objectName();
                int    v_LastIndex  = v_ObjectName.lastIndexOf("/");
                String v_PathName   = "";
                String v_FileName   = "";
                
                // 桶下一级目录 或 桶下的一级目录中的子文件
                if ( v_LastIndex > 0 )
                {
                    if ( !i_HaveChilds )
                    {
                        // 查桶下的所有对象
                        if ( v_DirLen == 0 )
                        {
                            String [] v_Name = v_ObjectName.split("/");
                            if ( v_Name.length >= 2 )
                            {
                                v_PathName = "";
                                v_FileName = v_Name[0] + "/";
                                if ( !v_FilterDouble.containsKey(v_PathName + v_FileName) )
                                {
                                    v_Ret.add(new MinioPath(v_PathName ,v_FileName ,PathType.Directory));
                                    v_FilterDouble.put(v_PathName + v_FileName ,"");
                                }
                                continue;
                            }
                        }
                        else if ( v_LastIndex >= v_DirLen )
                        {
                            v_ObjectName = v_ObjectName.substring(0 ,v_LastIndex);
                            v_LastIndex  = v_ObjectName.lastIndexOf("/");
                            v_PathName   = v_ObjectName.substring(0 ,v_LastIndex + 1);
                            v_FileName   = v_ObjectName.substring(v_LastIndex + 1) + "/";
                            if ( !v_FilterDouble.containsKey(v_PathName + v_FileName) )
                            {
                                v_Ret.add(new MinioPath(v_PathName ,v_FileName ,PathType.Directory));
                                v_FilterDouble.put(v_PathName + v_FileName ,"");
                            }
                            continue;
                        }
                    }
                    
                    v_PathName = v_ObjectName.substring(0 ,v_LastIndex + 1);
                    v_FileName = v_ObjectName.substring(v_LastIndex + 1);
                    
                    // 桶下的一级目录
                    if ( Help.isNull(v_FileName) )
                    {
                        v_ObjectName = v_ObjectName.substring(0 ,v_LastIndex);
                        v_LastIndex  = v_ObjectName.lastIndexOf("/");
                        if ( v_LastIndex > 0  )
                        {
                            v_PathName = v_ObjectName.substring(0 ,v_LastIndex + 1);
                            v_FileName = v_ObjectName.substring(v_LastIndex + 1) + "/";
                        }
                        else
                        {
                            v_FileName = v_PathName;
                            v_PathName = "";
                        }
                    }
                }
                // 桶下的一级文件
                else
                {
                    v_FileName = v_ObjectName;
                }
                
                v_Ret.add(new MinioPath(v_PathName ,v_FileName ,v_Object.isDir() ? PathType.Directory : PathType.File));
            }
        }
        catch (Exception exce)
        {   
            $Logger.error("遍历目录Minio[{}.{}]异常" ,v_BucketName ,i_Dir ,exce);
        }
        
        if ( v_FilterDouble != null )
        {
            v_FilterDouble.clear();
            v_FilterDouble = null;
        }
        return v_Ret;
    }
    
    
    
    /**
     * 获取路径类型
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_Path  路径
     * @return        返回 NULL 表示异常
     */
    public PathType getPathType(String i_UserNo ,String i_Path)
    {
        List<MinioPath> v_Items = this.getFiles(i_UserNo ,i_Path ,false);
        if ( v_Items == null )
        {
            return PathType.NotExist;
        }
        else
        {
            if ( v_Items.size() == 1 )
            {
                return v_Items.get(0).getPathType();
            }
            else if ( v_Items.size() >= 2 )
            {
                return PathType.Directory;
            }
            else
            {
                return PathType.NotExist;
            }
        }
    }
    
    
    
    /**
     * 上传文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_FileFullName   被上传文件的全路径
     * @return                 返回空字符串表示成功，其它情况表示异常信息
     */
    public String upload(String i_UserNo ,String i_FileFullName)
    {
        String v_MinioFileName = i_FileFullName;
        if ( !Help.isNull(v_MinioFileName) )
        {
            String [] v_Names = StringHelp.replaceAll(v_MinioFileName ,"\\" ,"/").split("/");
            v_MinioFileName = v_Names[v_Names.length - 1];
            return upload(i_UserNo ,v_MinioFileName ,i_FileFullName);
        }
        else
        {
            return "上传文件是空字符";
        }
    }
    
    
    
    /**
     * 上传文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_FileFullName   被上传文件的全路径
     * @param i_MinioFileName  Minio的文件名称
     * @return                 返回空字符串表示成功，其它情况表示异常信息
     */
    public String upload(String i_UserNo ,String i_FileFullName ,String i_MinioFileName)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        try ( FileInputStream v_FileInput = new FileInputStream(i_FileFullName) )
        {
            this.minio.putObject(PutObjectArgs.builder().bucket(v_BucketName).object(i_MinioFileName).stream(v_FileInput ,v_FileInput.available() ,-1).build());
            $Logger.info("上传[{}]Minio[{}.{}]成功" ,i_FileFullName ,v_BucketName ,i_MinioFileName);
            return "";
        }
        catch (Exception exce)
        {
            $Logger.error("上传[{}]Minio[{}.{}]异常" ,i_FileFullName ,v_BucketName ,i_MinioFileName ,exce);
            return exce.getMessage();
        }
    }
    
    
    
    /**
     * 下载文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_MinioFileName  Minio的文件名称
     * @param i_SavePath       下载后保存文件的路径
     * @return                 返回空字符串表示成功，其它情况表示异常信息
     */
    public String download(String i_UserNo ,String i_MinioFileName ,String i_SavePath)
    {
        return this.download(i_UserNo ,i_MinioFileName ,i_SavePath ,null);
    }
    
    
    
    /**
     * 下载文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_MinioFileName  Minio的文件名称
     * @param i_SavePath       下载后保存文件的路径
     * @param i_SaveFileName   下载后保存文件的名称
     * @return                 返回空字符串表示成功，其它情况表示异常信息
     */
    public String download(String i_UserNo ,String i_MinioFileName ,String i_SavePath ,String i_SaveFileName)
    {
        String v_BucketName   = this.makeBucketName(i_UserNo);
        String v_SaveFileName = i_SaveFileName;
        if ( Help.isNull(v_SaveFileName) )
        {
            String [] v_Names = i_MinioFileName.split("/");
            v_SaveFileName = v_Names[v_Names.length - 1];
        }
        
        try ( InputStream      v_Input = this.minio.getObject(GetObjectArgs.builder().bucket(v_BucketName).object(i_MinioFileName).build());
              FileOutputStream v_Out   = new FileOutputStream(i_SavePath + Help.getSysPathSeparator() + v_SaveFileName) )
        {
            byte [] v_Buffer     = new byte[this.bufferSize];
            int     v_BufferSize = 0;
            while ( (v_BufferSize = v_Input.read(v_Buffer)) != -1 )
            {
                v_Out.write(v_Buffer ,0 ,v_BufferSize);
            }
            $Logger.info("下载Minio[{}.{}]成功[{}/{}]" ,v_BucketName ,i_MinioFileName ,i_SavePath ,v_SaveFileName);
            return "";
        }
        catch (Exception exce)
        {
            $Logger.error("下载Minio[{}.{}]异常" ,v_BucketName ,i_MinioFileName ,exce);
            return exce.getMessage();
        }
    }
    
    
    
    /**
     * 查询文件元信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_MinioFileName  Minio的文件名称
     * @return                 返回null表示异常
     */
    public StatObjectResponse queryStat(String i_UserNo ,String i_MinioFileName)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        // 构建查询参数
        StatObjectArgs v_StatArgs = StatObjectArgs.builder()
                .bucket(v_BucketName)
                .object(i_MinioFileName)
                .build();
        
        try
        {
            StatObjectResponse v_Stat = this.minio.statObject(v_StatArgs);
            return v_Stat;
        }
        catch (Exception exce)
        {
            $Logger.info("查询Minio文件元信息[{}.{}]异常" ,v_BucketName ,i_MinioFileName ,exce);
            return null;
        }
    }
    
    
    
    /**
     * 获取文件临时链接（如果不设置值：默认7天有效期）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_MinioFileName  Minio的文件名称
     * @param i_Expiry         过期天数（最大7天，是Minio限制的）
     * @return                 返回null表示异常
     */
    public String share(String i_UserNo ,String i_MinioFileName ,Integer i_Expiry)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        try
        {
            GetPresignedObjectUrlArgs v_ShareUrlArgs = GetPresignedObjectUrlArgs.builder().bucket(v_BucketName).object(i_MinioFileName).method(Method.GET).expiry(1 ,TimeUnit.DAYS).build();
            String v_ShareUrl = this.minio.getPresignedObjectUrl(v_ShareUrlArgs);
            $Logger.info("共享Minio[{}.{}]成功[{}]" ,v_BucketName ,i_MinioFileName ,v_ShareUrl);
            return v_ShareUrl;
        }
        catch (Exception exce)
        {
            $Logger.error("共享Minio[{}.{}]异常" ,v_BucketName ,i_MinioFileName ,exce);
            return null;
        }
    }
    
    
    
    /**
     * 删除文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2026-07-14
     * @version     v1.0
     *
     * @param i_UserNo         用户工号
     * @param i_MinioFileName  Minio的文件名称
     * @return                 返回是否成功
     */
    public boolean delete(String i_UserNo ,String i_MinioFileName)
    {
        String v_BucketName = this.makeBucketName(i_UserNo);
        try
        {
            this.minio.removeObject(RemoveObjectArgs.builder().bucket(v_BucketName).object(i_MinioFileName).build());
            $Logger.info("删除Minio[{}.{}]成功" ,v_BucketName ,i_MinioFileName);
            return true;
        }
        catch (Exception exce)
        {
            $Logger.error("删除Minio[{}.{}]异常" ,v_BucketName ,i_MinioFileName ,exce);
            return false;
        }
    }

    
    
    /**
     * 获取：桶前缀名称
     */
    public String getBucketPrefixName()
    {
        return bucketPrefixName;
    }


    
    /**
     * 设置：桶前缀名称
     * 
     * @param i_BucketPrefixName 桶前缀名称
     */
    public void setBucketPrefixName(String i_BucketPrefixName)
    {
        this.bucketPrefixName = i_BucketPrefixName;
    }



    /**
     * 获取：缓存大小
     */
    public int getBufferSize()
    {
        return bufferSize;
    }


    
    /**
     * 设置：缓存大小
     * 
     * @param i_BufferSize 缓存大小
     */
    public void setBufferSize(int i_BufferSize)
    {
        this.bufferSize = i_BufferSize;
    }


    
    /**
     * 获取：Minio客户端对象
     */
    public MinioClient getMinio()
    {
        return minio;
    }


    
    /**
     * 设置：Minio客户端对象
     * 
     * @param i_Minio Minio客户端对象
     */
    public void setMinio(MinioClient i_Minio)
    {
        this.minio = i_Minio;
    }
    
}
