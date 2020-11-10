import java.io.*;
import java.util.HashMap;

import commonutils.TcpStream;

import static commonutils.ServerProtocol.*;

public class StorageServer {

    public static final String STORAGE_PATH = "~/usb/storage_root";
    public static final File STORAGE_PATH_FILE = new File(STORAGE_PATH);

    private static TcpStream interStream;

    public static void main(String[] args) throws IOException {
        
        // create server socket at PORT
        System.out.println("\nStarting up Storage Server...");
        interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);

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
                System.out.println("Failed to connect to client");
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

        // write socket in to file, reopen socket
        FileOutputStream outToFile = new FileOutputStream(filenameZip);
        interStream.readToOutputStream(outToFile);
        interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);

        // send ack for ending
        System.out.println(interStream.readMessage());
        sendProtocolMessage(interStream, UPLOAD_END_ACK_VAL, res);
    }
}