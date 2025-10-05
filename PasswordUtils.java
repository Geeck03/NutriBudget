import java.security.*;

public class PasswordUtils{

    /**
     * Hashes a password using SHA-256.
     *
     * @param password The plaintext password
     * @return The hashed password as a hex string
     */
    public static String hashPassword(String password){
        try{
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes){
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException("SHA-256 algorithm not available.", e);
        }
    }
}
