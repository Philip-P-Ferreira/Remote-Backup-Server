import static commonutils.ServerProtocol.*;

import commonutils.TcpStream;
import java.io.*;
import java.net.*;

public class InterServer
{
    public static ServerSocket storageSocket;
    public static ServerSocket clientSocket;

    public static TcpStream storageStream;

    public static void main(String[] args) throws IOException
    {
        // establish respective sockets and storage stream
        storageSocket = new ServerSocket(STORAGE_PORT);
        clientSocket = new ServerSocket(CLIENT_PORT);
        System.out.println('\n' + InterServerStrings.STARTING_UP_MSG);

        // accept storage server
        connectStorage();

        // server always on
        boolean serverOn = true;
        while (serverOn)
        {
            try
            {
                // thread created when a client connects to server
                Thread serverThread = new Thread(new InterServerThread(clientSocket));
                serverThread.start();
            }
            catch (IOException e)
            {
                System.out.printf(InterServerStrings.ACCEPT_FAIL_FORMAT + '\n', e.getMessage());
            }
        }
        storageStream.close();
        storageSocket.close();
        clientSocket.close();
    }

    /**
     * forwardToStorage -
     * Forward a String message to storage server. Is synchronized
     *
     * @param req - String to forward
     * @throws IOException
     */
    public static synchronized void forwardToStorage(String req) throws IOException
    {
        if (storageStream == null)
        {
            throw new IOException(InterServerStrings.NO_STREAM_TO_STORAGE_MSG);
        }
        storageStream.writeMessage(req);
    }

    /**
     * getMessageFromStorage -
     * Gets a String message from storage server
     *
     * @return - String message from storage
     * @throws IOException
     */
    public static synchronized String getMessageFromStorage() throws IOException
    {
        return storageStream.readMessage();
    }

    public static synchronized String duplexFowardMsg(String req)
    {
        String res = "";
        try
        {
            forwardToStorage(req);
            res = getMessageFromStorage();
        }
        catch (IOException e)
        {
            System.out.println(InterServerStrings.NO_CONNECT_STORAGE_MSG);
        }

        return res;
    }

    /**
     * streamToStorage -
     * Streams a steam of bytes from given TcpStream to storage
     *
     * @param clientStream - Source stream of data
     * @param sizeInBytes - Size of byte stream
     * @throws IOException
     */
    public static synchronized void streamToStorage(TcpStream clientStream, long sizeInBytes) throws IOException
    {
        clientStream.pipeTcpStreams(storageStream, sizeInBytes);
    }

    /**
     * streamToClient -
     * Streams byte stream from storage to given TcpStream
     *
     * @param clientStream - Stream to receive stream
     * @param sizeInBytes - size of byte stream
     * @throws IOException
     */
    public static synchronized void streamToClient(TcpStream clientStream, long sizeInBytes) throws IOException
    {
        storageStream.pipeTcpStreams(clientStream, sizeInBytes);
    }

    /**
     * connectStorage -
     * Attempts to connect to storage server and establish tcp stream
     *
     */
    public static void connectStorage()
    {
        Runnable connectStorage = new Runnable() {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        storageStream = new TcpStream(storageSocket);
                    }
                    catch (IOException ignored)
                    {
                    }
                    System.out.println(InterServerStrings.CONNECTED_STORAGE_MSG);
                }
            }
        };
        Thread thread = new Thread(connectStorage);
        thread.start();
    }
}
