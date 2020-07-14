package lu.op.cellar.sec.saga.logic;

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

	public static String[] retrieveState(String saga_id) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		Statement st = c.createStatement();
		ResultSet rs = st.executeQuery("select saga_id, state, max(time_stamp) as time_stamp, compensation from sagalog where saga_id = '"+saga_id+"' group by saga_id, state, compensation");
		rs.next();
		String[] result = new String[2];
		result[0] = rs.getString("state"); //state
	    result[1] = rs.getString("compensation"); //compensation
		return result;
		
	}
	public static void writeRecord(String saga_id, String state, boolean compensation) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		Connection c = DriverManager.getConnection(param1, param2,  param3);
		PreparedStatement st = c.prepareStatement("INSERT INTO SAGALOG (saga_id, state, time_stamp, compensation) VALUES (?, ?, ?, ?)");
		st.setString(1, saga_id);
		st.setString(2, state);
		st.setTimestamp(3, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		st.setBoolean(4, compensation);
		st.executeUpdate();
		st.close();

	}
	
}