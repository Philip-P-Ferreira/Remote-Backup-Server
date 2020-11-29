import static commonutils.ServerProtocol.*;

import commonutils.*;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.BiConsumer;

public class DesktopClient
{
    public enum CLIENT_OPTIONS { UPLOAD, DOWNLOAD, REMOVE, LIST, STATS, EXIT }
    public static final File CLIENT_DIR = new File("iofiles/client");
    public static final String[] BYTE_SUFFIX_ARR = {"B", "kB", "MB", "GB", "TB"};
    public static final int BYTE_CONVERSION_NUM = 1000;

    public static void main(String[] args) throws IOException
    {
        // main switch for option handling
        final Scanner console = new Scanner(System.in);
        boolean session = true;

        while (session)
        {
            try
            {
                switch (getOption(console))
                {
                case UPLOAD:
                    handleUpload(console);
                    break;

                case DOWNLOAD:
                    handleDownload(console);
                    break;

                case REMOVE:
                    handleRemove(console);
                    break;

                case LIST:
                    handleList();
                    break;

                case STATS:
                    handleStats();
                    break;

                case EXIT:
                    session = false;
                    break;
                }
            }
            catch (IOException e)
            {
                System.out.printf(DesktopClientStrings.BAD_INTER_FORMAT + '\n', e.getMessage());
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
    private static CLIENT_OPTIONS getOption(Scanner input)
    {
        // keep track of input validity
        int commandNum = -1;
        boolean validInput = false;

        // loop as long as input isn't valid
        while (!validInput)
        {
            // prompt user
            System.out.println('\n' + DesktopClientStrings.OPTIONS_PROMPT);

            // check validity of input
            if (!input.hasNextInt())
            {
                System.out.println('\n' + DesktopClientStrings.INPUT_NUMBER_PROMPT);
                input.nextLine();
            }
            else if ((commandNum = input.nextInt()) < 1 || commandNum > (CLIENT_OPTIONS.values().length))
            {
                System.out.println('\n' + DesktopClientStrings.INVALID_NUMBER_PROMPT);
            }
            else
            {
                validInput = true;
            }
        }
        input.nextLine(); // burn new line
        System.out.print('\n');
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
    private static void handleUpload(Scanner input) throws IOException
    {
        // prompt user for file, make file
        File fileToSend = null;
        boolean validFile = false;

        // loop until valid file from user
        while (!validFile)
        {
            System.out.print(DesktopClientStrings.FILE_PATH_PROMPT);
            String lineInput = input.nextLine();

            // exit if just enter space
            if (lineInput.isEmpty())
            {
                return;
            }

            // create file and check if exists
            fileToSend = new File(lineInput.trim());
            if (fileToSend.exists())
            {
                validFile = true;
            }
            else
            {
                System.out.println(DesktopClientStrings.FILE_NOT_EXIST_MSG + '\n');
            }
        }

        // create zip file and corresponding stream
        File zipFile = new File(CLIENT_DIR.getPath() + File.separatorChar + fileToSend.getName() + ZIP_SUFFIX);

        // compress file
        System.out.println('\n' + DesktopClientStrings.COMPRESSING_MSG);
        FileUtils.zipFile(fileToSend, zipFile);
        System.out.println(DesktopClientStrings.COMPRESS_DONE_MSG + '\n');

        // create arg map to send
        HashMap<String, String> req = new HashMap<>();
        req.put(FILENAME_KEY, fileToSend.getName());
        req.put(FILE_SIZE_KEY, Long.toString(zipFile.length()));

        // create tcp stream, signal server to start
        TcpStream interServerStream = makeInterStream();
        HashMap<String, String> resMap = requestAndResponse(interServerStream, UPLOAD_START_VAL, req);

        // handle status types
        switch (resMap.get(STATUS_KEY))
        {
        case STATUS_BAD_STORAGE_VAL:
            System.out.println(DesktopClientStrings.BAD_STORAGE_MSG);
            break;

        case STATUS_INVALID_FILENAME_VAL:
            System.out.printf(DesktopClientStrings.INVALID_FILE_UPLOAD_FORMAT + '\n', fileToSend.getName());
            break;

        case STATUS_OK_VAL:
            System.out.println(DesktopClientStrings.UPLOADING_MSG);

            FileInputStream zipFileIn = new FileInputStream(zipFile);
            interServerStream.writeFromInputStream(zipFileIn, zipFile.length());

            zipFileIn.close();
            System.out.println(DesktopClientStrings.UPLOADING_DONE_MSG);

            // clean up streams
            System.out.printf(DesktopClientStrings.STATUS_RESPONSE_FORMAT + '\n',
                              createProtocolMap(interServerStream.readMessage()).get(STATUS_KEY));
            interServerStream.close();
            break;
        }

        zipFile.delete();
    }

    /**
     * handleDownload -
     * Using the passed in scanner, get a file name from user to download from storage.
     * Then attempt to download file. Handle if file exists or does not
     *
     * @param input - Scanner, user input
     * @throws IOException
     */
    private static void handleDownload(Scanner input) throws IOException
    {
        // get file name from user
        System.out.print(DesktopClientStrings.FILE_NAME_PROMPT);
        String filename = input.nextLine();

        // if blankline, exit
        if (filename.isEmpty())
        {
            return;
        }

        // put file name as arg
        HashMap<String, String> reqMap = new HashMap<>();
        reqMap.put(FILENAME_KEY, filename);

        // send req to server
        System.out.println(DesktopClientStrings.DOWNLOAD_REQUEST_MSG);
        TcpStream interStream = makeInterStream();
        HashMap<String, String> resMap = requestAndResponse(interStream, REQUEST_DOWNLOAD_VAL, reqMap);

        // read response and act accordingly
        switch (resMap.get(STATUS_KEY))
        {
        case STATUS_BAD_STORAGE_VAL:
            System.out.println(DesktopClientStrings.BAD_STORAGE_MSG);
            break;

        case STATUS_INVALID_FILENAME_VAL:
            System.out.println(DesktopClientStrings.FILE_NOT_ON_STORAGE_MSG);
            break;

        case STATUS_OK_VAL:

            // create files to hold download / unzip
            File zipFile = new File(CLIENT_DIR.getPath() + File.separatorChar + filename + ZIP_SUFFIX);

            // signal server to start download
            reqMap.clear();
            FileOutputStream zipOut = new FileOutputStream(zipFile);
            sendProtocolMessage(interStream, START_DOWNLOAD_VAL, reqMap);

            // download contents
            System.out.println('\n' + DesktopClientStrings.DOWNLOADING_MSG);
            interStream.readToOutputStream(zipOut, Long.parseLong(resMap.get(FILE_SIZE_KEY)));
            zipOut.close();
            System.out.println(DesktopClientStrings.DOWNLOAD_DONE_MSG);

            // unzip
            System.out.println('\n' + DesktopClientStrings.UNZIPPING_MSG);
            FileUtils.unzipFile(zipFile, CLIENT_DIR);
            System.out.println(DesktopClientStrings.UNZIP_DONE_MSG);

            zipFile.delete();
        }
    }

    /**
     * handleRemove -
     * Using scanner, get a file name to delete from storage.
     * Handles if file exists or not
     *
     * @param input - Scanner, user input
     * @throws IOException
     */
    private static void handleRemove(Scanner input) throws IOException
    {
        System.out.print(DesktopClientStrings.FILE_NAME_RM_PROMPT);
        String filename = input.nextLine();

        HashMap<String, String> requestMap = new HashMap<>();
        requestMap.put(FILENAME_KEY, filename);

        HashMap<String, String> responseMap = requestAndResponse(makeInterStream(), REMOVE_FILE_VAL, requestMap);

        switch (responseMap.get(STATUS_KEY))
        {
        case STATUS_BAD_STORAGE_VAL:
            System.out.println(DesktopClientStrings.BAD_STORAGE_MSG);
            break;
        case STATUS_INVALID_FILENAME_VAL:
            System.out.println(DesktopClientStrings.FILE_NOT_ON_STORAGE_MSG);
            break;
        case STATUS_OK_VAL:
            System.out.println(DesktopClientStrings.REMOVE_FILE_SUCCESS_MSG);
            break;
        }
    }

    /**
     * handleList -
     * Gets list of file from storage and displays them (or appropriate message if none or error)
     *
     * @throws IOException
     */
    private static void handleList() throws IOException
    {
        System.out.println(DesktopClientStrings.LISTING_START_MSG);

        final HashMap<String, String> responseMap = requestAndResponse(makeInterStream(), LIST_FILES_VAL);
        switch (responseMap.get(STATUS_KEY))
        {
        case STATUS_BAD_STORAGE_VAL:
            System.out.println(DesktopClientStrings.BAD_STORAGE_MSG);
            break;

        case STATUS_OK_VAL:
            final String[] fileList = responseMap.get(FILE_LIST_KEY).split(FILE_NAME_DELIM);

            if (fileList[0].isEmpty())
            {
                System.out.println(DesktopClientStrings.NO_FILES_TO_LIST_MSG);
            }
            else
            {
                for (final String filename : responseMap.get(FILE_LIST_KEY).split(FILE_NAME_DELIM))
                {
                    System.out.println(filename);
                }
            }
        }
    }

    /**
     * handleStats -
     * Sends storage stats back to the client
     * 
     * @throws IOException
     */
    private static void handleStats() throws IOException
    {

        System.out.println(DesktopClientStrings.STATS_START_MSG);

        // handle based on status response
        final HashMap<String, String> responseMap = requestAndResponse(makeInterStream(), GET_STATS_VAL);
        switch (responseMap.get(STATUS_KEY))
        {

        case STATUS_BAD_STORAGE_VAL:
            System.out.println(DesktopClientStrings.BAD_STORAGE_MSG);
            break;

        case STATUS_OK_VAL:
            HashMap<String, String> statsMap =
                createProtocolMap(responseMap.get(STATS_RESPONSE_VAL), STATS_PAIR_DELIM, STATS_PAIR_SEPARATOR);

            // extract each stats and print
            statsMap.forEach(new BiConsumer<String, String>() {
                @Override public void accept(String key, String val)
                {

                    String output;
                    switch (key)
                    {
                    case STATS_MAX_CAPACITY_KEY:
                        output = formatByteSize(Long.parseLong(val));
                        System.out.printf(DesktopClientStrings.STATS_MAX_FORMAT + '\n', output);
                        break;

                    case STATS_FREE_SPACE_KEY:
                        output = formatByteSize(Long.parseLong(val));
                        System.out.printf(DesktopClientStrings.STATS_FREE_FORMAT + '\n', output);
                        break;

                    case STATS_LAST_WRITE_KEY:
                        output = new Date(Long.parseLong(val)).toString();
                        System.out.printf(DesktopClientStrings.STATS_LAST_WRITE_FOMAT + '\n', output);
                        break;
                    }
                }
            });
        }
    }

    /**
     * requestAndResponse -
     * Sends a request of type requestType with argMap to the given TcpStream.
     * Then reads a respone from the TcpStream.
     *
     * @param stream - TcpStream, connection to intermediate server
     * @param argMap - Map, contains args to send in request
     * @param requestType - String, type of request
     * @return - Map, response from server
     * @throws IOException
     */
    private static HashMap<String, String> requestAndResponse(TcpStream stream, String requestType,
                                                              HashMap<String, String> argMap) throws IOException
    {
        // attempt to connect to server
        HashMap<String, String> resMap = new HashMap<>();
        sendProtocolMessage(stream, requestType, argMap);
        resMap = createProtocolMap(stream.readMessage());

        return resMap;
    }

    /**
     * requestAndResponse -
     * Overloaded function, sends request without any args
     *
     * @param stream - TcpStream, connection to intermediate server
     * @param requestType - Stringm, type of request
     * @return - Map, response from server
     * @throws IOException
     */
    private static HashMap<String, String> requestAndResponse(TcpStream stream, String requestType) throws IOException
    {
        return requestAndResponse(stream, requestType, new HashMap<>());
    }

    /**
     * makeInterStream -
     * Returns a TcpStream to the intermediate server
     *
     * @return - TcpSream, to interstream
     * @throws IOException
     */
    private static TcpStream makeInterStream() throws IOException
    {
        return new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
    }

    private static String formatByteSize(long byteSize) {

        int suffixCount = 0; 
        while (byteSize > BYTE_CONVERSION_NUM) {
            ++suffixCount;
            byteSize /= BYTE_CONVERSION_NUM;
        }

        return Long.toString(byteSize) + ' ' + BYTE_SUFFIX_ARR[suffixCount];
    }
}
