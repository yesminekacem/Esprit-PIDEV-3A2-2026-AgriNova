package tn.esprit.utils;

import javafx.scene.control.Alert;

public class Alerts {
    public static void error(String title, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    public static void info(String title, String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }
}
