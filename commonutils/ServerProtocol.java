package commonutils;

import java.util.*;
import java.io.*;

public class ServerProtocol {

    // general key-value pair constants
    public static final String PAIR_DELIM = "&";
    public static final String PAIR_SEPARATOR = "=";
    public static final String REQUEST_KEY = "type";

    // request/response type values
    public static final String UPLOAD_START_VAL = "upload_start";
    public static final String UPLOAD_START_ACK_VAL = "upload_start_ack";
    public static final String UPLOAD_END_VAL = "upload_end";
    public static final String UPLOAD_END_ACK_VAL = "upload_end_ack";
    public static final String GET_INFO_VAL = "get_info";
    public static final String INFO_RESPONSE_VAL = "info";
    public static final String REQUEST_DOWNLOAD_VAL = "request_download";
    public static final String REQUEST_DOWNLOAD_ACK_VAL = "request_download_ack";
    public static final String START_DOWNLOAD = "start_download";
    public static final String END_DOWNLOAD = "end_download";

    // argument keys
    public static final String STATUS_KEY = "status";
    public static final String FILENAME_KEY = "filename";
    public static final String INFO_KEY = "info";

    // status values
    public static final String STATUS_OK_VAL = "ok";
    public static final String STATUS_BAD_STORAGE_VAL = "bad_storage";
    public static final String STATUS_INVALID_FILENAME_VAL = "invalid_filename";

    // info response constants
    public static final char INFO_PAIR_DELIM = ';';
    public static final char INFO_PAIR_SEPARATOR = '>';
    public static final String INFO_MAX_CAPACITY_KEY = "max_capacity";
    public static final String INFO_CAPACITY_USAGE = "capacity_usage";
    public static final String LAST_WRITE = "last_write";

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
  public static HashMap<String, String> createProtocolMap(
    String str, String delimiter, String separator) {
    String argArr[] = str.split(delimiter);
    HashMap<String, String> map = new HashMap<>();

    for (final String arg : argArr) {
        int sepIndex = arg.indexOf(separator);
        map.put(arg.substring(0, sepIndex), arg.substring(sepIndex + 1));
    }

    return map;
}

/**
   * sendProtocolMessage -
   * accepts a command type, a tcpStream, and a map. Creates a string of key value pairs
   * based on the map and sends via the tcp stream
   *
   * @param stream - tcpStream, communicates to a socket
   * @param type - commands type value
   * @param argMap - map of key value pairs to send
   * @throws IOException
   */
    public static void sendProtocolMessage(
      TcpStream stream, String type, HashMap<String, String> argMap) throws IOException {
        String msg = REQUEST_KEY + PAIR_SEPARATOR + type;

        for (final Map.Entry<String, String> pair : argMap.entrySet()) {
        msg += PAIR_DELIM + pair.getKey() + PAIR_SEPARATOR + pair.getValue();
        }
        stream.writeMessage(msg);
    }
}
