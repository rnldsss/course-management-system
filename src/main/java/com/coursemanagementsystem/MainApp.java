package com.coursemanagementsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/dashboard.fxml"));
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/style/tailwindfx.css").toExternalForm());
        stage.setTitle("Manajemen Tugas Mahasiswa");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}