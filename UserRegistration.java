import java.sql.*;

public class UserRegistration{

    private static final String DB_URL = "jdbc:mysql://localhost:3306/?";
    private static final String DB_USER = "?";
    private static final String DB_PASSWORD = "?";
	//Need "?" to be filled out
	
    /**
     * Attempts to register a new user.
     *
     * @param username The entered username
     * @param password The entered password
     * @param reenteredPassword The re-typed password
     * @return true if user successfully registered, false otherwise
     */
    public boolean registerUser(String username, String password, String reenteredPassword) {
        if(!password.equals(reenteredPassword)){
            System.out.println("Passwords do not match.");
            return false;
        }

        if(userExists(username)){
            System.out.println("Username already exists.");
            return false;
        }

        // Hash the password before inserting
        String hashedPassword = PasswordUtils.hashPassword(password);
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)){

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e){
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    private boolean userExists(String username){
        String sql = "SELECT * FROM users WHERE username = ?";

        try(Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e){
            System.err.println("Database error: " + e.getMessage());
            return true; //be safe: assume user exists if DB fails
        }
    }
}
