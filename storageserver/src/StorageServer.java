import java.io.*;
import java.net.*;
import commonutils.*;

public class StorageServer {

    public static final String STORAGE_PATH = "";
    public static final String ZIP_SUFFIX = ".zip";
    public static final File STORAGE_PATH_FILE = new File(STORAGE_PATH);

    public static void main(String[] args) throws IOException {
        
        // create server socket at PORT
        System.out.println("\nStarting up Storage Server...");
        ServerSocket storageServerSocket = new ServerSocket(ServerProtocol.PORT);

        // server always on
        boolean serverOn = true;
        while (serverOn) {
            try {
                // thread created when client (interserver) connects to server
                Thread storageServerThread = new Thread(new StorageServerThread(storageServerSocket.accept()));
                storageServerThread.start();
            } catch (IOException e) {
                System.out.println("Failed to connect to client");
            }
        }
        storageServerSocket.close();
    }
}