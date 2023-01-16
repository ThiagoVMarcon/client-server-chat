import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class User {
  String name;
  SocketChannel sc;
  State state;

  User(String name, SocketChannel sc) {
    this.name = name;
    this.sc = sc;
    this.state = State.INIT;
  }
}

class Room {
  String name;
  Set<User> UserList;

  Room(String name) {
    this.name = name;
    UserList = new HashSet<>();
  }
}

enum State {
  INIT,
  OUTSIDE,
  INSIDE;
}

public class ChatServer {
  // Map of users and rooms
  static private final Map<String, User> UsersMap = new HashMap<>();
  static private final Map<String, Room> RoomsMap = new HashMap<>();

  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  static public void main(String args[]) throws Exception {
    // Parse port from command line
    int port = Integer.parseInt(args[0]);

    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();

      // Set it to non-blocking, so we can use select
      ssc.configureBlocking(false);

      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress(port);
      ss.bind(isa);

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("Listening on port " + port);

      while (true) {
        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();

          // What kind of activity is it?
          if (key.isAcceptable()) {

            // It's an incoming connection. Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println("Got connection from " + s);

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking(false);

            // Register it with the selector, for reading
            sc.register(selector, SelectionKey.OP_READ, new User("", sc));

          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel) key.channel();
              boolean ok = processInput(sc, selector, key);

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println("Closing connection to " + s);
                  s.close();
                } catch (IOException ie) {
                  System.err.println("Error closing socket " + s + ": " + ie);
                }
              }

            } catch (IOException ie) {

              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();
              } catch (IOException ie2) {
                System.out.println(ie2);
              }

              System.out.println("Closed " + sc);
            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch (IOException ie) {
      System.err.println(ie);
    }
  }

  // Just read the message from the socket and send it to stdout
  static private boolean processInput(SocketChannel sc, Selector selector, SelectionKey keySource) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read(buffer);
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit() == 0) {
      return false;
    }

    // Decode and print the message to stdout
    String message = decoder.decode(buffer).toString();
    processMessage(sc, keySource, message);
    // System.out.print( message );
    if (keySource.attachment() == null) {
      keySource.attach(message);
    }

    return true;
  }

  // Process the message received from the socket
  static private void processMessage(SocketChannel sc, SelectionKey keySource, String message) throws IOException {
    // The message is a command
    if (message.charAt(0) == '/') {
      String command[] = message.split(" ", 2);
      switch (command[0]) {
        case "/nick":
          // passar para dentro da função processNick
          String nick = command[1].replaceAll("[\\n\\t ]", "");
          nick = nick.substring(0, nick.length() - 1);
          processNick(sc, keySource, nick);  
          break;
        case "/join":
          // join room function
          break;
        case "/leave":
          // leav room function
          break;
        case "/bye":
          // bye function
          break;
      }
    }
    // The message is not a command 
    // else {
    //   buffer.clear();
    //   buffer.put(message.getBytes(charset));
    //   buffer.flip();
    //   sc.write(buffer);
    // }
    message(sc, message);
    
  }

  static private void processNick(SocketChannel sc, SelectionKey keySource, String newName) throws IOException {
    User actual = (User) keySource.attachment();
    if (UsersMap.containsKey(newName)) {
      {
        buffer.clear();
        buffer.put("ERROR\n".getBytes(charset));
        buffer.flip();
        sc.write(buffer);
        return;
      }
    }    
    UsersMap.remove(actual.name);
    actual.name = newName;
    actual.state = State.OUTSIDE;
    UsersMap.put(actual.name, actual);
    buffer.clear();
    buffer.put("OK\n".getBytes(charset));
    buffer.flip();
    sc.write(buffer);
  }

  static private void processJoin(SocketChannel sc, SelectionKey keySource) throws IOException {

  }

  static private void processLeave(SocketChannel sc, SelectionKey keySource) throws IOException {
    
  }

  static private void processBye(SocketChannel sc, SelectionKey keySource) throws IOException {
    
  }

  static private void message(SocketChannel sc, String message) throws IOException {
    for (Map.Entry<String, User> set : UsersMap.entrySet()) {
    } 
  }
}
