package bridge;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Small CLI / test harness to exercise the Py4J bridge.
 *
 * Improvements over the previous main:
 * - Reuses Py4JHelper.getWrapper() (so it integrates with the project's existing connection logic).
 * - Adds reconnect/retry logic with backoff.
 * - Supports commands:
 *     search <query> [limit]
 *     createRecipe <name>
 *     exit
 *   Also supports interactive mode if run without args.
 * - Handles Python-side missing-method errors gracefully and prints useful diagnostics.
 *
 * Usage examples:
 *   java bridge.KrogerBridge search "Banana" 10
 *   java bridge.KrogerBridge createRecipe "My Recipe"
 *   java bridge.KrogerBridge            <-- interactive
 */
public class KrogerBridge {
    public static void main(String[] args) {
        try {
            IKrogerWrapper wrapper = waitForWrapper(5, 1000);
            if (wrapper == null) {
                System.err.println("❌ Unable to obtain Python wrapper after retries. Exiting.");
                return;
            }

            if (args.length == 0) {
                interactiveLoop(wrapper);
            } else {
                // Single-shot mode: treat args[0] as command
                String cmd = args[0].trim().toLowerCase();
                if ("search".equals(cmd)) {
                    String q = args.length > 1 ? args[1] : "Banana";
                    int limit = args.length > 2 ? parseIntOrDefault(args[2], 10) : 10;
                    runSearch(wrapper, q, limit);
                } else if ("createrecipe".equals(cmd) || "createRecipe".equals(args[0])) {
                    String name = args.length > 1 ? args[1] : "New recipe";
                    runCreateRecipe(wrapper, name);
                } else {
                    System.err.println("Unknown command: " + args[0]);
                    System.err.println("Supported commands: search <q> [limit], createRecipe <name>");
                }
            }
        } finally {
            // Best-effort shutdown of Py4J client held by Py4JHelper
            try {
                Py4JHelper.shutdown();
            } catch (Throwable ignored) {}
            System.out.println("🔌 Exiting.");
        }
    }

    private static IKrogerWrapper waitForWrapper(int attempts, long delayMs) {
        for (int i = 1; i <= attempts; i++) {
            try {
                IKrogerWrapper w = Py4JHelper.getWrapper();
                if (w != null) {
                    System.out.println("✅ Py4J wrapper obtained.");
                    return w;
                }
            } catch (Throwable t) {
                System.err.println("Attempt " + i + " failed to get wrapper: " + t.getMessage());
            }
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs * i); // simple backoff
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    private static void interactiveLoop(IKrogerWrapper wrapper) {
        System.out.println("Interactive mode. Commands: search <q> [limit], createRecipe <name>, exit");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) break;

                String[] parts = line.split("\\s+", 3);
                String cmd = parts[0].toLowerCase();
                try {
                    if ("search".equals(cmd)) {
                        String q = parts.length > 1 ? parts[1] : "";
                        int limit = 10;
                        if (parts.length > 2) limit = parseIntOrDefault(parts[2], 10);
                        runSearch(wrapper, q, limit);
                    } else if ("createrecipe".equals(cmd) || "createRecipe".equals(cmd)) {
                        String name = parts.length > 1 ? line.substring(cmd.length()).trim() : "New recipe";
                        runCreateRecipe(wrapper, name);
                    } else {
                        System.out.println("Unknown command: " + cmd);
                    }
                } catch (Throwable t) {
                    System.err.println("Command failed: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("Interactive loop error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runSearch(IKrogerWrapper wrapper, String query, int limit) {
        try {
            System.out.println("🔎 Searching for \"" + query + "\" (limit=" + limit + ")...");
            String jsonString = wrapper.search(query, limit);
            if (jsonString == null || jsonString.trim().isEmpty()) {
                System.out.println("No results (empty response).");
                return;
            }
            JSONArray array = new JSONArray(jsonString);
            System.out.println("Found " + array.length() + " items:");
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "(no name)");
                String price = obj.has("price") ? String.valueOf(obj.optDouble("price", 0.0)) : "(no price)";
                System.out.printf(" - %s  |  price: %s%n", name, price);
            }
        } catch (Throwable t) {
            System.err.println("Search failed: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void runCreateRecipe(IKrogerWrapper wrapper, String name) {
        try {
            System.out.println("➕ Requesting Python to create recipe: " + name);
            int id = -1;
            try {
                id = wrapper.createRecipe(name);
            } catch (Throwable t) {
                // If Python does not implement createRecipe you'll get an exception from Py4J.
                System.err.println("createRecipe call failed: " + t.getMessage());
                throw t;
            }
            if (id > 0) {
                System.out.println("✅ Python created recipe id: " + id);
            } else {
                System.err.println("⚠ Python returned non-positive id: " + id);
            }
        } catch (Throwable t) {
            System.err.println("Create recipe failed: " + t.getMessage());
            t.printStackTrace();
            System.err.println("Hint: ensure the Python entry-point implements 'createRecipe(name)'");
        }
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
}