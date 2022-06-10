

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Base64;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.json.*;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with the current state
 *     -> modal means that it opens the GUI and suspends background processes. Processing 
 *        still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements OutputPanel.EventHandlers {
  JDialog frame;
  PicturePanel picturePanel;
  OutputPanel outputPanel;
  static String greeting = "";
  //int port = 8080;
  static Socket sock = null;
  //String host = "localhost";
  static Scanner scanner = new Scanner(System.in);
  static OutputStream out;
  static InputStream in;

  /**
   * Construct dialog
   */
  public ClientGui() {
    frame = new JDialog();
    frame.setLayout(new GridBagLayout());
    frame.setMinimumSize(new Dimension(1000, 1000));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    // setup the top picture frame
    picturePanel = new PicturePanel();
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 0.25;
    frame.add(picturePanel, c);

    // setup the input, button, and output area
    c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.weighty = 0.75;
    c.weightx = 1;
    c.fill = GridBagConstraints.BOTH;
    outputPanel = new OutputPanel();
    outputPanel.addEventHandlers(this);
    frame.add(outputPanel, c);
  }

  /**
   * Shows the current state in the GUI
   * @param makeModal - true to make a modal window, false disables modal behavior
   */
  public void show(boolean makeModal) {
    frame.pack();
    frame.setModal(makeModal);
    frame.setVisible(true);
  }

  /**
   * Creates a new game and set the size of the grid 
   * @param dimension - the size of the grid will be dimension x dimension
   */
  public void newGame(int dimension) {
    picturePanel.newGame(dimension);
    outputPanel.appendOutput(greeting);
  }

  /**
   * Insert an image into the grid at position (col, row)
   * 
   * @param filename - filename relative to the root directory
   * @param row - the row to insert into
   * @param col - the column to insert into
   * @return true if successful, false if an invalid coordinate was provided
   * @throws IOException An error occured with your image file
   */
  public boolean insertImage(String filename, int row, int col) throws IOException {
    String error = "";
    try {
      // insert the image
      if (picturePanel.insertImage(filename, row, col)) {
      // put status in output
      //  outputPanel.appendOutput("Inserting " + filename + " in position (" + row + ", " + col + ")");
        return true;
      }
      error = "File(\"" + filename + "\") not found.";
    } catch(PicturePanel.InvalidCoordinateException e) {
      // put error in output
      error = e.toString();
    }
    outputPanel.appendOutput(error);
    return false;
  }

  /**
   * Submit button handling
   * 
   * Change this to whatever you need
   */
  @Override
  public void submitClicked() {
    String input = outputPanel.getInputText();
    // if has input
    if (input.length() > 0) {
      // append input to the output panel
      outputPanel.appendOutput(input);
      // clear input text box
      outputPanel.setInputText("");
    }

    try {
      JSONObject request = new JSONObject();
      request.put("request", input);
      NetworkUtils.Send(out, JsonUtils.toByteArray(request));
      byte[] responseBytes = NetworkUtils.Receive(in);
      JSONObject response = JsonUtils.fromByteArray(responseBytes);

      if (response.has("error")) {
        System.out.println(response.getString("error"));
      } else if (response.getString("message").equals("quit")) {
        System.exit(0);
      } else if (response.getString("header").equals("standard")){
        //String quitTest = response.getInt("points").equals("quit")
        outputPanel.setPoints(response.getInt("points"));
        outputPanel.appendOutput(response.getString("message"));
        System.out.println("Your image");
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(response.getString("data"));
        ImageIcon icon = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
          picturePanel.insertImage(bais, 0, 0);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
          System.out.println("Client had a problem reading server message.");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Key listener for the input text box
   * 
   * Change the behavior to whatever you need
   */
  @Override
  public void inputUpdated(String input) {
    if (input.equals("surprise")) {
      outputPanel.appendOutput("Don't scare me!");
    }
  }

  public static void main(String[] args) throws IOException, ClassNotFoundException {
    // create the frame
    String host = "localhost";
    int port = 8080;
    if (args.length >= 1){ // host, if provided
      host=args[0];
    }
    if (args.length >= 2){
      port = Integer.valueOf(args[1]);
    }
    ClientGui main = new ClientGui();

    try {
      sock = new Socket(host, port);
      out = sock.getOutputStream();
      in = sock.getInputStream();
      ObjectInputStream ins = new ObjectInputStream(in);
      greeting = (String) ins.readObject();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
  // setup the UI to display on image
    main.newGame(1);
    
    // add images to the grid
    main.insertImage("img/questions.jpg", 0, 0);
    
    // show the GUI dialog as modal
    main.show(true); // you should not have your logic after this. You main logic should happen whenever "submit" is clicked
  }
}
