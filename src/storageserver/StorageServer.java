import static commonutils.ServerProtocol.*;

import commonutils.FileUtils;
import commonutils.TcpStream;
import java.io.*;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class StorageServer
{
    // file constant
    public static final File STORAGE_DIR = new File("iofiles/storage");

    // number constants
    private static final int SLEEP_TIME = 1000;
    private static final int REPEAT_COUNT = 4;

    // instance tcp stream
    private static TcpStream interStream;
    private static int attemptCount = 0; // keep track of connect attempts

    public static void main(String[] args) throws IOException
    {
        // vars to keep track of server connection status
        boolean connectedToServer = false;
        HashMap<String, String> requestMap;

        // server always on
        boolean serverOn = true;
        while (serverOn)
        {
            // method to attempt connect, returns corresponding boolean
            connectedToServer = connectToInterServer();

            // repeat while connection is still active
            while (connectedToServer)
            {
                try
                {
                    // handle request type
                    requestMap = createProtocolMap(interStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);

                    System.out.print('\n');
                    switch (requestMap.get(REQUEST_KEY))
                    {
                    case UPLOAD_START_VAL:
                        handleUpload(requestMap);
                        break;
                    case REQUEST_DOWNLOAD_VAL:
                        handleDownload(requestMap);
                        break;
                    case REMOVE_FILE_VAL:
                        handleRemove(requestMap);
                        break;
                    case LIST_FILES_VAL:
                        handleList();
                        break;
                    case GET_STATS_VAL:
                        handleStats();
                        break;
                    }
                }
                catch (IOException e)
                {
                    System.out.println(StorageStrings.CONNECTION_BROKEN + ": " + e.getMessage());
                    connectedToServer = false;
                    sleep();
                }
            }
        }
        interStream.close();
    }

    /**
     * connectToInterServer -
     * helper function to connect to the intermediate server repeatedly while
     * displaying a formatted status message
     *
     * @return - boolean, was able to connect or not
     */
    private static boolean connectToInterServer()
    {
        try
        {
            // format output based on attempts
            if (attemptCount == 0)
            {
                clear_console();
                System.out.print('\n' + StorageStrings.ATTEMPTING_RECONNECT);
            }
            interStream = new TcpStream(INTERSERVER_ADDRESS, STORAGE_PORT);

            // on success, set relevant variables / output
            attemptCount = 0;
            System.out.println('\n' + StorageStrings.CONNECTED_TO_INTER);
            return true;
        }
        catch (IOException e)
        {
            sleep();
            ++attemptCount;

            // add to output based on how many times we've tried
            if (attemptCount == REPEAT_COUNT)
            {
                attemptCount = 0;
            }
            else if (attemptCount != 0)
            {
                System.out.print('.');
            }
            return false;
        }
    }

    /**
     * handleUpload -
     * Performs all necessary actions to accept an upload from client
     *
     * @param req - Request map from client
     * @throws IOException
     */
    private static void handleUpload(HashMap<String, String> req) throws IOException
    {
        HashMap<String, String> res = new HashMap<>(); // map to send back
        File fileToStore = new File(STORAGE_DIR.getPath() + '/' + req.get(FILENAME_KEY));

        // if file exists, send invalid file name status
        if (fileToStore.exists())
        {
            res.put(STATUS_KEY, STATUS_INVALID_FILENAME_VAL);
            sendProtocolMessage(interStream, UPLOAD_START_ACK_VAL, res);
            System.out.println(StorageStrings.FILE_ALREADY_EXISTS);
        }
        else
        {
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
            FileUtils.unzipFile(zipFile, STORAGE_DIR);
            zipFile.delete(); // delete zip (already decompressed)
        }
    }

    /**
     * handleDownload -
     * Performs all actions to send file to client
     *
     * @param requestMap - request map from client
     * @throws IOException
     */
    private static void handleDownload(HashMap<String, String> requestMap) throws IOException
    {
        // Map to store response, file to send back
        HashMap<String, String> responseMap = new HashMap<>();
        File fileToSend = new File(STORAGE_DIR.getPath() + '/' + requestMap.get(FILENAME_KEY));

        // if file exists, send a zip back
        if (fileToSend.exists())
        {
            System.out.println(StorageStrings.ZIPPING_START_MSG);
            File zipFile = new File(fileToSend.getPath() + ZIP_SUFFIX);
            FileUtils.zipFile(fileToSend, zipFile);
            System.out.println(StorageStrings.ZIP_DONE_MSG);

            // get file size and send ack back
            responseMap.put(FILE_SIZE_KEY, "" + zipFile.length());
            responseMap.put(STATUS_KEY, STATUS_OK_VAL);
            sendProtocolMessage(interStream, REQUEST_DOWNLOAD_ACK_VAL, responseMap);

            // read next request, if start_download, stream the file
            requestMap = createProtocolMap(interStream.readMessage(), PAIR_DELIM, PAIR_SEPARATOR);
            if (requestMap.get(REQUEST_KEY).equals(START_DOWNLOAD_VAL))
            {
                System.out.println('\n' + StorageStrings.SENDING_START_MSG);
                FileInputStream zipInStream = new FileInputStream(zipFile);
                interStream.writeFromInputStream(zipInStream, zipFile.length());
                zipInStream.close();
                System.out.println(StorageStrings.SEND_DONE_MSG);
            }
            zipFile.delete();
        }
        else
        {
            // file does not exist
            responseMap.put(STATUS_KEY, STATUS_INVALID_FILENAME_VAL);
            sendProtocolMessage(interStream, REQUEST_DOWNLOAD_ACK_VAL, responseMap);
        }
    }

    /**
     * handleRemove -
     * Attempts to delete the file specified in the request map. If it does not exist, send back
     * invalid file name status, otherwise ok status
     *
     * @param req - Map, requestt
     * @throws IOException
     */
    private static void handleRemove(HashMap<String, String> req) throws IOException
    {
        // get file name
        System.out.println(StorageStrings.REMOVING_START_MSG);
        File fileToRm = new File(STORAGE_DIR.getPath() + '/' + req.get(FILENAME_KEY));

        // if exists, delete, otherwise invalid file
        HashMap<String, String> responseMap = new HashMap<>();
        if (fileToRm.exists())
        {
            deleteFile(fileToRm);
            System.out.printf(StorageStrings.REMOVE_DONE_FORMAT + '\n', fileToRm.getName());
            responseMap.put(STATUS_KEY, STATUS_OK_VAL);
        }
        else
        {
            System.out.printf(StorageStrings.FILE_NOT_EXIST_FORMAT + '\n', fileToRm.getName());
            responseMap.put(STATUS_KEY, STATUS_INVALID_FILENAME_VAL);
        }
        sendProtocolMessage(interStream, REMOVE_FILE_ACK_VAL, responseMap);
    }

    /**
     * handleList -
     * Sends a list of files (if any) back to intermediate server
     *
     * @throws IOException
     */
    private static void handleList() throws IOException
    {
        System.out.println(StorageStrings.LIST_FILES_MSG);

        // list files and compile into one string, separated by a comma
        String[] files = STORAGE_DIR.list();
        String fileResArg = "";
        for (final String fileStr : files)
        {
            fileResArg += (fileStr + FILE_NAME_DELIM);
        }

        // send back
        HashMap<String, String> resMap = new HashMap<>();
        resMap.put(STATUS_KEY, STATUS_OK_VAL);
        resMap.put(FILE_LIST_KEY, fileResArg);
        sendProtocolMessage(interStream, LIST_RESPONSE_VAL, resMap);
    }

    private static void handleStats() throws IOException
    {

        System.out.println(StorageStrings.GET_STATS_MSG);

        // map to hold each stat
        HashMap<String, String> statsMap = new HashMap<>();
        statsMap.put(STATS_FREE_SPACE_KEY, Long.toString(STORAGE_DIR.getUsableSpace()));
        statsMap.put(STATS_MAX_CAPACITY_KEY, Long.toString(STORAGE_DIR.getTotalSpace()));
        statsMap.put(STATS_LAST_WRITE_KEY, Long.toString(STORAGE_DIR.lastModified()));

        // custom functor to create stats string
        class StatsBuilder implements BiConsumer<String, String>
        {
            String statsStr = "";

            @Override public void accept(String key, String val)
            {
                statsStr += key + STATS_PAIR_SEPARATOR + val + STATS_PAIR_DELIM;
            }
        };

        // build the stats string
        StatsBuilder statsBuilder = new StatsBuilder();
        statsMap.forEach(statsBuilder);

        // send stats back
        HashMap<String, String> resMap = new HashMap<>();
        resMap.put(STATS_KEY, statsBuilder.statsStr);
        resMap.put(STATUS_KEY, STATUS_OK_VAL);
        sendProtocolMessage(interStream, STATS_RESPONSE_VAL, resMap);
    }

    /**
     * deleteFile -
     * Recursively deletes a file and any potential sub files if a directory
     *
     * @param file - file to delete, can be file or directory
     */
    public static void deleteFile(File file)
    {

        if (file.isDirectory())
        {
            for (final File subFile : file.listFiles())
            {
                deleteFile(subFile);
            }
        }
        file.delete();
    }

    // helper method to easily clear console
    private static void clear_console()
    {
        System.out.print("\033[H\033[2J");
    }

    // helper method to sleep execution
    private static void sleep()
    {
        try
        {
            Thread.sleep(SLEEP_TIME);
        }
        catch (InterruptedException ignored)
        {
            // don't care about exception
        }
    }
}