package commonutils;

import java.io.*;
import java.net.*;

public class TcpStream {
  private Socket socket;
  private DataOutputStream outputStream;
  private BufferedReader inputStream;

  // Constructor
  public TcpStream(String serverAddress, int port) throws IOException {
    socket = new Socket(serverAddress, port);
    outputStream = new DataOutputStream(socket.getOutputStream());
    inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  // Alt constructor (from a Socket)
  public TcpStream(Socket socket) throws IOException {
    this.socket = socket;
    outputStream = new DataOutputStream(socket.getOutputStream());
    inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  /**
   * writeMessage -
   * writes a string to the output of the TcpStream.
   * Adds newline to denote end of message
   *
   * @param str - message to write
   * @throws IOException
   */
  public void writeMessage(String str) throws IOException {
    outputStream.writeBytes(str + '\n');
  }

  public void writeFromInputStream(InputStream inStream) throws IOException {
      int data = inStream.read();
      while (data != -1) {
          outputStream.write(data);
          data = inStream.read();
      }
  }

  /**
   * readMessage -
   * reads a message from the input of the TcpStream. Reads next line (up to
   * newline character) Throws IOException in all normal cases AND if the
   * resulting string is NULL
   *
   * @return - message from input stream
   * @throws IOException
   */
  public String readMessage() throws IOException {
    String readLine = inputStream.readLine();
    if (readLine == null) {
      throw new IOException();
    }
    return readLine;
  }

  public void readToOutputStream(OutputStream outStream) throws IOException {
    int data = inputStream.read();
    while (data != -1) {
        outStream.write(data);
        data = inputStream.read();
    }
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
}
