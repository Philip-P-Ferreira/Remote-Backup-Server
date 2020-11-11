import java.io.*;
import java.util.HashMap;

import commonutils.TcpStream;

import static commonutils.ServerProtocol.*;

public class StorageServer {

    // file constants
    public static final String STORAGE_PATH = "iofiles/in/";
    public static final File STORAGE_PATH_FILE = new File(STORAGE_PATH);

    // instance tcp stream
    private static TcpStream interStream;

    public static void main(String[] args) throws IOException {
        
        // create server socket at PORT
        System.out.println("\nStarting up Storage Server...");
        interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);
        System.out.println("Connected to intermediate server");

        HashMap<String,String> requestMap;
        // server always on
        boolean serverOn = true;
        while (serverOn) {
            try {
                requestMap = createProtocolMap(interStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);
                
                switch (requestMap.get(REQUEST_KEY)) {
                    
                    case UPLOAD_START_VAL:
                        handleUpload(requestMap);
                        serverOn = false;
                        break;
                    
                        default:
                        break;
                }

                
            } catch (IOException e) {
                System.out.println("Failed to connect to inter server");
                serverOn = false;
            }
        }
        interStream.close();
    }

    private static void handleUpload(HashMap<String,String> req) throws IOException {
        
        // get name of file from request
        String filename = STORAGE_PATH + req.get(FILENAME_KEY);
        String filenameZip = filename + ZIP_SUFFIX;

        // send ack back
        HashMap<String,String> res = new HashMap<>();
        res.put(STATUS_KEY, STATUS_OK_VAL);
        sendProtocolMessage(interStream, UPLOAD_START_ACK_VAL, res);
        System.out.println("Ready to receive upload...");

        // write socket in to file, reopen socket
        FileOutputStream outToFile = new FileOutputStream(filenameZip);
        interStream.readToOutputStream(outToFile);
        interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);
        System.out.println("File received, reconnected to server");
    }
}