package server;

import java.net.*;
import java.io.*;
import java.util.*;
import org.json.*;
import java.lang.*;

import java.io.FileNotFoundException;
import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;
import java.util.concurrent.locks.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
//import org.json.ParseException;
//import org.json.simple.parser.*;
import java.util.LinkedHashMap;
import java.util.Map;

class SockBaseServer implements Runnable {
    static String logFilename = "logs.txt";
    Lock mutex = new ReentrantLock();
    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;

    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer. 
    public void run() {
        String name = "";
        Response response;
        boolean quit = false;
        String question;
        int x, y, nextAnswer, answer = 0;
        Random rand = new Random();

        System.out.println("Ready...");
        try {
            // read the proto object and put into new objct
            Request op = Request.parseDelimitedFrom(in);
            String result = null;         

            // if the operation is NAME (so the beginning then say there is a commention and greet the client)
            if (op.getOperationType() == Request.OperationType.NAME) {
                // get name from proto object
                name = op.getName();

                // writing a connect message to the log with name and CONNENCT
                writeToLog(name, Message.CONNECT);
                System.out.println("Got a connection and a name: " + name);
                updateLeaderBoard(name, false, true);
                response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.GREETING)
                    .setMessage("Hello " + name + " and welcome.\n")
                    .build();
                response.writeDelimitedTo(out);
            } else {
                response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.BYE)
                    .setMessage("\nFirst communication with server should be NAME OperationType.\nGoodbye.\n")
                    .build();
                response.writeDelimitedTo(out);
                quit = true;
            }

            while(!quit){
                x = rand.nextInt(13);
                y = rand.nextInt(13);
                nextAnswer = x * y;
                
                question = "What is the product of " + x + " x " + y + "?\t(Answer: " + nextAnswer + ")";

                op = Request.parseDelimitedFrom(in);
                
                if (op.getOperationType() == Request.OperationType.QUIT) {
                    response = Response.newBuilder()
                        .setResponseType(Response.ResponseType.BYE)
                        .setMessage("\nGoodbye.\n")
                        .build();
                    response.writeDelimitedTo(out);
                    quit = true;
                    System.out.println("----Client " + name + " has left----");
                } else if (op.getOperationType() == Request.OperationType.LEADER) {
                    System.out.println("---LEADER Request---");
                    Response.Builder response2 = Response.newBuilder();
                    response2.setResponseType(Response.ResponseType.LEADER);

                    try {
                        FileInputStream ins = new FileInputStream("leaderboard.json");
                        JSONObject obj = new JSONObject(new JSONTokener(ins));
                        Iterator<String> keys = obj.keys();

                        while(keys.hasNext()) {
                            String key = keys.next();
                            if (obj.get(key) instanceof JSONObject) {
                                JSONObject person = obj.getJSONObject(key);
                                System.out.print("Name: " + key + ", wins: " + person.getInt("wins") + ", logins: " + person.getInt("logins") + ".\n");
                                response2.addLeader(Entry.newBuilder().setName(key).setWins(person.getInt("wins")).setLogins(person.getInt("logins")));    
                            }
                        }
                        response = response2.build();
                        ins.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    response.writeDelimitedTo(out);
                } else if (op.getOperationType() == Request.OperationType.NEW) {
                    System.out.println("---NEW Game Request---");
                    answer = x * y;
                    System.out.println("---Correct answer is "+ nextAnswer  + " for " + name + "---");
                    game.newGame();
                    response = Response.newBuilder()
                        .setResponseType(Response.ResponseType.TASK)
                        .setImage(game.getImage())
                        .setTask(question)
                        .setEval(true)
                        .build();
                    response.writeDelimitedTo(out);
                } else if (op.getOperationType() == Request.OperationType.ANSWER) {
                    System.out.println("---ANSWER Request---");
                    System.out.println("---Correct answer is "+ nextAnswer  + " for " + name + "---");
                    String isExit = op.getAnswer().toLowerCase();
                    if(isExit.equals("exit")) {
                        response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.BYE)
                            .setMessage("\nGoodbye.\n")
                            .build();
                        response.writeDelimitedTo(out);
                    } else if(game.getWon()) {
                        updateLeaderBoard(name, true, false);
                        response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.WON)
                            .setImage(game.getImage())
                            .build();
                        response.writeDelimitedTo(out);
                    } else if(op.getAnswer().equals(String.valueOf(answer))) {
                        answer = x * y;
                        response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setImage(replace(game.getIdxMax()/5))
                            .setTask(question)
                            .setEval(true)
                            .build();
                        response.writeDelimitedTo(out);
                    } else {
                        answer = x * y;
                        response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setImage(game.getImage())
                            .setTask(question)
                            .setEval(false)
                            .build();
                        response.writeDelimitedTo(out);
                    }
                } else {
                    System.out.println("---UNKNOWN Request---");
                    response = Response.newBuilder()
                        .setResponseType(Response.ResponseType.ERROR)
                        .setMessage("Unknown Request.\n")
                        .build();
                    response.writeDelimitedTo(out);
                }
            }
        } catch (SocketException se) {
            System.out.println("----Lost Client Unexpectantly----");
            se.printStackTrace();
        } catch (NullPointerException npe) {
            System.out.println("----Client " + name + " has left----");
        }catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (out != null)  out.close();
                if (in != null)   in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error in finally during run().");
            }
        }
    }

    /**
     * Replaces num characters in the image. I used it to turn more than one x when the task is fulfilled
     * @param num -- number of x to be turned
     * @return String of the new hidden image
     */
    public String replace(int num){
        mutex.lock();
        for (int i = 0; i < num; i++){
            if (game.getIdx() < game.getIdxMax()) {
                game.replaceOneCharacter();
            }
            if (game.getIdx() >= game.getIdxMax()) {
                game.setWon();

            }
        }
        mutex.unlock();
        return game.getImage();
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
            // read old log file 
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }

    private void updateLeaderBoard(String name, boolean wins, boolean logins){
        try {
            name = name.toLowerCase();
            FileInputStream ins = new FileInputStream("leaderboard.json");
            JSONObject obj = new JSONObject(new JSONTokener(ins));
            JSONObject person = null;
            try{
                person = obj.getJSONObject(name);
            } catch (Exception e) {
                System.out.println("Did not find!");
            }
            if(person != null) {
                if(wins) {
                    person.put("wins", person.getInt("wins") + 1);
                    System.out.println("Updated " + name + " to add a win. Total wins: " + person.getInt("wins"));
                }
                if(logins) {
                    person.put("logins", person.getInt("logins") + 1);
                    System.out.println("Updated " + name + " to add a login. Total logins: " + person.getInt("logins"));
                }
            } else {
                JSONObject newEntry = new JSONObject();
                Map stats = new LinkedHashMap(2);
                stats.put("wins", 0);
                stats.put("logins", 1);
                obj.put(name, stats);
                person = obj.getJSONObject(name);
                System.out.println("Added new person " + name + " with " + person.getInt("wins") + " wins and " + person.getInt("logins") + " logins.");
            }

            try {
                PrintWriter outputFile = new PrintWriter("leaderboard.json");
                mutex.lock();
                outputFile.println(obj.toString());
                mutex.unlock();
                outputFile.flush();
                outputFile.close();
                ins.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main (String args[]) throws Exception {
        Game game = new Game();
        int pool = 5;

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        ServerSocket serv = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            serv = new ServerSocket(port);
            Executor threadPool = Executors.newFixedThreadPool(pool);        
            while(true) {

                clientSocket = serv.accept();
                threadPool.execute(new SockBaseServer(clientSocket, game));
            }
        } catch(Exception e) {
            e.printStackTrace();
            if (clientSocket != null) clientSocket.close();
            System.exit(2);
        }
    }
}