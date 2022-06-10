import java.util.Timer;
import java.util.TimerTask;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import org.json.*;
import org.json.JSONArray;
import org.json.JSONObject;
//import org.json.ParseException;
//import org.json.simple.parser.*;

public class Game {
	private String name;
	private int points;
	private int roundPoints;
	private int wins;
	private String state;
	private String currentImage;
	private String currentFolder = "captainamerica";
	private String currentQuote = "/quote1.png";
	private Timer timer;

	public Game() {
		state = "new";
		points = 0;
		roundPoints = 5;
		wins = 0;
		name = "n/a";
		currentImage = "img/hi.png";

	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public String getName() {
		return name;
	}

	public int getPoints() {
		return points;
	}

	public String getState() {
		return state;
	}

	public String getImage() {
		return currentImage;
	}

	public String clientInput(String newInput) {
		String singleInput = newInput.replaceAll("\\s","");
		String input = singleInput.toLowerCase();
		if(input.equals("quit")) {
			return "quit";
		}
		if(state.equals("new")) {
			name = input;
			reset();
			return "Hello " + input + ",\nPlease enter \"start\" to begin game, \"score\" to see leader board, or \"quit\" to exit";
		}
		if (state.equals("menu")) {
			switch(input) {
				case "start":
					state = "playing";
					currentImage = "img/" + currentFolder + currentQuote;
					System.out.println("--Answer is: " + currentFolder);
					startTimer();
					return "Starting\nWho said this? (type \"next\" for new quote from this character or \"next\" for a new character quote)";
				case "score":
					return getLeaderBoard() + "\nPlease enter \"start\" to begin game, \"score\" to see leader board, or \"quit\" to exit";
				default:
					return "Invalid Option";
			}
		}
		if (state.equals("playing")) {
			if(wins == -1) {
				currentImage = "img/lose.jpg";
				state = "new";
				return "TIME IS UP!!!\n\nHello, please tell me your name.";
			}
			if(input.equals("next")){
				points -= 2;
				nextfolder();
				currentQuote = "/quote1.png";
				imageUpdate();
				System.out.println("--Answer is: " + currentFolder);
				return "Who said this? (type \"more\" for new quote from this character or \"next\" for a new character quote)";
			}
			if(input.equals("more")){
				if(currentQuote.equals("/quote4.png")){
					return "There are no more quotes for this character.";
				}
				if (currentQuote.equals("/quote3.png")) {
					roundPoints = 1;
				} else {
					roundPoints--;
				}
				nextQuote();
				imageUpdate();
				System.out.println("--Answer is: " + currentFolder);
				return "Who said this? (type \"more\" for new quote from this character or \"next\" for a new character quote)";
			}
			if(input.equals(currentFolder)) {
				wins++;
				points += roundPoints;
				if(wins == 3) {
					timer.cancel();
					currentImage = "img/win.jpg";
					updateLeaderBoard();
					state = "new";
					return "\nHello, please tell me your name.";
				}
				roundPoints = 5;
				nextfolder();
				currentQuote = "/quote1.png";
				imageUpdate();
				System.out.println("--Answer is: " + currentFolder);
				return "CORRECT!!!\nWho said this? (type \"more\" for new quote from this character or \"next\" for a new character quote)";
			} else {
				return "--Incorrect.--";
			}
		}
		return "";
	}

	public void reset() {
		state = "menu";
		points = 0;
		roundPoints = 5;
		wins = 0;
		currentImage = "img/hi.png";
	}

	private void nextQuote() {
		if (currentQuote.equals("/quote1.png")) {
			currentQuote = "/quote2.png";
		} else if (currentQuote.equals("/quote2.png")) {
			currentQuote = "/quote3.png";
		} else if (currentQuote.equals("/quote3.png")) {
			currentQuote = "/quote4.png";
		} else {
			currentQuote = "/quote1.png";
		}
	}

	private void nextfolder() {
		if (currentFolder.equals("captainamerica")) {
			currentFolder = "darthvader";
		}  else if (currentFolder.equals("darthvader")) {
			currentFolder = "homersimpson";
		} else if (currentFolder.equals("homersimpson")) {
			currentFolder = "jacksparrow";
		} else if (currentFolder.equals("jacksparrow")) {
			currentFolder = "joker";
		} else if (currentFolder.equals("joker")) {
			currentFolder = "tonystark";
		} else if (currentFolder.equals("tonystark")) {
			currentFolder = "wolverine";
		} else {
			currentFolder = "captainamerica";
		}
	}

	private void imageUpdate() {
		currentImage = "img/" + currentFolder + currentQuote;
	}

	private void startTimer() {
		timer = new Timer();
		timer.schedule(new Lose(), 60000);
	}

	class Lose extends TimerTask {
		public void run() {
			wins = -1;
			timer.cancel();
		}
	}

	private void updateLeaderBoard(){
		try {
			FileInputStream ins = new FileInputStream("leaderboard.json");
        	JSONObject obj = new JSONObject(new JSONTokener(ins));
        	JSONObject person = null;
        	try{
        		person = obj.getJSONObject(name);
        	} catch (Exception e) {
          		System.out.println("Did not find!");
        	}
        	if(person != null) {
        		System.out.println("Persons score is " + person.getInt("score"));
        		person.put("score", person.getInt("score") + points);
        		System.out.println("Persons score is " + person.getInt("score"));
        	} else {
        		JSONObject pts = new JSONObject();
        		pts.put("score", points);
        		obj.put(name, pts);
        		person = obj.getJSONObject(name);
        		System.out.println("Persons score is " + person.getInt("score"));
        	}

			PrintWriter outputFile = new PrintWriter("leaderboard.json");
      		outputFile.println(obj.toString());
	        outputFile.flush();
	        outputFile.close();
	        ins.close();
        } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private String getLeaderBoard() {
		try {
			FileInputStream ins = new FileInputStream("leaderboard.json");
        	JSONObject obj = new JSONObject(new JSONTokener(ins));
        	ins.close();
        	return obj.toString();
        } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return "";
	}  
}