import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;
import java.util.Base64.Encoder;

public class Client {
    Socket clientSocket;
    ObjectOutputStream oos;
    String clientDir;

    public Client(String serverIP, int serverPort, String cwd) throws Exception {
        out("Connecting to " + serverIP + ":" + serverPort + ". Current working dir is " + cwd);
        clientSocket = new Socket(serverIP, serverPort);
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        clientDir = cwd;

        try {
            watch();
        } catch (Exception ex) {
            out("WTF >_<");
        } finally {
            oos.close();
            clientSocket.close();
        }
    }

    public void sendMessage(Map<String, String> msg) throws Exception {
        oos.writeObject(msg);
    }

    private void watch() throws Exception {
        Map<String, Long> last_files = new HashMap<String, Long>();
        while (true) {
            Thread.sleep(1000);

            Map<String, Long> files = getFilesList();
            Map<String, String> changes = getChanges(last_files, files);
            last_files = files;
            
            for (Map.Entry<String, String> entry : changes.entrySet()) {
                String fname = entry.getKey();
                String change = entry.getValue();
                if (change.equals("file_added")) {
                    fileAdded(fname);
                } else if (change.equals("file_deleted")) {
                    fileDeleted(fname);
                } else {
                    out("WTF 0.o");
                }
            }
        }
    }

    private Map<String, String> getChanges(Map<String, Long> last_files, Map<String, Long> files) {        
        Map<String, String> changes = new HashMap<String, String>();

        for (Map.Entry<String, Long> entry : files.entrySet()) {
            String fname = entry.getKey();
            Long mtime = entry.getValue();
            if (!last_files.containsKey(fname) || mtime.compareTo((Long)last_files.get(fname)) > 0) {
                changes.put(fname, "file_added");
            }
        }

        for (Map.Entry<String, Long> entry : last_files.entrySet()) {
            String fname = entry.getKey();
            if (!files.containsKey(fname)) {
                changes.put(fname, "file_deleted");
            }
        }

        return changes;
    }

    private Map<String, Long> getFilesList() {
        File[] files = (new File(clientDir)).listFiles();
        HashMap<String, Long> files_list = new HashMap<String, Long>();
        for (File file : files) {            
            if(file.isFile()) {
                String fname = file.getName();
                long mtime = file.lastModified();
                files_list.put(fname, mtime);
            }            
        }
        return files_list;
    }

    private void fileAdded(String filename) throws Exception {
        out(filename + " added.");
        String data = new String(Base64.getEncoder().encode(Files.readAllBytes(Paths.get(clientDir,filename))), "UTF-8");
        Map<String, String> msg = new HashMap<String, String>();
        msg.put("type", "file_added");
        msg.put("filename", filename);
        msg.put("data", data);
        sendMessage(msg);
    }

    private void fileDeleted(String filename) throws Exception {
        out(filename + " deleted.");
        Map<String, String> msg = new HashMap<String, String>();
        msg.put("type", "file_deleted");
        msg.put("filename", filename);
        sendMessage(msg);
    }

    public static void out(String msg) {
        System.out.println(msg);
    }

    public static void main (String[] args) throws Exception {
        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        new Client(serverIP, serverPort, cwd);        
    }
}