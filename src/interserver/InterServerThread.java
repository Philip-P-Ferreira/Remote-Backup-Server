import static commonutils.ServerProtocol.*;

import commonutils.TcpStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class InterServerThread implements Runnable
{
    private TcpStream clientStream;

    // Constructor
    public InterServerThread(ServerSocket servSocket) throws IOException
    {
        clientStream = new TcpStream(servSocket);
    }

    // Server thread "main method"
    public void run()
    {
        // try to read from client
        try
        {
            // get request from client
            final String clientRequest = clientStream.readMessage();
            final HashMap<String, String> requestMap = createProtocolMap(clientRequest, PAIR_DELIM, PAIR_SEPARATOR);

            // try to get response from storage
            final String storageResponseStr = InterServer.duplexFowardMsg(clientRequest);

            // if bad storage (empty string), send back bad status
            if (storageResponseStr.isEmpty())
            {
                final HashMap<String, String> badStorageStat = new HashMap<>();
                badStorageStat.put(STATUS_KEY, STATUS_BAD_STORAGE_VAL);

                sendProtocolMessage(clientStream, requestMap.get(REQUEST_KEY) + ACK_SUFFIX, badStorageStat);
            }
            else
            {
                // create response map
                final HashMap<String, String> storageResponseMap =
                    createProtocolMap(storageResponseStr, PAIR_DELIM, PAIR_SEPARATOR);

                // handle based on type
                clientStream.writeMessage(storageResponseStr); // always send response to client
                switch (requestMap.get(REQUEST_KEY))
                {
                case UPLOAD_START_VAL:
                    handleUpload(requestMap, storageResponseMap);
                    break;

                case REQUEST_DOWNLOAD_VAL:
                    handleDownload(storageResponseMap);
                    break;

                default:
                    // all other request, simply forward messages
                    break;
                }
            }

            clientStream.close();
        }
        catch (IOException e)
        {
            System.out.printf(InterStorageStrings.NO_REACH_CLIENT_FORMAT + '\n', e.getMessage());
        }
    }

    /**
     * handleUpload -
     * Using a request map and the storage response, intermediates upload from
     * client to storage
     *
     * @param request - Map client request
     * @param response - Map, storage response
     * @throws IOException
     */
    private void handleUpload(HashMap<String, String> request, HashMap<String, String> response) throws IOException
    {
        System.out.println('\n' + InterStorageStrings.UPLOAD_REQUESTED_MSG);
        // if good status from storage, stream upload
        final String status = response.get(STATUS_KEY);
        if (status != null && status.equals(STATUS_OK_VAL))
        {
            System.out.println(InterStorageStrings.STARTING_UPLOAD_MSG);
            long fileSize = Long.parseLong(request.get(FILE_SIZE_KEY));
            InterServer.streamToStorage(clientStream, fileSize);
            System.out.println(InterStorageStrings.UPLOAD_DONE_MSG);

            clientStream.writeMessage(InterServer.getMessageFromStorage());
            clientStream.close();
        }
        else
        {
            System.out.println(InterStorageStrings.FILE_EXISTS_ERROR_MSG);
        }
    }

    /**
     * handleDownload -
     * Using response map, intermediates download from storage to client
     *
     * @param response - Map, response from storage
     * @throws IOException
     */
    private void handleDownload(HashMap<String, String> response) throws IOException
    {
        System.out.println('\n' + InterStorageStrings.DOWNLOAD_REQUESTED_MSG);
        // if good storage response, stream download
        String status = response.get(STATUS_KEY);
        if (status != null && status.equals(STATUS_OK_VAL))
        {
            InterServer.forwardToStorage(clientStream.readMessage());

            System.out.println(InterStorageStrings.STARTING_DOWLOAD_MSG);
            InterServer.streamToClient(clientStream, Long.parseLong(response.get(FILE_SIZE_KEY)));
            System.out.println(InterStorageStrings.DOWNLOAD_DONE_MSG);
        }
        else
        {
            System.out.println(InterStorageStrings.INVALID_FILE_ERROR_MSG);
        }
    }
}
