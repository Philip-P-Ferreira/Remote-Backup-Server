import java.io.*;
import java.net.*;

import commonutils.TcpStream;

import static commonutils.ServerProtocol.*;

public class InterServer {

    public static ServerSocket storageSocket;
    public static ServerSocket clientSocket;

    public static TcpStream storageStream;

    public static void main(String[] args) throws IOException {

        // establish respective sockets and storage stream
        storageSocket = new ServerSocket(STORAGE_PORT);
        clientSocket = new ServerSocket(CLIENT_PORT);
        System.out.println("Starting up intermediate server...");

        // accept storage server
        storageStream = new TcpStream(storageSocket.accept());
        System.out.println("Connected to storage server");

        // server always on
        boolean serverOn = true;
        while (serverOn) {
            try {
                // thread created when a client connects to server
                Thread serverThread = new Thread(new InterServerThread(clientSocket.accept()));
                serverThread.start();
            } catch (IOException e) {
                System.out.println("Failed to accept");
                e.printStackTrace();
            }
        }
        storageStream.close();
        storageSocket.close();
        clientSocket.close();
    }

    public static synchronized String forwardToStorage(String req) throws IOException {

        storageStream.writeMessage(req);
        return getMessageFromStorage();
    }

    public static synchronized String getMessageFromStorage() throws IOException {
        return storageStream.readMessage();
    }

    public static synchronized void streamToStorage(TcpStream clientStream, long sizeInBytes) throws IOException {
        clientStream.pipeTcpStreams(storageStream, sizeInBytes);
    }
} 
