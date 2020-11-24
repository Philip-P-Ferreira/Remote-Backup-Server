import static commonutils.ServerProtocol.*;

import commonutils.*;
import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class DesktopClient {
  public enum CLIENT_OPTIONS { UPLOAD, DOWNLOAD, REMOVE, LIST, STATS, EXIT }

  public static void main(String[] args) throws IOException {
    // main switch for option handling
    Scanner console = new Scanner(System.in);
    boolean session = true;

    while (session) {
      try {
        switch (getOption(console)) {
          case UPLOAD:
            handleUpload(console);
            break;

          case DOWNLOAD:
            handleDownload(console);
            break;

          case EXIT:
            session = false;
            break;

          default:
            break;
        }
      } catch (IOException e) {
        System.out.println("Could not connect to intermediate server: " + e.getMessage());
      }
    }
  }

  /**
   * getOption -
   * Prompts the user to input a number corresponding to an action.
   * Returns that options. Performs input checking
   *
   * @param input - Scanner to read input
   * @return - CLIENT_OPTIONS, enum represnting all options
   */
  private static CLIENT_OPTIONS getOption(Scanner input) {
    // keep track of input validity
    int commandNum = -1;
    boolean validInput = false;

    // loop as long as input isn't valid
    while (!validInput) {
      // prompt user
      System.out.println("\n1. Upload\n2. Download\n3. Remove\n4. List\n5. Stats\n6. Exit");
      System.out.println("Select a command (input corresponding number)");

      // check validity of input
      if (!input.hasNextInt()) {
        System.out.println("\nPlease input a number");
        input.nextLine();
      } else if ((commandNum = input.nextInt()) < 1
          || commandNum > (CLIENT_OPTIONS.values().length)) {
        System.out.println("\nInvalid number");
      } else {
        validInput = true;
      }
    }
    input.nextLine(); // burn new line
    return CLIENT_OPTIONS.values()[commandNum - 1];
  }

  /**
   * handleUpload -
   * Accepts user input for a filepath, compresses that file, and streams
   * it to the server
   *
   * @param input - Scanner to read input
   * @throws IOException
   */
  private static void handleUpload(Scanner input) throws IOException {
    // prompt user for file, make file
    File fileToSend = null;
    boolean validFile = false;

    // loop until valid file from user
    while (!validFile) {
      System.out.print("\nEnter the file path to upload: ");
      String lineInput = input.nextLine();

      // exit if just enter space
      if (lineInput.isEmpty()) {
        return;
      }

      // create file and check if exists
      fileToSend = new File(lineInput);
      if (fileToSend.exists()) {
        validFile = true;
      } else {
        System.out.println("File does not exist, please try again");
      }
    }

    // create zip file and corresponding stream
    File zipFile = new File(fileToSend.getPath() + ZIP_SUFFIX);

    // compress file
    System.out.println("\nCompressing file...");
    FileUtils.zipFile(fileToSend, zipFile);
    System.out.println("File has been zipped\n");

    // create arg map to send
    HashMap<String, String> req = new HashMap<>();
    req.put(FILENAME_KEY, fileToSend.getName());
    req.put(FILE_SIZE_KEY, "" + fileToSend.length());

    // create tcp stream, signal server to start
    TcpStream interServerStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
    HashMap<String, String> resMap = getResponse(interServerStream, req, UPLOAD_START_VAL);

    // handle status types
    switch (resMap.get(STATUS_KEY)) {
      case STATUS_BAD_STORAGE_VAL:
        System.out.println("Could not connect to storage");
        break;

      case STATUS_INVALID_FILENAME_VAL:
        System.out.println("File name \"" + fileToSend.getName() + "\" already exists on storage");
        break;

      case STATUS_OK_VAL:
        System.out.println("Uploading file...");

        FileInputStream zipFileIn = new FileInputStream(zipFile);
        interServerStream.writeFromInputStream(zipFileIn, zipFile.length());

        zipFileIn.close();
        System.out.println("Upload complete");

        // clean up streams
        System.out.println("Status response: "
            + createProtocolMap(interServerStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR)
                  .get(STATUS_KEY));
        interServerStream.close();
        break;
    }

    zipFile.delete();
  }

  private static void handleDownload(Scanner input) throws IOException {
    // get file name from user
    System.out.print("Enter file name to download: ");
    String filename = input.nextLine();

    // put file name as arg
    HashMap<String, String> reqMap = new HashMap<>();
    reqMap.put(FILENAME_KEY, filename);

    // send req to server
    TcpStream interStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
    HashMap<String, String> resMap = getResponse(interStream, reqMap, REQUEST_DOWNLOAD_VAL);

    // read response and act accordingly
    switch (resMap.get(STATUS_KEY)) {
      case STATUS_BAD_STORAGE_VAL:
        System.out.println("Could not connect to storage");
        break;

      case STATUS_INVALID_FILENAME_VAL:
        System.out.println("File does not exist on storage");
        break;

      case STATUS_OK_VAL:

        // create files to hold download / unzip
        File destFile = new File(filename);
        File zipFile = new File(filename + ZIP_SUFFIX);

        // signal server to start download
        reqMap.clear();
        FileOutputStream zipOut = new FileOutputStream(zipFile);
        sendProtocolMessage(interStream, START_DOWNLOAD_VAL, reqMap);

        // download contents
        System.out.println("\nDownloading file...");
        interStream.readToOutputStream(zipOut, Long.parseLong(resMap.get(FILE_SIZE_KEY)));
        zipOut.close();
        System.out.println("File downloaded");

        // unzip
        System.out.println("\nDecompressing file...");
        FileUtils.unzipFile(zipFile, destFile);
        System.out.println("File decompressed");

        zipFile.delete();
    }
  }

  private static HashMap<String, String> getResponse(
      TcpStream stream, HashMap<String, String> argMap, String requestType) throws IOException {
    // attempt to connect to server
    HashMap<String, String> resMap = new HashMap<>();
    sendProtocolMessage(stream, requestType, argMap);
    resMap = createProtocolMap(stream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);

    return resMap;
  }
}
