module com.breakout {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.breakout to javafx.fxml;

    exports com.breakout;
}
