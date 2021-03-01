package lu.op.cellar.sec.saga.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

public class SagalogUtilities {
	
	public static String param1 = "jdbc:postgresql://localhost:5432/postgres";
	public static String param2 = "postgres";
    public static String param3 ="cellar";

    
	public static String[] retrieveState(String saga_id, String activity_id) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		Statement st = c.createStatement();
		ResultSet rs = st.executeQuery("select saga_id, state, time_stamp, compensation, params from sagalog where saga_id = '"+saga_id+"' and activity_id = '"+activity_id+"'");
		rs.next();
		String[] result = new String[3];
		result[0] = rs.getString("state"); //state
	    result[1] = rs.getString("compensation"); //compensation
	    result[2] = rs.getString("params"); //params
	    c.close();
		return result;
		
	}
	public static void writeRecord(String saga_id, String state, boolean compensation, String activity_id, String params) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		PreparedStatement st = c.prepareStatement("INSERT INTO SAGALOG (saga_id, state, time_stamp, compensation, activity_id, params) VALUES (?, ?, ?, ?, ?, ?)");
		st.setString(1, saga_id);
		st.setString(2, state);
		st.setTimestamp(3, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		st.setBoolean(4, compensation);
		st.setString(5, activity_id);
		st.setString(6, params);
		st.execute();
		st.close();
		c.close();
	}
	
}