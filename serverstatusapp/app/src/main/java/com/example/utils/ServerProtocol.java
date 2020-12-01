package com.example.utils;

import java.io.*;
import java.util.*;

public class ServerProtocol
{
    // general key-value pair constants
    public static final String PAIR_DELIM = "&";
    public static final String PAIR_SEPARATOR = "=";
    public static final String REQUEST_KEY = "type";

    // request/response type values
    public static final String ACK_SUFFIX = "_ack";
    public static final String UPLOAD_START_VAL = "upload_start";
    public static final String UPLOAD_START_ACK_VAL = UPLOAD_START_VAL + ACK_SUFFIX;
    public static final String UPLOAD_RECEIVED_VAL = "upload_received";
    public static final String GET_STATS_VAL = "get_stats";
    public static final String STATS_RESPONSE_VAL = "stats";
    public static final String REQUEST_DOWNLOAD_VAL = "request_download";
    public static final String REQUEST_DOWNLOAD_ACK_VAL = REQUEST_DOWNLOAD_VAL + ACK_SUFFIX;
    public static final String START_DOWNLOAD_VAL = "start_download";
    public static final String REMOVE_FILE_VAL = "remove_file";
    public static final String REMOVE_FILE_ACK_VAL = REMOVE_FILE_VAL + ACK_SUFFIX;
    public static final String LIST_FILES_VAL = "list_files";
    public static final String LIST_RESPONSE_VAL = "list";

    // argument keys
    public static final String STATUS_KEY = "status";
    public static final String FILENAME_KEY = "filename";
    public static final String FILE_SIZE_KEY = "file_size";
    public static final String STATS_KEY = "stats";
    public static final String FILE_LIST_KEY = "files";

    // status values
    public static final String STATUS_OK_VAL = "ok";
    public static final String STATUS_BAD_STORAGE_VAL = "bad_storage";
    public static final String STATUS_INVALID_FILENAME_VAL = "invalid_filename";

    // STATS response constants
    public static final String STATS_PAIR_DELIM = ";";
    public static final String STATS_PAIR_SEPARATOR = ">";
    public static final String STATS_MAX_CAPACITY_KEY = "max_capacity";
    public static final String STATS_FREE_SPACE_KEY = "capacity_usage";
    public static final String STATS_LAST_WRITE_KEY = "last_write";

    // list response constants
    public static final String FILE_NAME_DELIM = ",";

    // networks constants
    public static final int STORAGE_PORT = 6788;
    public static final int CLIENT_PORT = 6789;
    // public static final String INTERSERVER_ADDRESS = "127.0.0.1";
    public static final String INTERSERVER_ADDRESS = "18.219.79.157";

    public static final String ZIP_SUFFIX = ".zip";

    /**
     * createProtocolMap -
     * create a hashmap of String-String pairs, where pairs are delimited by
     * delimiter, and separated by separator.
     *
     * @param str - String to turn into hashmap
     * @param delimiter - splits String-String pairs
     * @param separator - breaks pair into key and value
     * @return
     */
    public static HashMap<String, String> createProtocolMap(String str, String delimiter, String separator)
    {
        String argArr[] = str.split(delimiter);
        HashMap<String, String> map = new HashMap<>();

        for (final String arg : argArr)
        {
            int sepIndex = arg.indexOf(separator);
            map.put(arg.substring(0, sepIndex), arg.substring(sepIndex + 1));
        }

        return map;
    }

    /**
     * createProtocolMap -
     * Overloaded function for the common case of a request / response map
     *
     * @param str - String to turn into hashmap
     * @return
     */
    public static HashMap<String, String> createProtocolMap(String str)
    {
        return createProtocolMap(str, PAIR_DELIM, PAIR_SEPARATOR);
    }

    /**
     * sendProtocolMessage -
     * accepts a command type, a tcpStream, and a map. Creates a string of key
     * value pairs based on the map and sends via the tcp stream
     *
     * @param stream - tcpStream, communicates to a socket
     * @param type - commands type value
     * @param argMap - map of key value pairs to send
     * @throws IOException
     */
    public static void sendProtocolMessage(TcpStream stream, String type, HashMap<String, String> argMap)
        throws IOException
    {
        String msg = REQUEST_KEY + PAIR_SEPARATOR + type;

        for (final Map.Entry<String, String> pair : argMap.entrySet())
        {
            msg += PAIR_DELIM + pair.getKey() + PAIR_SEPARATOR + pair.getValue();
        }
        stream.writeMessage(msg);
    }

    /**
     * sendProtocolMessage -
     * Overloaded function, send message without any arguments
     *
     * @param stream - TcpStream, communicates to a socket
     * @param type - command type value
     * @throws IOException
     */
    public static void sendProtocolMessage(TcpStream stream, String type) throws IOException
    {
        sendProtocolMessage(stream, type, new HashMap<String, String>());
    }
}
