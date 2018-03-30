import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


/*
*
*
* Andrew Dale
*
* Mar 26, 2018
*
* Server class
*
*
* */

public class Server extends Application implements Runnable {
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private File serverDirectory;
    private ArrayList<fileGen> fileGenArrayList;
    private File[] filesList;
    BufferedReader in;
    Thread a;


    public void start(Stage primaryStage){
        BorderPane pane = new BorderPane();
        Button close = new Button("Kill Me");
        pane.getStylesheets().add("Style.css");
        close.setPrefSize(200,200);
        close.setOnAction(e -> System.exit(0));
        close.getStyleClass().add("server-button");

        pane.setCenter(close);

        Scene scene = new Scene(pane, 200, 200);
        primaryStage.setTitle("Server Killer");
        primaryStage.setResizable(false);

        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image("server.png"));
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        primaryStage.show();
        a = new Thread(new Server());
        a.start();
    }

    public void run(){
        new Server().go();

    }



    public static void main(String[] args) {
        launch(args);


    }
    public void go() {
        // Get files from server
        serverDirectory = new File("src\\serverFiles");

        // Start listening for connections
        try {
            serverSocket = new ServerSocket(8080);
            while (true) {
                System.out.println("Listening");
                clientSocket = serverSocket.accept();
                System.out.println("Client connected");
                Thread t = new Thread(new ClientConnectionHandler());
                t.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public class ClientConnectionHandler implements Runnable {

        @Override
        public void run() {
            getClientCommand();
        }
        // Method to disconnect the client
        private void disconnect() {
            try {
                clientSocket.close();
                System.out.println("Client disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Interpret command
        private synchronized void getClientCommand() {
            String clientCommand;
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                clientCommand = in.readLine();
                String clientCommandTokens[] = clientCommand.split(" ");
                for (int i = 0; i < clientCommandTokens.length; i++) {
                    String entry = clientCommandTokens[i];
                    System.out.println("Command: " + clientCommandTokens[i]);
                }

                if (clientCommandTokens[0].equals("DIR")) {
                    System.out.println("Received command: " + clientCommandTokens[0]);
                    sendFilesList();
                }
                else if (clientCommandTokens[0].equals("UPLOAD")) {
                    System.out.println("Received command: " + clientCommandTokens[0]);
                    receiveFile(clientCommandTokens[1]);
                }
                else if (clientCommandTokens[0].equals("DOWNLOAD")) {
                    System.out.println("Received command: " + clientCommandTokens[0]);
                    fileSend(clientCommandTokens[1]);
                }
                else {
                    System.out.println("Unknown command");
                    disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        private synchronized void sendFilesList() {

            fileGenArrayList = new ArrayList<>();
            filesList = serverDirectory.listFiles();
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                for (File entry : filesList) {
                    System.out.println(entry.getName());
                    out.println(entry.getName());
                    out.flush();
                    fileGenArrayList.add(new fileGen(entry));
                }
                out.println("\0");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            disconnect();

        }

        private synchronized void receiveFile(String fileName) {
            System.out.println("Attempting to receive: " + fileName);
            File receiveFile = new File(serverDirectory.getPath() + "\\" + fileName);
            System.out.println("File created: " + serverDirectory.getPath() + "\\" + fileName);
            String line;
            try {
                PrintWriter fout = new PrintWriter(receiveFile);

                while ((line = in.readLine()) != null) {
                    if (line.equals("\0")) {
                        break;
                    }
                    fout.println(line);
                    fout.flush();
                }
                fout.close();
                System.out.println("File successfully received by client");
            } catch (IOException e) {
                e.printStackTrace();
            }
            disconnect();


        }
        private synchronized void fileSend(String fileName) {
            int index = -1;
            System.out.println("File: " + fileName);
            for (fileGen entry : fileGenArrayList) {
                System.out.println(entry.getFileName());
                if (entry.getFileName().equals(fileName)) {

                    System.out.println("Match Found");
                    index = fileGenArrayList.indexOf(entry);
                }
            }
            String line;

            try {
                in = new BufferedReader(new FileReader(serverDirectory + "\\"
                        + fileGenArrayList.get(index).getFileName()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                while ((line = in.readLine()) != null) {
                    out.println(line);
                    out.flush();
                }
                out.println("\0");
                out.flush();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


}
