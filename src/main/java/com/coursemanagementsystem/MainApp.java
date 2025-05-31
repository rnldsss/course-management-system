package com.coursemanagementsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/coursemanagementsystem/dashboard.fxml"));
        Scene scene = new Scene(root);
        // CSS dashboard tetap
        scene.getStylesheets().add(getClass().getResource("/com/coursemanagementsystem/tailwindfx.css").toExternalForm());

        primaryStage.setTitle("Sistem Manajemen Tugas Mahasiswa");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}