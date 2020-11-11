import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import static commonutils.ServerProtocol.*;

import commonutils.TcpStream;

public class InterServerThread implements Runnable{
    
    private TcpStream clientStream;

    // Constructor
    public InterServerThread(Socket socket) throws IOException {
        clientStream = new TcpStream(socket);
    }

    // Server thread "main method"
    public void run() {

        // try to read from client
        try {
            // get request from client
            String clientRequest = clientStream.readMessage();
            HashMap<String,String> requestMap = createProtocolMap(clientRequest, PAIR_DELIM, PAIR_SEPARATOR);

            // try to get response from storage
            HashMap<String,String> storageResponseMap = new HashMap<>();
            try {
                storageResponseMap = InterServer.forwardToStorage(clientRequest);
            } catch (IOException e) {
                System.out.println("Could not connect to storage");
            }

            // a response map to send back status if needed
            HashMap<String,String> badStorageResMap = new HashMap<>();
            badStorageResMap.put(STATUS_KEY, STATUS_BAD_STORAGE_VAL);

            // check if response was good and check request type
            boolean badStorage = storageResponseMap.isEmpty();
            switch (requestMap.get(REQUEST_KEY)) {
                
                case UPLOAD_START_VAL:
                    if (badStorage) {
                        sendProtocolMessage(clientStream, UPLOAD_START_ACK_VAL, badStorageResMap);
                    } else {
                        System.out.println("Starting upload stream...");
                        sendProtocolMessage(clientStream, UPLOAD_START_ACK_VAL, storageResponseMap);
                        handleUpload(requestMap);
                    }

                default:
                    break;
            }

            clientStream.close();
            

            
        } catch (IOException e) {
            System.out.println("Could not reach client");
        }
    }

    private void handleUpload(HashMap<String,String> req) throws IOException {
        InterServer.streamToStorage(clientStream);
        reconnectClient();

        String endReqStr = clientStream.readMessage();
        HashMap<String,String> endResMap = InterServer.forwardToStorage(endReqStr);

        sendProtocolMessage(clientStream, UPLOAD_END_ACK_VAL, endResMap);
    }

    private void reconnectClient() throws IOException {
        clientStream = new TcpStream(InterServer.clientSocket.accept());
    }
}
