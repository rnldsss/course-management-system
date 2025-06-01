package com.coursemanagementsystem;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coursemanagementsystem/dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Load final CSS
            URL cssResource = getClass().getResource("/com/coursemanagementsystem/style.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
                System.out.println("✅ CSS berhasil dimuat: " + cssResource.toExternalForm());
            } else {
                System.out.println("❌ CSS tidak ditemukan di path: /com/coursemanagementsystem/style.css");
            }

            // Setup stage
            primaryStage.setTitle("Sistem Manajemen Tugas Mahasiswa");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            System.out.println("❌ Error saat memuat aplikasi:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
