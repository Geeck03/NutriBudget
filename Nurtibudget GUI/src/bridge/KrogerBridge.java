package bridge;

import py4j.ClientServer;
import py4j.Py4JNetworkException;
import org.json.JSONArray;
import org.json.JSONObject;

public class KrogerBridge {
    public static void main(String[] args) {
        ClientServer clientServer = null;

        try {
            System.out.println("🔌 Connecting to Python on port 25333...");

            clientServer = new ClientServer(null);
            IKrogerWrapper pythonEntry = (IKrogerWrapper) clientServer.getPythonServerEntryPoint(
                    new Class[]{IKrogerWrapper.class}
            );

            System.out.println("✅ Connected to Python!");

            // Call Python search
            String jsonString = pythonEntry.search("Banana", 10);

            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.getString("name");
                double price = obj.getDouble("price");

                System.out.println("🍌 " + name + " - $" + price);
            }

        } catch (Py4JNetworkException e) {
            System.err.println("❌ Could not connect to Python server!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (clientServer != null) {
                clientServer.shutdown();
                System.out.println("🔌 Disconnected from Python.");
            }
        }
    }
}
