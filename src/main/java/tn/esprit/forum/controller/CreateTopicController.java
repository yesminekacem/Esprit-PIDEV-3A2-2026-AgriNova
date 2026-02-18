package tn.esprit.forum.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.forum.dao.PostDao;
import tn.esprit.forum.entity.Post;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;

import java.sql.SQLException;

public class CreateTopicController {

    @FXML private TextField txtTitle;
    @FXML private TextArea txtContent;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblContentCounter;
    @FXML private Button btnPublish;
    @FXML private Label lblTitleCounter;

    private PostDao postDao;

    @FXML
    private void initialize() {
        try {
            postDao = new PostDao(); // ✅ FIX
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "DB Error", "Cannot initialize PostDao: " + e.getMessage());
            btnPublish.setDisable(true);
            return;
        }

        cbCategory.setItems(FXCollections.observableArrayList(
                "Organic Farming",
                "Soil Management",
                "Water Management",
                "Harvesting",
                "Equipment",
                "Crop Management",
                "General",
                "Testing"
        ));

        cbCategory.setValue("General");

        txtContent.textProperty().addListener((obs, oldV, newV) -> validateContent());
        txtTitle.textProperty().addListener((obs, oldV, newV) -> validateTitle());

        validateTitle();
        validateContent();
    }

    @FXML
    private void onPublish() {
        String title = txtTitle.getText() == null ? "" : txtTitle.getText().trim();
        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        String category = cbCategory.getValue();

        if (title.isEmpty() || content.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Missing fields", "Title and Content are required.");
            return;
        }
        if (category == null) {
            alert(Alert.AlertType.WARNING, "Missing fields", "Please choose a category.");
            return;
        }

        User u = SessionManager.getInstance().getCurrentUser();
        if (u == null) {
            alert(Alert.AlertType.ERROR, "Not logged in", "Please login first.");
            return;
        }

        Post p = new Post();
        p.setTitle(title);
        p.setContent(content);
        p.setCategory(category);
        p.setStatus("ACTIVE");
        p.setAuthor(u.getFullName());
        p.setAuthorId(u.getId());

        try {
            postDao.add(p);
            alert(Alert.AlertType.INFORMATION, "Success", "Topic published!");
            Router.go(Routes.FORUM_LIST);
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "DB Error", e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        Router.go(Routes.FORUM_LIST);
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private boolean validateTitle() {
        String title = txtTitle.getText() == null ? "" : txtTitle.getText().trim();
        int min = 5;

        if (lblTitleCounter != null) {
            lblTitleCounter.setText(title.length() + " / " + min + " characters minimum");
        }

        txtTitle.getStyleClass().removeAll("input-error", "input-valid");

        if (title.length() < min) {
            txtTitle.getStyleClass().add("input-error");
            btnPublish.setDisable(true);
            return false;
        } else {
            txtTitle.getStyleClass().add("input-valid");
            btnPublish.setDisable(false);
            return true;
        }
    }

    private boolean validateContent() {
        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        int min = 20;

        if (lblContentCounter != null) {
            lblContentCounter.setText(content.length() + " / " + min + " characters minimum");
        }

        txtContent.getStyleClass().removeAll("input-error", "input-valid");
        if (lblContentCounter != null) {
            lblContentCounter.getStyleClass().remove("helper-error");
        }

        if (content.length() < min) {
            txtContent.getStyleClass().add("input-error");
            if (lblContentCounter != null) {
                lblContentCounter.getStyleClass().add("helper-error");
            }
            btnPublish.setDisable(true);
            return false;
        } else {
            txtContent.getStyleClass().add("input-valid");
            btnPublish.setDisable(false);
            return true;
        }
    }
}