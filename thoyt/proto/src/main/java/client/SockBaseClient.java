package client;

import java.net.*;
import java.io.*;

import org.json.*;

import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

import java.util.*;
import java.util.stream.Collectors;

import java.lang.Thread;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        boolean quit = false;
        int i1=0, i2=0;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            //Initial request
            op.writeDelimitedTo(out);

            //Greetings
            response = Response.parseDelimitedFrom(in);
            System.out.println(response.getMessage());
            if(response.getResponseType() == Response.ResponseType.BYE) {
                quit = true;
            }

            // Correspondence
            while(!quit) {

                // read from the server
                System.out.println("\nWhat would you like to do? \n  1 - to see the leader board \n  2 - to enter a game\n  3 - to quit");

                try {
                    int intToSend = Integer.parseInt(stdin.readLine());

                    // QUIT Option
                    if(intToSend == 3) {
                        quit = true;
                        op = Request.newBuilder()
                            .setOperationType(Request.OperationType.QUIT)
                            .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());

                    // GAME OPTION    
                    } else if (intToSend == 2) {
                        op = Request.newBuilder()
                            .setOperationType(Request.OperationType.NEW)
                            .build();
                        op.writeDelimitedTo(out);
                        boolean end = false;

                        while (!end) {
                            response = Response.parseDelimitedFrom(in);
                            if(response.getResponseType() == Response.ResponseType.BYE) {
                                System.out.println(response.getMessage());
                                end = true;
                                quit = true;
                            } else if(response.getResponseType() == Response.ResponseType.TASK) {
                                System.out.println("\nTaskType: " + response.getResponseType());
                                System.out.println("Image: \n" + response.getImage());
                                System.out.println("Task: \n" + response.getTask());
                                System.out.println("Eval: \n" + response.getEval());

                                stdin = new BufferedReader(new InputStreamReader(System.in));
                                String answerToSend = stdin.readLine();

                                op = Request.newBuilder()
                                    .setOperationType(Request.OperationType.ANSWER)
                                    .setAnswer(answerToSend)
                                    .build();
                                op.writeDelimitedTo(out);

                            } else if(response.getResponseType() == Response.ResponseType.WON) {
                                System.out.println("\nTaskType: " + response.getResponseType());
                                System.out.println("Image: \n" + response.getImage());
                                System.out.println("YOU WON!\n");
                                end = true;
                            }
                        }

                    // LEADER OPTION
                    } else if (intToSend == 1) {
                        op = Request.newBuilder()
                            .setOperationType(Request.OperationType.LEADER)
                            .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        for(Entry leaders : response.getLeaderList()){
                            System.out.println("Name: " + leaders.getName() + ", Wins: " + leaders.getWins() + ", Logins: " + leaders.getLogins() + "."); 
                        }                  
                    } else {
                        op = Request.newBuilder()
                            .setOperationType(Request.OperationType.NAME)
                            .setName("invalid")
                            .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());                        
                    }
                } catch (NumberFormatException nfe) {
                    System.out.println("\n----Input must be an integer----\n");
                    nfe.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\n----Sever Fail----\n");
        } finally {
            Thread.sleep(2000);
            if (in != null)   in.close();
            if (out != null)  out.close();
            if (serverSock != null) serverSock.close();
        }
    }
}


