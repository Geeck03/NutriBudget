package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PythonBridge {

    /**
     * Run a python script and send username/password via stdin (each on its own line).
     * Returns the trimmed stdout of the process, or null on error.
     */
    public static String runWithStdin(String scriptPath, String command, String username, String password) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("python");
            cmd.add(scriptPath);
            cmd.add(command);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("user.dir")));
            Process p = pb.start();

            // write credentials to stdin
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
                bw.write(username == null ? "" : username);
                bw.newLine();
                bw.write(password == null ? "" : password);
                bw.newLine();
                bw.flush();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = p.getInputStream();
            byte[] buffer = new byte[4096];
            int r;
            while ((r = is.read(buffer)) != -1) baos.write(buffer, 0, r);
            p.waitFor();
            return new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
