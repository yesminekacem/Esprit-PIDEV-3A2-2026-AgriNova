package tn.esprit.forum.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.forum.dao.PostDao;
import tn.esprit.forum.entity.Post;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;

import java.sql.SQLException;
import java.util.List;

public class EditTopicController {

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea txtContent;
    @FXML private Label lblTitleCounter;
    @FXML private Label lblContentCounter;

    private int postId;
    private final PostDao postDao = new PostDao();

    @FXML
    private void initialize() {
        cbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "ARCHIVED"));
        cbCategory.setItems(FXCollections.observableArrayList(
                List.of("Organic Farming", "Soil Management", "Water Management", "Harvesting", "Equipment", "Testing", "General")
        ));

        txtTitle.textProperty().addListener((obs, o, n) -> validateTitle());
        txtContent.textProperty().addListener((obs, o, n) -> validateContent());

        // run once so labels show correct values when screen opens
        validateTitle();
        validateContent();
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void loadData() {
        try {
            Post p = postDao.getById(postId);
            if (p == null) return;

            txtTitle.setText(p.getTitle());
            txtContent.setText(p.getContent());

            cbCategory.setValue(p.getCategory() == null ? "General" : p.getCategory());
            cbStatus.setValue(p.getStatus() == null ? "ACTIVE" : p.getStatus());

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Could not load topic", e.getMessage());
        }
    }
    @FXML
    private void onSave() {
        if (!validateTitle() | !validateContent()) {
            showError("Validation error", "Please fix the highlighted fields.");
            return;
        }

        // 1) Normalize inputs
        String title = (txtTitle.getText() == null) ? "" : txtTitle.getText().trim().replaceAll("\\s+", " ");
        String content = (txtContent.getText() == null) ? "" : txtContent.getText().trim();
        String category = cbCategory.getValue();
        String status = cbStatus.getValue();

        // 2) Validation rules
        if (title.isEmpty()) {
            showError("Validation error", "Title is required.");
            return;
        }
        if (title.length() < 5) {
            showError("Validation error", "Title must be at least 5 characters.");
            return;
        }
        if (title.length() > 200) {
            showError("Validation error", "Title must be at most 200 characters.");
            return;
        }

        if (content.isEmpty()) {
            showError("Validation error", "Content is required.");
            return;
        }
        if (content.length() < 10) {
            showError("Validation error", "Content must be at least 10 characters.");
            return;
        }
        if (content.length() > 5000) {
            showError("Validation error", "Content must be at most 5000 characters.");
            return;
        }

        // Category: if user didn’t choose, set a safe default
        if (category == null || category.isBlank()) {
            category = "General";
        }

        // Status: must be one of these
        if (status == null || (!status.equals("ACTIVE") && !status.equals("ARCHIVED"))) {
            status = "ACTIVE";
        }

        // 3) Save
        try {
            Post existing = postDao.getById(postId);
            if (existing == null) {
                showError("Not found", "This topic no longer exists.");
                return;
            }

            existing.setTitle(title);
            existing.setContent(content);
            existing.setCategory(category);
            existing.setStatus(status);

            postDao.update(existing);

            PostViewController ctrl = Router.goWithController(Routes.FORUM_POST);
            ctrl.setPostId(postId);
            ctrl.loadData();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Could not save changes", e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        PostViewController ctrl = Router.goWithController(Routes.FORUM_POST);
        ctrl.setPostId(postId);
        ctrl.loadData();
    }


    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    private boolean validateTitle() {
        String title = txtTitle.getText() == null ? "" : txtTitle.getText().trim().replaceAll("\\s+", " ");
        int len = title.length();
        int min = 5;

        lblTitleCounter.setText(len + " / " + min + " characters minimum");

        txtTitle.getStyleClass().removeAll("input-error", "input-valid");
        lblTitleCounter.getStyleClass().remove("helper-error");

        if (len == 0) return false;

        if (len < min) {
            txtTitle.getStyleClass().add("input-error");
            lblTitleCounter.getStyleClass().add("helper-error");
            return false;
        } else {
            txtTitle.getStyleClass().add("input-valid");
            return true;
        }
    }

    private boolean validateContent() {
        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        int len = content.length();
        int min = 20;

        lblContentCounter.setText(len + " / " + min + " characters minimum");

        txtContent.getStyleClass().removeAll("input-error", "input-valid");
        lblContentCounter.getStyleClass().remove("helper-error");

        if (len == 0) return false;

        if (len < min) {
            txtContent.getStyleClass().add("input-error");
            lblContentCounter.getStyleClass().add("helper-error");
            return false;
        } else {
            txtContent.getStyleClass().add("input-valid");
            return true;
        }
    }


}
