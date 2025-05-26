module com.coursemanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive java.sql;
    requires transitive javafx.graphics;

    opens com.coursemanagementsystem to javafx.fxml;
    exports com.coursemanagementsystem;
    exports com.coursemanagementsystem.controller;
    opens com.coursemanagementsystem.controller to javafx.fxml;
    exports com.coursemanagementsystem.model;
    opens com.coursemanagementsystem.model to javafx.fxml;
    opens com.coursemanagementsystem.database to javafx.fxml;
    exports com.coursemanagementsystem.database;
}