import java.io.*;
import java.net.*;
import java.util.*;
// import java.util.zip.ZipInputStream;

import commonutils.*;

public class StorageServerThread implements Runnable {
    
    TcpStream interServerStream;

    public StorageServerThread(Socket socket) throws IOException {
        interServerStream = new TcpStream(socket);
    }

    // "main method" of each thread
    public void run() {

        System.out.println('\n' + interServerStream.getIpAddress() + '@' + interServerStream.getPort() + " -> ");

        try {
        // extract request type
         HashMap<String,String> requestMap = 
            ServerProtocol.createProtocolMap(interServerStream.readMessage(), ServerProtocol.PAIR_DELIM, ServerProtocol.PAIR_SEPARATOR);
        
        // handle based on type
        switch (requestMap.get(ServerProtocol.REQUEST_KEY)) {
        }
         

        } catch (IOException e) {
            System.out.println("Could not reach client");
        }
    }
}
