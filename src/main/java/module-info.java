module com.coursemanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.coursemanagementsystem to javafx.fxml;
    exports com.coursemanagementsystem;
}
