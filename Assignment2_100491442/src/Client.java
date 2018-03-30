import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;


/*
*
* Andrew Dale
*
* Mar 27, 2018
*
* Client Class
*
*
* */

public class Client extends Application {
    private TableView<fileGen> localFilesTable;
    private TableView<fileGen> serverFilesTable;
    private TableColumn<fileGen, String> localFilesCol;
    private TableColumn<fileGen, String> remoteFilesCol;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private BufferedReader fileIn;
    private File myDirectory;
    private PrintWriter fOut;
    private Stage stage;
    private TextField ta = new TextField();

    @Override
    public void start(Stage primaryStage) throws Exception {

        this.stage = primaryStage;

        Pane root = new Pane();
        BorderPane layout = new BorderPane();
        Button uploadB = new Button("_Upload");

        primaryStage.getIcons().add(new Image("Squirtlewhat.png"));

        // Upload Button
        uploadB.getStyleClass().add("up-button");
        uploadB.setPrefWidth(75);
        uploadB.setAlignment(Pos.BASELINE_RIGHT);
        uploadB.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TablePosition tablePosition = localFilesTable.getSelectionModel().getSelectedCells().get(0);
                int row = tablePosition.getRow();
                fileGen fileGen = localFilesTable.getItems().get(row);
                TableColumn column = tablePosition.getTableColumn();
                String fileName = (String) column.getCellObservableValue(fileGen).getValue();
                System.out.println("fileName selected = " + fileName);
                servConnect();
                out.println("UPLOAD " + fileName);
                out.flush();
                String line;
                try {
                    fileIn = new BufferedReader(new FileReader(myDirectory.getPath() + "\\" + fileName));
                    while ((line = fileIn.readLine()) != null) {
                        out.println(line);
                        out.flush();
                    }
                    out.println("\0");
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }



                boolean Found = false;
                for(fileGen entry :  serverFilesTable.getItems()){
                    if(entry.getFileName().equals(fileGen.getFileName())){
                        Alert alert = new Alert(Alert.AlertType.ERROR, "File Already Exists in Target DIR",
                                ButtonType.OK);
                        alert.showAndWait();
                        Found = true;
                        break;
                    }
                    Found = false;


                }
                if (!Found) {
                    serverFilesTable.getItems().add(fileGen);
                    System.out.println("File upload complete");
                    refresh();
                }





            }
        });

        // Download Button

        Button downloadB = new Button("_Download");
        downloadB.setPrefWidth(95);
        downloadB.setAlignment(Pos.BASELINE_RIGHT);
        downloadB.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                TablePosition tablePosition = serverFilesTable.getSelectionModel().getSelectedCells().get(0);
                int row = tablePosition.getRow();
                fileGen fileGen = serverFilesTable.getItems().get(row);
                TableColumn column = tablePosition.getTableColumn();

                String fileName = (String) column.getCellObservableValue(fileGen).getValue();
                System.out.println("fileName selected = " + fileName);
                servConnect();
                out.println("DOWNLOAD " + fileName);
                out.flush();
                String line;
                File downloadFile = new File(myDirectory.getPath() + "\\" + fileName);

                try {
                    fOut = new PrintWriter(downloadFile);
                    while ((line = in.readLine()) != null) {
                        if (line.equals("\0")) {
                            break;
                        }
                        fOut.println(line);
                    }
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Update file listing
                boolean Found = false;
                for(fileGen entry :  localFilesTable.getItems()){
                    if(entry.getFileName().equals(fileGen.getFileName())){
                        Alert alert = new Alert(Alert.AlertType.ERROR, "File Already Exists in Target DIR",
                                ButtonType.OK);
                        alert.showAndWait();
                        Found = true;
                        break;
                    }
                    Found = false;


                }
                if (!Found) {
                    localFilesTable.getItems().add(fileGen);
                    System.out.println("File Download Complete");
                    refresh();
                }

            }
        });


        Button exitB = new Button("E_xit");
        exitB.setOnAction(evt -> System.exit(0));
        exitB.setPrefWidth(125);

        Button refreshB = new Button("_Refresh");
        refreshB.setOnAction(e -> refresh());
        refreshB.getStyleClass().add("refresh-button");
        refreshB.setPrefWidth(85);
        refreshB.setAlignment(Pos.BASELINE_LEFT);



        HBox hbox = new HBox(10);
        HBox hbox2 = new HBox(10);
        AnchorPane apane = new AnchorPane();
        hbox.setPadding(new Insets(10, 0, 10, 10));
        hbox.getChildren().addAll(uploadB, downloadB);
        hbox2.setPadding(new Insets(10, 10,  10, 10));
        hbox2.getChildren().add(refreshB);
        apane.getChildren().addAll(hbox, hbox2);
        AnchorPane.setRightAnchor(hbox2, 0.0);
        AnchorPane.setLeftAnchor(hbox, 0.0);
        layout.setTop(apane);



        ta.setEditable(false);
        Button ChooseDir = new Button("Ch_oose Dir...");
        ChooseDir.setOnAction(e -> newDir());
        ChooseDir.setPrefWidth(125);
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(ChooseDir, exitB, ta);
        vbox.setPadding(new Insets(10));



        layout.setBottom(vbox);



        // Initial Fill of Tables

        localFilesTable = new TableView<>();
        serverFilesTable = new TableView<>();
        localFilesCol = new TableColumn<>("Local Files");
        localFilesCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        localFilesCol.setMinWidth(300);
        localFilesTable.getColumns().add(localFilesCol);

        remoteFilesCol = new TableColumn<>("Server Files");
        remoteFilesCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        remoteFilesCol.setMinWidth(300);
        serverFilesTable.getColumns().add(remoteFilesCol);

        layout.setLeft(localFilesTable);
        layout.setRight(serverFilesTable);

        stage.setResizable(false);

        root.getChildren().add(layout);
        root.getStylesheets().add("Style.css");

        downloadB.getStyleClass().add("down-button");

        primaryStage.setTitle("Sharing Pokedex .CSV File Machine");
        Scene scene = new Scene(root, 600, 560);

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_ANY),
                new Runnable() {
                    @Override
                    public void run() {
                        uploadB.fire();
                    }
                }
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_ANY),
                new Runnable() {
                    @Override
                    public void run() {
                        downloadB.fire();
                    }
                }
        );

        scene.getAccelerators().put(
                 new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_ANY),
                new Runnable() {
                    @Override
                    public void run() {
                        exitB.fire();
                    }
                }
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_ANY),
                new Runnable() {
                    @Override
                    public void run() {
                        ChooseDir.fire();
                    }
                }
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_ANY),
                new Runnable() {
                    @Override
                    public void run() {
                        refreshB.fire();
                    }
                }
        );


        primaryStage.setScene(scene);
        primaryStage.show();

        servConnect();
        ConnectionHandler connectionHandler = new ConnectionHandler();
        Thread t = new Thread(connectionHandler);
        t.start();
        getLocalFiles();


    }

    // Stage refresh, kind of a janky solution
    private void refresh(){
        try {
            ta.clear();
            stage.close();
            start(stage);
        }
        catch(Exception e){
            // who cares
        }
    }

    // New Directory Selector, only lists csvs and txts
    private void newDir(){

        ObservableList<fileGen> localFiles = FXCollections.observableArrayList();
        DirectoryChooser dCho = new DirectoryChooser();
        dCho.setInitialDirectory(new File(System.getProperty("user.home")));
        File mainDir = dCho.showDialog(null);
        ta.setText(mainDir.toString());


        if(mainDir != null){
            localFilesTable.getItems().clear();
            File[] filelist = mainDir.listFiles();
            for(File files : filelist){
                if((files.toString().contains(".txt") || files.toString().contains(".csv"))
                        && !files.toString().contains(" ")){
                    localFiles.add(new fileGen(files));
                }


            }
            myDirectory = mainDir;
            localFilesTable.setItems(localFiles);

        }







    }

    private void servConnect() {
        int port = 8080;
        try {
            clientSocket = new Socket("127.0.0.1", port);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream());
            System.out.println("Now connected to server");

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Could not connect to server. Check connection and try again." +
                    "\nPort: "  + port, ButtonType.OK);
            alert.setHeaderText("Failure to connect.");
            alert.showAndWait();
            if(alert.getResult() == ButtonType.OK){
                System.exit(1);
            }
            alert.setOnCloseRequest(new EventHandler<DialogEvent>() {
                @Override
                public void handle(DialogEvent event) {
                    System.exit(1);
                }
            });
        }


    }

    // Getting client files
    private void getLocalFiles() {
        ObservableList<fileGen> localFiles = FXCollections.observableArrayList();
        myDirectory = new File("src\\clientFiles");
        File[] fileList = myDirectory.listFiles();
        for (File entry : fileList) {
            if (entry.isFile()) {

                localFiles.add(new fileGen(entry));
            }

        }

        localFilesTable.setItems(localFiles);



    }
    public class ConnectionHandler implements Runnable {

        @Override
        public void run() {

            getRemoteFilesList();
        }


        // Receiving Server Files

        public synchronized void getRemoteFilesList() {

            ObservableList<fileGen> remoteFiles = FXCollections.observableArrayList();
            String fileName;
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                out.println("DIR");
                out.flush();



                while ((fileName = in.readLine()) != null) {
                    if (fileName.equals("\0")) {
                        break;
                    }

                    remoteFiles.add(new fileGen(fileName));

                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            serverFilesTable.setItems(remoteFiles);

        }

    }



    public static void main(String[] args) {
        launch(args);
    }
}
