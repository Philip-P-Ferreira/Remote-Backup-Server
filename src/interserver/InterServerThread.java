import static commonutils.ServerProtocol.*;

import commonutils.TcpStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

public class InterServerThread implements Runnable {
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
      HashMap<String, String> requestMap =
          createProtocolMap(clientRequest, PAIR_DELIM, PAIR_SEPARATOR);

      // try to get response from storage
      String storageResponseStr = "";
      try {
        storageResponseStr = InterServer.forwardToStorage(clientRequest);
      } catch (IOException e) {
        System.out.println("Could not connect to storage");
      }
      HashMap<String, String> storageResponseMap = new HashMap<>();
      storageResponseMap = createProtocolMap(storageResponseStr, PAIR_DELIM, PAIR_SEPARATOR);

      // a response map to send back bad status if needed
      HashMap<String, String> badStorageStat = new HashMap<>();
      badStorageStat.put(STATUS_KEY, STATUS_BAD_STORAGE_VAL);

      // check if response was good and check request type
      boolean badStorage = storageResponseStr.isEmpty();
      switch (requestMap.get(REQUEST_KEY)) {
        case UPLOAD_START_VAL:
          if (badStorage) {
            sendProtocolMessage(clientStream, UPLOAD_START_ACK_VAL, badStorageStat);
          } else {
            System.out.println("Starting upload stream...");
            clientStream.writeMessage(storageResponseStr);
            handleUpload(requestMap, storageResponseMap);
          }
          break;
        case REQUEST_DOWNLOAD_VAL:
          if (badStorage) {
            sendProtocolMessage(clientStream, REQUEST_DOWNLOAD_ACK_VAL, badStorageStat);
          } else {
            System.out.println("Requesting download...");
            clientStream.writeMessage(storageResponseStr);
            handleDownload(storageResponseStr);
          }
          break;
        default:
          break;
      }

      clientStream.close();

    } catch (IOException e) {
      System.out.println("Could not reach client: " + e.getMessage());
    }
  }

  private void handleUpload(HashMap<String, String> request, HashMap<String, String> response)
      throws IOException {
    String status = response.get(STATUS_KEY);
    if (status != null && status.equals(STATUS_OK_VAL)) {
      long fileSize = Long.parseLong(request.get(FILE_SIZE_KEY));
      InterServer.streamToStorage(clientStream, fileSize);
      System.out.println("End of upload stream");

      clientStream.writeMessage(InterServer.getMessageFromStorage());
      clientStream.close();
    } else {
      System.out.println("File alreay exists on storage");
    }
  }

  private void handleDownload(String storageRes) throws IOException {
    HashMap<String, String> storageResMap =
        createProtocolMap(storageRes, PAIR_DELIM, PAIR_SEPARATOR);
    InterServer.forwardToStorage(clientStream.readMessage());

    System.out.println("Starting download stream...");
    InterServer.streamToClient(clientStream, Long.parseLong(storageResMap.get(FILE_SIZE_KEY)));
    System.out.println("Download complete");
  }
}
