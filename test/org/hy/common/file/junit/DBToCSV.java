package org.hy.common.file.junit;

import org.hy.common.db.DBOperation;
import org.hy.common.db.DBTableBigger;
import org.hy.common.db.DatabaseInfo;
import org.hy.common.file.FileHelp;





/**
 * 数据库数据直接导出为CSV文件
 *
 * @author   ZhengWei(HY)
 * @version  V1.0  2012-04-12
 */
public class DBToCSV 
{

	public static void main(String [] args) throws Exception
	{
		DatabaseInfo v_DBInfo = new DatabaseInfo();
		
		v_DBInfo.setDriver("oracle.jdbc.driver.OracleDriver");
		v_DBInfo.setUrl("jdbc:oracle:thin:@133.64.63.104:1521:UIDB2");
		v_DBInfo.setUser("ICD1");
		v_DBInfo.setPassword("icd1");
		
//		v_DBInfo.setDriver("oracle.jdbc.driver.OracleDriver");
//		v_DBInfo.setUrl("jdbc:oracle:thin:@127.0.0.1:1521:QHuoYun");
//		v_DBInfo.setUser("ICDRPT");
//		v_DBInfo.setPassword("icdrpt");

		
		
//		String v_SQL = "SELECT  A.UserName ,A.User_ID ,A.Password ,A.Default_Tablespace ,TO_CHAR(A.Created ,'YYYY-MM-DD HH24:MI:SS') FROM HY A ,HY B ,HY C ,HY D";
		String v_SQL = "SELECT A.CallID ,A.CallerNo ,A.CalleeNo ,A.AgentID ,TO_CHAR(A.BeginTime ,'YYYY-MM-DD HH24:MI:SS') AS BeginTime ,TO_CHAR(A.EndTime ,'YYYY-MM-DD HH24:MI:SS') AS EndTime FROM TRecordInfo1 A "; // WHERE A.BeginTime >= TO_DATE('2012-01-01' ,'YYYY-MM-DD') AND A.BeginTime <  TO_DATE('2012-01-02' ,'YYYY-MM-DD')";
		
		DBOperation.DATABASEINFO = v_DBInfo;
		DBOperation v_DBOpr = DBOperation.getInstance();
		
		
		
//		Connection    v_Conn          = v_DBOpr.getConnection();
//		Statement     v_Statement     = null;
//	    ResultSet     v_Resultset     = null;
//	    v_Statement = v_Conn.createStatement(ResultSet.TYPE_FORWARD_ONLY ,ResultSet.CONCUR_READ_ONLY);
//	    v_Resultset = v_Statement.executeQuery(v_SQL);
//	    System.out.println("-- Starting...");
	    
		
		
		DBTableBigger v_DBTableBigger = v_DBOpr.queryBiggerSQL(v_SQL);
  		System.out.println("Row Size = " + v_DBTableBigger.getRowSize());
		
  		
  		
		FileHelp v_FileHelp = new FileHelp();
		v_FileHelp.setOverWrite(true);
		v_FileHelp.createCSV("C:\\HY.csv" ,null ,v_DBTableBigger ,"GBK");
	}
	
}
