import java.io.*;
import java.net.*;
import java.util.HashMap;

import commonutils.TcpStream;

import static commonutils.ServerProtocol.*;

public class StorageTest {
    public static void main(String[] args) throws IOException {
       
        // establish server socket and storage socket
        ServerSocket sSocket = new ServerSocket(STORAGE_PORT);
        TcpStream storageStream = new TcpStream(sSocket.accept());

        // test file name
        String filename = "folder";
        String filepath = "iofiles/out/folder.zip";

        // singal storage to start upload
        HashMap<String,String> req = new HashMap<>();
        req.put(FILENAME_KEY, filename);
        sendProtocolMessage(storageStream, UPLOAD_START_VAL, req);
        System.out.println(storageStream.readMessage());

        // write file to socket, reaccept socket
        FileInputStream fileIn = new FileInputStream(filepath);
        storageStream.writeFromInputStream(fileIn);
        fileIn.close();
        storageStream = new TcpStream(sSocket.accept());

        // signal server that it is done
        req.clear();
        sendProtocolMessage(storageStream, UPLOAD_END_VAL, req);
        System.out.println(storageStream.readMessage());

        // wrap things up
        storageStream.close();
        sSocket.close();
    }
}
