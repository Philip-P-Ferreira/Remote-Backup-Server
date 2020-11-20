import static commonutils.ServerProtocol.*;

import commonutils.TcpStream;
import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DesktopClient {

  public enum CLIENT_OPTIONS {
    UPLOAD,
    DOWNLOAD,
    REMOVE,
    LIST,
    STATS,
    EXIT
  }

  public static void main(String[] args) throws IOException {
    
    // main switch for option handling
    Scanner console = new Scanner(System.in);
    boolean session = true;
    while (session) {
      switch(getOption(console)) {
        case UPLOAD:
          handleUpload(console);
          break;
        case EXIT:
          session = false;
          break;
        default:
          break;
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
      } else if ((commandNum = input.nextInt()) < 1 || commandNum > (CLIENT_OPTIONS.values().length)) {
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
    ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

    // compress file
    System.out.println("\nCompressing file...");
    zipFile(fileToSend, fileToSend.getName(), zipOut);
    zipOut.close();
    System.out.println("File has been zipped\n");

    // create arg map to send
    HashMap<String, String> req = new HashMap<>();
    req.put(FILENAME_KEY, fileToSend.getName());
    req.put(FILE_SIZE_KEY, "" + fileToSend.length());

    // create tcp stream, signal server to start
    boolean goodConnect = true; // flag for good upload connection
    TcpStream interServerStream = null;
    HashMap<String,String> resMap = new HashMap<>();
    try {
      // attempt to connect to server
      interServerStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
      sendProtocolMessage(interServerStream, UPLOAD_START_VAL, req);
      resMap = createProtocolMap(interServerStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);
    } catch (IOException e) {
        System.out.println("Could not connect to intermediate server");
        zipFile.delete();

        goodConnect = false;
    }

    // check response status
    boolean goodResponse = false;
    
    if (goodConnect) {
      switch (resMap.get(STATUS_KEY)) {
        
        case STATUS_BAD_STORAGE_VAL:
          System.out.println("Could not connect to storage");
          break;


        case STATUS_INVALID_FILENAME_VAL:
          System.out.println("File already exists on storage");
          break;

        default:
          goodResponse = true;
      }
    }

    // write file to socket
    if (goodConnect && goodResponse) {
      System.out.println("Uploading file...");

      FileInputStream zipFileIn = new FileInputStream(zipFile);
      interServerStream.writeFromInputStream(zipFileIn, zipFile.length());

      zipFileIn.close();
      System.out.println("Upload complete");

    // clean up streams
    System.out.println("Status response: " + createProtocolMap(interServerStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR).get(STATUS_KEY));
    interServerStream.close();
    }

    zipFile.delete();
  }


  /**
   * zipFile - 
   * Recursively zip a file including a directory and all sub directories
   * 
   * @param fileToZip - File containing contents to compress
   * @param fileName - name of file (used in zip entry naming)
   * @param zipOut - Output stream to zip file
   * @throws IOException
   */
  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
    // skip if hidden
    if (!fileToZip.isHidden()) {
      if (fileToZip.isDirectory()) {
          // create new entry for a directory
        zipOut.putNextEntry((new ZipEntry(fileName + "/")));
        zipOut.closeEntry();

        // go through each file in directory
        File[] children = fileToZip.listFiles();
        for (File childFile : children) {
          zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
        }

      } else {
          // if file, just add to zip archive
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        // write to zip file out
        int length;
        byte[] bytes = new byte[1024];
        while ((length = fis.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        fis.close();
      }
    }
  }
}
