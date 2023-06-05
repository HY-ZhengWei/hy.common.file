package org.hy.common.xml.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.Return;
import org.hy.common.XJavaID;
import org.hy.common.xml.SerializableDef;
import org.hy.common.xml.XHttp;
import org.hy.common.xml.log.Logger;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;





/**
 * 封装Drools规则引擎的执行类
 * 
 * 有三种方法注入规则
 *    方法1：以规则文件的方式注入并生成规则引擎    （本地文件）
 *    方法2：以规则文本的方式注入并生成规则引擎    （文本信息）
 *    方法3：以规则远端请求的方式注入并生成规则引擎（远端请求）
 * 
 * 
 * 当三种方法均注入规则时，解释的优先级为：
 *     本地文件 > 文本信息 > 远端请求
 * 
 *     即，文本优先，远端请求最后解释
 *
 * @author      ZhengWei(HY)
 * @createDate  2020-05-25
 * @version     v1.0
 */
public class XRule extends SerializableDef implements XJavaID
{

    private static final long serialVersionUID = 1329720425183820778L;
    
    private static final Logger $Logger        = new Logger(XRule.class);
    
    /** 正则表达式识别：package xxx.xxx; 的包信息 */
    private static final String $REGEX_Package = "[Pp][Aa][Cc][Kk][Aa][Gg][Ee]( )+\\w+\\.\\w+[\\w\\.]*;";
    
    /** 正则表达式识别：import xxx.xxx; 的引包信息 */
    private static final String $REGEX_Import  = "[Ii][Mm][Pp][Oo][Rr][Tt]( )+\\w+\\.\\w+[\\w\\.]*;";
    
    
    
    /** XJava对象池中的ID标识 */
    private String              xjavaID;
    
    /** 规则引擎的文本信息 */
    private String              ruleInfo;
    
    /** 规则引擎的文件路径 */
    private String              ruleFile;
    
    /** 通过Http请求获取规则引擎的远程文本信息 */
    private XHttp               ruleRemote;
    
    /** 规则会话：无状态的 */
    private StatelessKieSession kieSession;
    
    /** 注释。可用于日志的输出等帮助性的信息 */
    private String              comment;
    
    /** 是否为“懒汉模式”，即只在需要时才加载。默认为：true（懒汉模式） */
    private boolean             isLazyMode;
    
    /** 是否需要初始化（内部使用） */
    private boolean             isNeedInit;

    
    
    public XRule()
    {
        this(true);
    }
    
    
    
    public XRule(boolean i_IsLazyMode)
    {
        this.ruleInfo   = null;
        this.kieSession = null;
        this.isLazyMode = i_IsLazyMode;
        this.isNeedInit = true;
    }
    
    
    
    /**
     * 执行规则引擎的计算
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-25
     * @version     v1.0
     *
     * @param io_RuleData
     * @return
     */
    public boolean execute(Object io_RuleData)
    {
        if ( io_RuleData == null )
        {
            return false;
        }
        
        if ( this.isLazyMode )
        {
            this.initRule();
        }
        
        if ( this.kieSession == null )
        {
            return false;
        }
        
        this.kieSession.execute(io_RuleData);
        return true;
    }
    
    
    
    /**
     * 执行规则引擎的计算
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-25
     * @version     v1.0
     *
     * @param io_RuleData
     * @return
     */
    public boolean execute(Iterable<?> io_RuleData)
    {
        if ( io_RuleData == null )
        {
            return false;
        }
        
        if ( this.isLazyMode )
        {
            this.initRule();
        }
        
        if ( this.kieSession == null )
        {
            return false;
        }
        
        this.kieSession.execute(io_RuleData);
        return true;
    }
    
    
    
    /**
     * 初始化规则引擎
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-25
     * @version     v1.0
     *
     */
    public synchronized void initRule()
    {
        if ( (Help.isNull(this.ruleInfo) && Help.isNull(this.ruleFile)) || !this.isNeedInit )
        {
            return;
        }
        
        /* 必须文件优先，再判定文本 */
        if ( !Help.isNull(this.ruleFile) )
        {
            this.initRuleFile();
        }
        else if ( !Help.isNull(this.ruleInfo) )
        {
            this.initRuleInfo();
        }
        else if ( null != this.ruleRemote )
        {
            this.initRuleRemote();
        }
        
        this.isNeedInit = false;
    }
    
    
    
    /**
     * 初始化规则引擎（按远程Http请求）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-09
     * @version     v1.0
     *
     */
    private void initRuleRemote()
    {
        Return<?> v_Response = this.ruleRemote.request();
        
        if ( v_Response == null || !v_Response.booleanValue() || Help.isNull(v_Response.getParamStr()) )
        {
            $Logger.error(Date.getNowTime().getFullMilli() + " XRule Build Errors: " + Help.NVL(this.comment) + "\n" + this.ruleRemote.getUrl());
            return;
        }
        
        KnowledgeBuilder v_KBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        v_KBuilder.add(ResourceFactory.newByteArrayResource(v_Response.getParamStr().getBytes()) ,ResourceType.DRL);
        
        if ( v_KBuilder.hasErrors() )
        {
            $Logger.error(Date.getNowTime().getFullMilli() + " XRule Build Errors: " + Help.NVL(this.comment) + "\n" + this.ruleRemote.getUrl() + "\n" + v_Response.getParamStr());
            throw new RuntimeException("XRule Build Errors:\n" + v_KBuilder.getErrors());
        }
        
        InternalKnowledgeBase v_KBase = KnowledgeBaseFactory.newKnowledgeBase();
        v_KBase.addPackages(v_KBuilder.getKnowledgePackages());
        
        this.kieSession = v_KBase.newStatelessKieSession();
    }
    
    
    
    /**
     * 初始化规则引擎（按文本内容）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-25
     * @version     v1.0
     *
     */
    private void initRuleInfo()
    {
        KnowledgeBuilder v_KBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        v_KBuilder.add(ResourceFactory.newByteArrayResource(this.ruleInfo.getBytes()) ,ResourceType.DRL);
        
        if ( v_KBuilder.hasErrors() )
        {
            $Logger.error(Date.getNowTime().getFullMilli() + " XRule Build Errors: " + Help.NVL(this.comment) + "\n" + this.ruleInfo);
            throw new RuntimeException("XRule Build Errors:\n" + v_KBuilder.getErrors());
        }
        
        InternalKnowledgeBase v_KBase = KnowledgeBaseFactory.newKnowledgeBase();
        v_KBase.addPackages(v_KBuilder.getKnowledgePackages());
        
        this.kieSession = v_KBase.newStatelessKieSession();
    }
    
    
    
    /**
     * 初始化规则引擎（按文件）
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-05-26
     * @version     v1.0
     *
     */
    private void initRuleFile()
    {
        KnowledgeBuilder v_KBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        v_KBuilder.add(ResourceFactory.newClassPathResource(this.ruleFile ,"UTF-8") ,ResourceType.DRL);
        
        if ( v_KBuilder.hasErrors() )
        {
            $Logger.error(Date.getNowTime().getFullMilli() + " XRule Build Errors: " + Help.NVL(this.comment) + "\n" + this.ruleFile);
            throw new RuntimeException("XRule Build Errors:\n" + v_KBuilder.getErrors());
        }
        
        InternalKnowledgeBase v_KBase = KnowledgeBaseFactory.newKnowledgeBase();
        v_KBase.addPackages(v_KBuilder.getKnowledgePackages());
        
        this.kieSession = v_KBase.newStatelessKieSession();
    }
    
    
    
    /**
     * 获取命名空间的“包”信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-02
     * @version     v1.0
     *
     * @return  返回结果为：package org.hy.common.xml.junit.drools;  带最后面的分号
     */
    public String getPackage()
    {
        if ( Help.isNull(this.ruleInfo) )
        {
            return null;
        }
        
        Pattern v_Pattern = Pattern.compile($REGEX_Package);
        Matcher v_Matcher = v_Pattern.matcher(this.ruleInfo);
        
        if ( v_Matcher.find() )
        {
            return this.ruleInfo.substring(v_Matcher.start() ,v_Matcher.end());
        }
        
        return null;
    }
    
    
    
    /**
     * 获取所有引用类的信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2020-06-02
     * @version     v1.0
     *
     * @return  返回结果为：import java.util.List;  带最后面的分号
     */
    public List<String> getImports()
    {
        if ( Help.isNull(this.ruleInfo) )
        {
            return null;
        }
        
        List<String> v_Imports = new ArrayList<String>();
        Pattern      v_Pattern = Pattern.compile($REGEX_Import);
        Matcher      v_Matcher = v_Pattern.matcher(this.ruleInfo);
        
        while ( v_Matcher.find() )
        {
            v_Imports.add(this.ruleInfo.substring(v_Matcher.start() ,v_Matcher.end()));
        }
        
        return v_Imports;
    }


    
    /**
     * 获取：规则引擎的文本信息
     */
    public String getValue()
    {
        return ruleInfo;
    }


    
    /**
     * 设置：规则引擎的文本信息
     * 
     * @param i_RuleInfo
     */
    public void setValue(String i_RuleInfo)
    {
        this.ruleInfo   = i_RuleInfo;
        this.isNeedInit = true;
        
        if ( !this.isLazyMode )
        {
            this.initRule();
        }
    }

    
    
    /**
     * 获取：规则引擎的文件路径
     */
    public String getFile()
    {
        return ruleFile;
    }


    
    /**
     * 设置：规则引擎的文件路径
     * 
     * @param i_RuleFile
     */
    public void setFile(String i_RuleFile)
    {
        this.ruleFile   = i_RuleFile;
        this.isNeedInit = true;
        
        if ( !this.isLazyMode )
        {
            this.initRule();
        }
    }


    
    /**
     * 获取：通过Http请求获取规则引擎的远程文本信息
     */
    public XHttp getRuleRemote()
    {
        return ruleRemote;
    }


    
    /**
     * 设置：通过Http请求获取规则引擎的远程文本信息
     * 
     * @param i_RuleRemote
     */
    public void setRuleRemote(XHttp i_RuleRemote)
    {
        this.ruleRemote = i_RuleRemote;
        this.isNeedInit = true;
        
        if ( !this.isLazyMode )
        {
            this.initRule();
        }
    }



    /**
     * 获取：规则会话：无状态的
     */
    public StatelessKieSession getKieSession()
    {
        return kieSession;
    }


    
    /**
     * 设置XJava池中对象的ID标识。此方法不用用户调用设置值，是自动的。
     * 
     * @param i_XJavaID
     */
    @Override
    public void setXJavaID(String i_XJavaID)
    {
        this.xjavaID = i_XJavaID;
    }



    /**
     * 获取XJava池中对象的ID标识。
     * 
     * @return
     */
    @Override
    public String getXJavaID()
    {
        return this.xjavaID;
    }


    
    /**
     * 获取：注释。可用于日志的输出等帮助性的信息
     */
    @Override
    public String getComment()
    {
        return comment;
    }


    
    /**
     * 设置：注释。可用于日志的输出等帮助性的信息
     * 
     * @param comment
     */
    @Override
    public void setComment(String comment)
    {
        this.comment = comment;
    }


    
    /**
     * 获取：是否为“懒汉模式”，即只在需要时才加载。默认为：false（预先加载模式）
     */
    public boolean isLazyMode()
    {
        return isLazyMode;
    }


    
    /**
     * 设置：是否为“懒汉模式”，即只在需要时才加载。默认为：false（预先加载模式）
     * 
     * @param isLazyMode
     */
    public void setLazyMode(boolean isLazyMode)
    {
        this.isLazyMode = isLazyMode;
    }
    
}
