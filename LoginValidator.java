import java.sql.*;

public class LoginValidator{

    private static final String DB_URL = "jdbc:mysql://localhost:3306/?";
    private static final String DB_USER = "?";
    private static final String DB_PASSWORD = "?";
	//Need "?" to be filled out

    /**
     * Checks if username and password match a user in the database.
     *
     * @param username Entered username
     * @param password Entered password (plaintext from GUI)
     * @return true if valid user, false otherwise
     */
	 
    public boolean validateLogin(String username, String password){
        String sql = "SELECT password FROM users WHERE username = ?";

        try(Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()){
                String storedHash = rs.getString("password");
                String enteredHash = PasswordUtils.hashPassword(password);
                return storedHash.equals(enteredHash);
            }
            return false;

        } catch (SQLException e){
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
}
