import static commonutils.ServerProtocol.*;

import commonutils.FileUtils;
import commonutils.TcpStream;
import java.io.*;
import java.util.HashMap;

public class StorageServer {
  // file constants
  public static final String STORAGE_PATH = "iofiles/in/";
  public static final File STORAGE_PATH_FILE = new File(STORAGE_PATH);

  // number constants
  private static final int SLEEP_TIME = 1000;
  private static final int REPEAT_COUNT = 4;

  // instance tcp stream
  private static TcpStream interStream;
  private static int attemptCount = 0; // keep track of connect attempts

  public static void main(String[] args) throws IOException {
    // vars to keep track of server connection status
    boolean connectedToServer = false;
    HashMap<String, String> requestMap;

    // server always on
    boolean serverOn = true;
    while (serverOn) {
      // method to attempt connect, returns corresponding boolean
      connectedToServer = connectToInterServer();

      // repeat while connection is still active
      while (connectedToServer) {
        try {
          // handle request type
          requestMap = createProtocolMap(interStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);
          switch (requestMap.get(REQUEST_KEY)) {
            case UPLOAD_START_VAL:
              handleUpload(requestMap);
              break;
            case REQUEST_DOWNLOAD_VAL:
              handleDownload(requestMap);
              break;
            default:
              break;
          }
        } catch (IOException e) {
          System.out.println(StorageStrings.CONNECTION_BROKEN + ": " + e.getMessage());
          connectedToServer = false;
          sleep();
        }
      }
    }
    interStream.close();
  }

  // helper method to connect to server, returns corresponding boolean
  private static boolean connectToInterServer() {
    try {
      if (attemptCount == 0) {
        clear_console();
        System.out.print(StorageStrings.ATTEMPTING_RECONNECT);
      }
      interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);

      // on success, set relevant variables / output
      attemptCount = 0;
      System.out.println('\n' + StorageStrings.CONNECTED_TO_INTER);
      return true;

    } catch (IOException e) {
      sleep();
      ++attemptCount;

      // add to output based on how many times we've tried
      if (attemptCount == REPEAT_COUNT) {
        clear_console();
        attemptCount = 0;
      } else if (attemptCount != 0) {
        System.out.print('.');
      }
      return false;
    }
  }

  private static void handleUpload(HashMap<String, String> req) throws IOException {
    HashMap<String, String> res = new HashMap<>(); // map to send back
    File fileToStore = new File(STORAGE_PATH + req.get(FILENAME_KEY));

    // if file exists, send invalid file name status
    if (fileToStore.exists()) {
      res.put(STATUS_KEY, STATUS_INVALID_FILENAME_VAL);
      sendProtocolMessage(interStream, UPLOAD_START_ACK_VAL, res);
      System.out.println(StorageStrings.FILE_ALREADY_EXISTS);
    } else {
      // send ack back
      res.put(STATUS_KEY, STATUS_OK_VAL);
      sendProtocolMessage(interStream, UPLOAD_START_ACK_VAL, res);

      // get file size and create zip file destination
      long fileSize = Long.parseLong(req.get(FILE_SIZE_KEY));
      File zipFile = new File(fileToStore.getPath() + ZIP_SUFFIX);
      FileOutputStream outToZip = new FileOutputStream(zipFile);
      System.out.println(StorageStrings.UPLOAD_READY);

      // pipe socket into file
      interStream.readToOutputStream(outToZip, fileSize);
      outToZip.close();
      System.out.println(StorageStrings.UPLOAD_RECEIVED);

      // send upload success message
      sendProtocolMessage(interStream, UPLOAD_RECEIVED_VAL, res);

      // method to unzip file
      FileUtils.unzipFile(zipFile, STORAGE_PATH_FILE);
      zipFile.delete(); // delete zip (already decompressed)
    }
  }

  private static void handleDownload(HashMap<String, String> requestMap) throws IOException {
    
    // Map to store response, file to send back
    HashMap<String,String> responseMap = new HashMap<>();
    File fileToSend = new File(STORAGE_PATH + requestMap.get(FILENAME_KEY));

    // if file exists, send a zip back
    if (fileToSend.exists()) {
      System.out.println("Compressing file...");
      File zipFile = new File(fileToSend.getPath() + ZIP_SUFFIX);
      FileUtils.zipFile(fileToSend, zipFile);
      System.out.println("File compressed");

      // get file size and send ack back
      responseMap.put(FILE_SIZE_KEY, "" + zipFile.length());
      responseMap.put(STATUS_KEY, STATUS_OK_VAL);
      sendProtocolMessage(interStream, REQUEST_DOWNLOAD_ACK_VAL, responseMap);
      
      // read next request, if start_download, stream the file
      requestMap = createProtocolMap(interStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);
      if (requestMap.get(REQUEST_KEY).equals(START_DOWNLOAD_VAL)) {
        System.out.println("Sending file...");
        FileInputStream zipInStream = new FileInputStream(zipFile);
        interStream.writeFromInputStream(zipInStream, zipFile.length());
        zipInStream.close();
        System.out.println("File sent");
      }
      zipFile.delete();
    } else {
      // file does not exist
      responseMap.put(STATUS_KEY, STATUS_INVALID_FILENAME_VAL);
      sendProtocolMessage(interStream, REQUEST_DOWNLOAD_ACK_VAL, responseMap);
    }
  }

  // helper method to easily clear console
  private static void clear_console() {
    System.out.print("\033[H\033[2J");
  }

  // helper method to sleep execution
  private static void sleep() {
    try {
      Thread.sleep(SLEEP_TIME);
    } catch (InterruptedException ignored) {
      // don't care about exception
    }
  }
}