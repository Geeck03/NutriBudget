package bridge;

import py4j.ClientServer;
import py4j.Py4JNetworkException;


public class Py4JHelper {
    private static ClientServer client = null;
    private static boolean attempted = false;
    private static final Object LOCK = new Object();

    public static IKrogerWrapper getWrapper() {
        synchronized (LOCK) {
            if (client != null) {
                try {
                    return (IKrogerWrapper) client.getPythonServerEntryPoint(new Class[]{IKrogerWrapper.class});
                } catch (Throwable t) {

                }
            }
            if (attempted && client == null) {
                System.err.println("Kroger Python bridge previously failed to start — skipping repeated attempts.");
                return null;
            }
            attempted = true;
            try {
                client = new ClientServer.ClientServerBuilder()
                    .javaPort(0)        // any available local port
                    .pythonPort(25334)  // must match Python server port
                    .build();

                System.out.println("✅ Py4J client connected/reused.");
                return (IKrogerWrapper) client.getPythonServerEntryPoint(new Class[]{IKrogerWrapper.class});
            } catch (Py4JNetworkException ex) {
                System.err.println("Kroger Python bridge not reachable: " + ex.getMessage());
                client = null;
                return null;
            } catch (Throwable t) {
                t.printStackTrace();
                client = null;
                return null;
            }

        }
    }

    

    public static void shutdown() {
        synchronized (LOCK) {
            if (client != null) {
                try { client.shutdown(); } catch (Exception ignored) {}
                client = null;
            }
        }
    }
}