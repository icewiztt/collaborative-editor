package edu.icewiz.timny;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.*;
import static java.util.logging.Level.SEVERE;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;

import edu.icewiz.crdt.*;

public class LandingPageController {

    private Scene editingPageScene;
    private EditingPageController editingPageController;
    @FXML
    private Button connectButton;
    @FXML
    private TextField nameField;
    @FXML
    private TextField linkTextArea;

    @FXML
    private Button newButton;
    @FXML
    private Button openButton;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;

    @FXML
    void initialize(){
        progressBar.setVisible(false);
        statusLabel.setVisible(false);
        linkTextArea.setPromptText("Enter Port");
        nameField.setPromptText("Enter Name");
    }

    @FXML
    void connectPort(ActionEvent event) {
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(editingPageScene);
        editingPageController.setMyName(nameField.getText());
        editingPageController.setButtonName("Disconnect");
        editingPageController.connectServer(linkTextArea.getText());

    }

    @FXML
    void newFile(ActionEvent event) {
        editingPageController.setMyName(nameField.getText());
        editingPageController.setButtonName("Shutdown");
        editingPageController.fromStringToEditingServer("", linkTextArea.getText());
        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
        stage.setScene(editingPageScene);
    }

    @FXML
    void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Java file (*.java)", "*.java")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File fileToLoad = fileChooser.showOpenDialog(null);
        if(fileToLoad == null)return;
        loadFileToTextArea(fileToLoad, event);
    }

    //Load file function inspired by tutorial of https://edencoding.com/how-to-open-edit-sync-and-save-a-text-file-in-javafx/
    //But this tutorial does not work
    //Heavily modified to show progressBar correctly, change screen, and pass data to the other screen for editing
    private void loadFileToTextArea(File fileToLoad, ActionEvent event) {
        progressBar.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText("Loading file: " + fileToLoad.getName());
        Task<String> loadTask = fileLoaderTask(fileToLoad, event);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        Thread loadTaskThread = new Thread(loadTask);
        loadTaskThread.start();
    }

    private Task<String> fileLoaderTask(File fileToLoad, ActionEvent event) {
        //Create a task to load the file asynchronously
        Task<String> loadFileTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));
                //Use Files.lines() to calculate total lines - used for progress
                long lineCount;
                try (Stream<String> stream = Files.lines(fileToLoad.toPath())) {
                    lineCount = stream.count();
                }
                //Load in all lines one by one into a StringBuilder separated by "\n" - compatible with TextArea
                String line;
                StringBuilder totalFile = new StringBuilder();
                long linesLoaded = 0;
                while ((line = reader.readLine()) != null) {
                    totalFile.append(line);
                    if(++linesLoaded < lineCount)totalFile.append("\n");
                    updateProgress(linesLoaded, lineCount);
                    //Sleep for 100ms to demonstrate the progressBar updating
                    Thread.sleep(100);
                }
                return totalFile.toString();
            }
        };

        loadFileTask.setOnSucceeded(workerStateEvent -> {
            try {
                Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
                stage.setScene(editingPageScene);
                String tmp = loadFileTask.get();
                //Must set name before creating server
                editingPageController.setMyName(nameField.getText());
                editingPageController.fromStringToEditingServer(tmp, linkTextArea.getText());
                editingPageController.setButtonName("Shutdown");
            } catch (InterruptedException | ExecutionException e) {
                Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                statusLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            }
        });

        loadFileTask.setOnFailed(workerStateEvent -> {
            statusLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
        });
        return loadFileTask;
    }

    public void setEditingPageScene(Scene scene){
        editingPageScene = scene;
    }
    public void setEditingPageController(EditingPageController editingPageController){
        this.editingPageController = editingPageController;
    }
}
