package commonutils;

public class ServerProtocol {

    // general key-value pair constants
    public static final char PAIR_DELIM = '&';
    public static final char PAIR_SEPARATOR = '=';
    public static final String REQUEST_KEY = "type";

    // request/response type values
    public static final String UPLOAD_START_VAL = "upload_start";
    public static final String UPLOAD_START_ACK_VAL = "upload_start_ack";
    public static final String UPLOAD_END_VAL = "upload_end";
    public static final String UPLOAD_END_ACK_VAL = "upload_end_ack";
    public static final String GET_INFO_VAL = "get_info";
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
    public static final String STATUS_FAILED_VAL = "failed";

    // info response constants
    public static final char INFO_PAIR_DELIM = ';';
    public static final char INFO_PAIR_SEPARATOR = '>';
    public static final String INFO_MAX_CAPACITY_KEY = "max_capacity";
    public static final String INFO_CAPACITY_USAGE = "capacity_usage";
    public static final String LAST_WRITE = "last_write";

    

    // networks constants
    public static final int PORT = 6789;
    public static final String INTERSERVER_ADDRESS = "ec2-18-219-79-157.us-east-2.compute.amazonaws.com";


    
}
