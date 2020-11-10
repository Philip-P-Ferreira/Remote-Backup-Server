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
   * write -
   * writes a string to the output of the TcpStream.
   * Newline denotes end of one line or message
   *
   * @param str - message to write
   * @throws IOException
   */
  public void write(String str) throws IOException {
    outputStream.writeBytes(str);
  }

  /**
   * read -
   * reads a message from the input of the TcpStream. Reads next line (up to
   * newline character) Throws IOException in all normal cases AND if the
   * resulting string is NULL
   *
   * @return - message from input stream
   * @throws IOException
   */
  public String read() throws IOException {
    String readLine = inputStream.readLine();
    if (readLine == null) {
      throw new IOException();
    }
    return readLine;
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
