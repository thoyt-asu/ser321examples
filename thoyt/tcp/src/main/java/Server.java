
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Random;

import javax.imageio.ImageIO;

import org.json.*;

public class Server {
  static int port = 8080;

  public static JSONObject response(Game game, String request) throws IOException {
    JSONObject json = new JSONObject();
    String returnMessage = game.clientInput(request);

    json.put("header", "standard");

    json.put("points", game.getPoints());

    json.put("message", returnMessage);

    File file = new File(game.getImage());
    if (!file.exists()) {
      System.err.println("Cannot find file: " + file.getAbsolutePath());
      System.exit(-1);
    }
    // Read in image
    BufferedImage img = ImageIO.read(file);
    byte[] bytes = null;
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ImageIO.write(img, "png", out);
      bytes = out.toByteArray();
    }
    if (bytes != null) {
      Base64.Encoder encoder = Base64.getEncoder();
      json.put("data", encoder.encodeToString(bytes));
      return json;
    }
    return error("Unable to save image to byte array");
  }

  public static JSONObject error(String err) {
    JSONObject json = new JSONObject();
    json.put("header", "error")
    json.put("error", err);
    return json;
  }

  public static void main(String[] args) throws IOException {
    ServerSocket serv = null;
    Game game = new Game();
    boolean go = true;
    try {
      serv = new ServerSocket(port);
      // NOTE: SINGLE-THREADED, only one connection at a time
      while (true) {
        Socket sock = null;
        try {
          sock = serv.accept(); // blocking wait
          OutputStream out = sock.getOutputStream();
          ObjectOutputStream os = new ObjectOutputStream(out);
          InputStream in = sock.getInputStream();
          os.writeObject("Hello, please tell me your name.");
          os.flush();
          while (go) {
            byte[] messageBytes = NetworkUtils.Receive(in);
            JSONObject message = JsonUtils.fromByteArray(messageBytes);
            JSONObject returnMessage = null;
            if (message.has("request")) {
                if (message.get("request") instanceof String) {                  
                  System.out.println("Client has entered: " + message.get("request") );
                  returnMessage = response(game, (String) message.get("request"));
                  String quitTest = (String) returnMessage.get("message");
                  if(quitTest.equals("quit")) {
                    //go = false;
                  }
                }
            } else {
              returnMessage = error("Invalid message received");
            }

            // we are converting the JSON object we have to a byte[]
            byte[] output = JsonUtils.toByteArray(returnMessage);
            NetworkUtils.Send(out, output);
          }
        } catch (Exception e) {
          System.out.println("Client disconnect");
        } finally {
          if (sock != null) {
            sock.close();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (serv != null) {
        serv.close();
      }
    }
  }
}