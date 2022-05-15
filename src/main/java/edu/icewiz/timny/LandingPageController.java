package edu.icewiz.timny;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import javax.swing.plaf.basic.BasicSliderUI;
import java.io.*;
import static java.util.logging.Level.SEVERE;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;

public class LandingPageController {

    private Scene editingPageScene;
    @FXML
    private Button connectButton;

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
        linkTextArea.setText("9990");
    }

    @FXML
    void connectPort(ActionEvent event) {

    }

    @FXML
    void newFile(ActionEvent event) {

    }

    @FXML
    void openFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File fileToLoad = fileChooser.showOpenDialog(null);
        if(fileToLoad == null)return;
        progressBar.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText("Loading file: " + fileToLoad.getName());
        loadFileToTextArea(fileToLoad);
//        Stage stage = (Stage) ((Node)event.getSource()).getScene().getWindow();
//        stage.setScene(editingPageScene);
    }

    private void loadFileToTextArea(File fileToLoad) {
        Task<String> loadTask = fileLoaderTask(fileToLoad);
        progressBar.progressProperty().bind(loadTask.progressProperty());
        Thread loadTaskThread = new Thread(loadTask);
        loadTaskThread.start();
    }

    private Task<String> fileLoaderTask(File fileToLoad) {
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
                    totalFile.append("\n");
                    updateProgress(++linesLoaded, lineCount);
                    Thread.sleep(200);
                }
                return totalFile.toString();
            }
        };
        //If successful, update the text area, display a success message and store the loaded file reference
        loadFileTask.setOnSucceeded(workerStateEvent -> {
            try {
                String tmp = loadFileTask.get();
                System.out.println(tmp);
                statusLabel.setText("File loaded: " + fileToLoad.getName());
            } catch (InterruptedException | ExecutionException e) {
                Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
                statusLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
            }
        });
        //If unsuccessful, set text area with error message and status message to failed
        loadFileTask.setOnFailed(workerStateEvent -> {
            statusLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
        });
        return loadFileTask;
    }

    public void setEditingPageScene(Scene scene){
        editingPageScene = scene;
    }
}