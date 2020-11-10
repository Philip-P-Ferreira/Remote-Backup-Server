package commonutils;

import java.io.*;
import java.net.*;

public class TcpStream {
  private Socket socket;
  private InputStream inStream;
  private OutputStream outStream;

  private static int BUFFER_SIZE = 2048;

  // Constructor
  public TcpStream(String serverAddress, int port) throws IOException {
    
    socket = new Socket(serverAddress, port);
    inStream = socket.getInputStream();
    outStream = socket.getOutputStream();
  }

  // Alt constructor (from a Socket)
  public TcpStream(Socket socket) throws IOException {
    
    this.socket = socket;
    inStream = socket.getInputStream();
    outStream = socket.getOutputStream();
  }

  /**
   * writeMessage -
   * Writes a string plus a newline to the socket output
   * 
   * @param str - message to write
   * @throws IOException
   */
  public void writeMessage(String str) throws IOException {
    
    DataOutputStream dataOut = new DataOutputStream(outStream);
    dataOut.writeBytes(str + '\n');
  }

  /**
   * writeFromInputStream -
   * Writes to socket output the contents of the input. Will close the socket
   * because outStream must close to signal end of file
   * 
   * @param inStream
   * @throws IOException
   */
  public void writeFromInputStream(InputStream inStream) throws IOException {
    copyStream(inStream, outStream);
  }

  /**
   * readMessage -
   * Reads a message to the next newline from the socket
   * 
   * @return - String, message from socket
   * @throws IOException
   */
  public String readMessage() throws IOException {

    BufferedReader buffInReader = new BufferedReader(new InputStreamReader(inStream));

    String readLine = buffInReader.readLine();
    if (readLine == null) {
      throw new IOException();
    }
    return readLine;
  }

  /**
   * readToOutputStream - 
   * Write the contents of socket in to outStream. Will close outstream to signal end of file for next input.
   * 
   * @param outStream -- OutputStream to write to
   * @throws IOException
   */
  public void readToOutputStream(OutputStream outStream) throws IOException {
    
    copyStream(inStream, outStream);
  }

  /**
   * pipeTcpStreams - 
   * Pipes the input stream of current tcpstream to output stream of passed in TcpStream.
   * Will close the output TcpStream to signal end of file for next input
   * 
   * @param streamOut - TcpStream to write to
   * @throws IOException
   */
  public void pipeTcpStreams(TcpStream streamOut) throws IOException{
    copyStream(inStream, streamOut.outStream);
  }

  /**
   * getIpAddress -
   * Returns a string of the ip address to which
   * the socket is connected
   *
   * @return - ip address string
   */
  public String getIpAddress() {
    
    return socket.getInetAddress().toString();
  }

  /**
   * getPort -
   * returns the port that socket is connected to as an int
   *
   * @return - int, port number
   */
  public int getPort() {
    
    return socket.getPort();
  }

  /**
   * close -
   * Closes the socket
   * @throws IOException
   */
  public void close() throws IOException {
    
    socket.close();
  }

  /**
   * copyStream -
   * Helper method to copy the contents on inputstream into output stream using a buffer
   * THe output stream is closed to signal end of writing for next input stream.
   * 
   * @param in - input stream to read
   * @param out - output stream to read to
   * @throws IOException
   */
  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    
    BufferedOutputStream buffOut = new BufferedOutputStream(out);

    int c = 0;
    byte[] buff = new byte[BUFFER_SIZE];

    while ((c = in.read(buff)) > 0) {
      buffOut.write(buff, 0, c);
    }
    buffOut.close();
  }
}
