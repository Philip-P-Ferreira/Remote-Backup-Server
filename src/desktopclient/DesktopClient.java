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
    switch(getOption(console)) {
      case UPLOAD:
        handleUpload(console);
        break;
      default:
        break;
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
        System.out.println("Please input a number");
        input.nextLine();
      } else if ((commandNum = input.nextInt()) < 0 || commandNum > (CLIENT_OPTIONS.values().length) + 1) {
        System.out.println("Invalid number");
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
    System.out.print("\nEnter the file path to upload: ");
    File fileToSend = new File(input.nextLine());

    // create zip file and corresponding stream
    File zipFile = new File(fileToSend.getPath() + ZIP_SUFFIX);
    ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));

    // compress file
    zipFile(fileToSend, fileToSend.getName(), zipOut);
    zipOut.close();
    System.out.println("File has been zipped");

    // create arg map to send
    HashMap<String, String> req = new HashMap<>();
    req.put(FILENAME_KEY, fileToSend.getName());
    req.put(FILE_SIZE_KEY, "" + fileToSend.length());

    // create tcp stream, signal server to start
    TcpStream interServerStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
    sendProtocolMessage(interServerStream, UPLOAD_START_VAL, req);
    System.out.println(interServerStream.readMessage());

    // write file to socket
    FileInputStream zipFileIn = new FileInputStream(zipFile);
    interServerStream.writeFromInputStream(zipFileIn, zipFile.length());
    zipFileIn.close();

    // clean up streams
    System.out.println(interServerStream.readMessage());
    interServerStream.close();
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
