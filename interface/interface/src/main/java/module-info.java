module org.example.appinterface {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires java.sql;

    opens org.example.appinterface to javafx.fxml;
    opens org.example.appinterface.model to javafx.base;

    exports org.example.appinterface;
}
