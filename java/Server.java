import java.nio.file.Paths;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Map;

public class Server {
    class ClientHandler extends Thread {
        Socket clientSocket;
        String clientDir;
        ObjectInputStream ois;

        public ClientHandler(String dir, Socket socket) throws Exception {
            clientDir = dir;
            clientSocket = socket;
            ois = new ObjectInputStream(socket.getInputStream());            
        }

        private void saveFile(String filename, String data) throws Exception {
            if (Files.notExists(Paths.get(clientDir))) {
                Files.createDirectory(Paths.get(clientDir));
                out(clientDir + " created.");
            }
            Files.write(Paths.get(clientDir, filename), data.getBytes());
        }

        private void deleteFile(String filename) throws Exception {
            Files.deleteIfExists(Paths.get(clientDir, filename));
        }

        @SuppressWarnings (value="unchecked")
        private Map<String, String> getMessage() throws Exception {            
            return (Map<String, String>) ois.readObject();
        }

        @Override
        public void run() {
            boolean done = false;      
            while(!done) {
                try {
                    Map msg = getMessage();
                    String type = msg.get("type").toString();
                    if (type.equals("file_added")) {
                        String filename = msg.get("filename").toString();
                        String data = msg.get("data").toString();
                        saveFile(filename, data);
                        out(filename + " added.");
                    } else if (type.equals("file_deleted")) {
                        String filename = msg.get("filename").toString();
                        deleteFile(filename);
                        out(filename + " deleted.");
                    } else {
                        out("Got " + type + " message.");
                    }
                } catch (Exception ex) {
                    out(ex.getMessage());
                    done = true;
                }
            }

            try {
                ois.close();
                clientSocket.close();
            } catch(Exception ex) {
                out("WTF >_<");
            }               
        }
    }

    ServerSocket serverSocket;
    String serverDir;

    public Server(int port, String cwd) throws Exception {
        out("Listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port + ". Current working dir is " + cwd + ".");
        serverSocket = new ServerSocket(port);
        serverDir = cwd;
        while (true) {
            Socket socket = serverSocket.accept();
            out(getPrettyUserAddress(socket) + " just connected.");
            (new ClientHandler(getUserDir(socket), socket)).start();
        }
    }

    private String getPrettyUserAddress(Socket socket) {
        return socket.getRemoteSocketAddress().toString().toLowerCase().replaceAll(" ", "_").replaceAll(":","_");
    }

    private String getUserDir(Socket socket) {
        String addr = getPrettyUserAddress(socket);
        return Paths.get(serverDir, addr).toString();
    }

    private static void out(String msg) {
        System.out.println(msg);
    }

    public static void main (String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String cwd = Paths.get(".").toAbsolutePath().normalize().toString();
        new Server(port, cwd);        
    }
}