package org.hy.common.file.junit;

import org.hy.common.Date;
import org.hy.common.Des;
import org.hy.common.app.AppParameter;
import org.hy.common.file.FileHelp;





public class Code
{
    
    public static void main(String [] i_Args)
    {
        AppParameter v_AppParams = new AppParameter(i_Args);
        
        String v_Type = "d";
        if ( v_AppParams.isExists("type") )
        {
            v_Type = v_AppParams.getParamValue("type");
        }
        
        if ( !v_AppParams.isExists("key") )
        {
            System.out.println("Is not find key.");
            return;
        }
        
        Des v_Des = new Des(v_AppParams.getParamValue("key"));
        
        if ( v_AppParams.isExists("code") )
        {
            for (String v_Arg : i_Args)
            {
                if ( v_Arg.indexOf("=") >= 0 )
                {
                    String [] v_Params = v_Arg.split("=");
                    if ( v_Params.length >= 2 && "code".equalsIgnoreCase(v_Params[0]) )
                    {
                        String v_Code = v_Arg.substring(v_Arg.indexOf("=") + 1);
                        
                        if ( "d".equalsIgnoreCase(v_Type) )
                        {
                            System.out.println("[" + v_Des.encrypt(v_Code) + "]");
                        }
                        else
                        {
                            System.out.println("[" + v_Des.decrypt(v_Code) + "]");
                        }
                    }
                }
            }
        }
        else if ( v_AppParams.isExists("file") )
        {
            FileHelp v_FileHelp = new FileHelp();
            String   v_Code     = "";
            String   v_FileName = v_AppParams.getParamValue("file") + "." + Date.getNowTime().getFull_ID();
            
            try
            {
                v_Code = v_FileHelp.getContent(v_AppParams.getParamValue("file") ,"UTF-8");
                if ( "d".equalsIgnoreCase(v_Type) )
                {
                    v_Code = v_Des.encrypt(v_Code);
                }
                else
                {
                    v_Code = v_Des.decrypt(v_Code);
                }
                v_FileHelp.create(v_FileName ,v_Code);
                
                System.out.println("Create file is [" + v_FileName + "].");
            }
            catch (Exception exce)
            {
                exce.printStackTrace();
            }
        }
        else
        {
            System.out.println("Is not find code and file.");
            return;
        }
    }
    
    
    
    
    
}
