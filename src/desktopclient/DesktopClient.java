import java.util.HashMap;
import java.io.*;
import commonutils.TcpStream;
import static commonutils.ServerProtocol.*;

public class DesktopClient {
    public static void main(String[] args) throws IOException {
        // establish respective sockets and storage stream
        TcpStream interServerStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);

        // test file name
        String filename = "folder";
        String filepath = "iofiles/out/folder.zip";

        // singal storage to start upload
        HashMap<String,String> req = new HashMap<>();
        req.put(FILENAME_KEY, filename);
        sendProtocolMessage(interServerStream, UPLOAD_START_VAL, req);
        System.out.println(interServerStream.readMessage());

        // write file to socket, reopen TcpStream
        FileInputStream fileIn = new FileInputStream(filepath);
        interServerStream.writeFromInputStream(fileIn);
        fileIn.close();

        // wrap things up
        interServerStream.close();
    }
}
