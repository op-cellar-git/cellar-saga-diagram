package lu.op.cellar.sec.saga.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SagalogUtilities {
	
	public static String param1 = "jdbc:postgresql://localhost:5432/postgres";
	public static String param2 = "postgres";
    public static String param3 ="cellar";
    public static String timeoutQuery = "select a.saga_id,a.activity_id, now()-a.time_stamp \r\n" + 
    		"from sagalog a,\r\n" + 
    		"(\r\n" + 
    		"select saga_id, activity_id\r\n" + 
    		"from sagalog\r\n" + 
    		"where state != 'startup' and state != 'end'\r\n" + 
    		"group by saga_id, activity_id \r\n" + 
    		"having count(*)=1\r\n" + 
    		") b\r\n" + 
    		"where a.saga_id = b.saga_id and a.activity_id = b.activity_id and a.state like %s and (now()-a.time_stamp)::time > %s::time";

    public static List<String> timeouts(String activity, String threshold) throws ClassNotFoundException, SQLException {
    	String query = String.format(timeoutQuery, activity, threshold);
    	Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		//System.out.println(query);
		Statement st = c.createStatement();
		ResultSet rs = st.executeQuery(query);
		List<String> result =new  ArrayList<String>();
		while(rs.next()) {
			result.add(rs.getString("saga_id")+"|"+rs.getString("activity_id")); //saga.id|activity.id
		}
		c.close();
		return result;
    }
    
	public static String[] retrieveState(String saga_id, String activity_id) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		Statement st = c.createStatement();
		ResultSet rs = st.executeQuery("select saga_id, state, time_stamp, compensation from sagalog where saga_id = '"+saga_id+"' and activity_id = '"+activity_id+"'");
		rs.next();
		String[] result = new String[2];
		result[0] = rs.getString("state"); //state
	    result[1] = rs.getString("compensation"); //compensation
	    c.close();
		return result;
		
	}
	public static void writeRecord(String saga_id, String state, boolean compensation, String activity_id) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		PreparedStatement st = c.prepareStatement("INSERT INTO SAGALOG (saga_id, state, time_stamp, compensation, activity_id) VALUES (?, ?, ?, ?, ?)");
		st.setString(1, saga_id);
		st.setString(2, state);
		st.setTimestamp(3, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		st.setBoolean(4, compensation);
		st.setString(5, activity_id);
		st.execute();
		st.close();
		c.close();
	}
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		//System.out.println(SagalogUtilities.timeouts("'PrelockValidation%'", "'01:00:00'"));
		
		System.out.println("porco|dio".split("r")[0]);
	}
}