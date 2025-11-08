package bridge;

public interface IKrogerWrapper {
    // Return a JSON string
    String search(String query, int limit);
}
