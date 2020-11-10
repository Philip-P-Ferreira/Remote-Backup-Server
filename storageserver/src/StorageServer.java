import java.io.*;
// import java.net.*;
import java.util.*;

// import commonutils.*;

public class StorageServer {

    public static final String STORAGE_PATH = "/mnt/serverstorage/";
    public static final File STORAGE_PATH_FILE = new File(STORAGE_PATH);

    private static Date lastWriteTime = new Date();
    private static long totalSpace = STORAGE_PATH_FILE.getTotalSpace();
    private static long freeSpace = STORAGE_PATH_FILE.getFreeSpace();
    public static void main(String[] args) throws IOException {
         
        System.out.println("\nStarting up Storage Server...");
        // ServerSocket storageServerSocket = new ServerSocket(ServerProtocol.PORT);

        System.out.println("Free space: " + freeSpace + ", total space: " + totalSpace 
        + ". Last write: " + lastWriteTime.toString());

        //server always on
    }
}