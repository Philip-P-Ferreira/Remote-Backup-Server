package com.example.utils;
import static com.example.utils.ServerProtocol.*;

import java.io.IOException;
import java.util.HashMap;

public class NetworkThread implements Runnable
{

    private String response;
    private HashMap<String, String> requestMap;
    private String requestType;

    public NetworkThread(String reqType, HashMap<String, String> reqMap)
    {
        response = "";
        requestMap = reqMap;
        requestType = reqType;
    }

    public NetworkThread(String reqType)
    {
        response = "";
        requestMap = new HashMap<>();
        requestType = reqType;
    }

    public void run()
    {
        try
        {
            TcpStream interStream = new TcpStream(INTERSERVER_ADDRESS, CLIENT_PORT);
            sendProtocolMessage(interStream, requestType, requestMap);
            response = interStream.readMessage();
        }
        catch (IOException ignored)
        {
        }
    }

    public String getResponse()
    {
        return response;
    }
}
