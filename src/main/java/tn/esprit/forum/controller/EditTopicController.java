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
import java.util.List;

public class EditTopicController {

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cbCategory;

    @FXML private TextArea txtContent;
    @FXML private Label lblTitleCounter;
    @FXML private Label lblContentCounter;

    private int postId;
    private PostDao postDao;

    @FXML
    private void initialize() {
        // ✅ INIT DAO (MOST IMPORTANT FIX)
        try {
            postDao = new PostDao();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("DB Error", "Cannot initialize PostDao: " + e.getMessage());
            return;
        }



        cbCategory.setItems(FXCollections.observableArrayList(
                "Organic Farming",
                "Soil Management",
                "Water Management",
                "Harvesting",
                "Equipment",
                "Testing",
                "General"
        ));

        txtTitle.textProperty().addListener((obs, o, n) -> validateTitle());
        txtContent.textProperty().addListener((obs, o, n) -> validateContent());

        validateTitle();
        validateContent();
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }


    public void loadData() {
        if (postDao == null) {
            showError("DB Error", "PostDao is not initialized.");
            return;
        }

        if (postId <= 0) {
            showError("Error", "Invalid post ID.");
            return;
        }

        try {
            Post p = postDao.getById(postId);
            User u = SessionManager.getInstance().getCurrentUser();
            if (u == null || p.getAuthorId() != u.getId()) {
                showError("Access denied", "You can only edit your own post.");
                Router.go(Routes.FORUM_LIST);
                return;
            }
            if (p == null) {
                showError("Not found", "This topic no longer exists.");
                return;
            }

            txtTitle.setText(p.getTitle());
            txtContent.setText(p.getContent());

            cbCategory.setValue((p.getCategory() == null || p.getCategory().isBlank()) ? "General" : p.getCategory());

            validateTitle();
            validateContent();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Could not load topic", e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        // use || not |
        if (!validateTitle() || !validateContent()) {
            showError("Validation error", "Please fix the highlighted fields.");
            return;
        }

        String title = (txtTitle.getText() == null) ? "" : txtTitle.getText().trim().replaceAll("\\s+", " ");
        String content = (txtContent.getText() == null) ? "" : txtContent.getText().trim();
        String category = cbCategory.getValue();

        if (category == null || category.isBlank()) category = "General";

        try {
            Post existing = postDao.getById(postId);
            User u = SessionManager.getInstance().getCurrentUser();
            if (u == null || existing.getAuthorId() != u.getId()) {
                showError("Access denied", "You can only edit your own post.");
                Router.go(Routes.FORUM_LIST);
                return;
            }
            if (existing == null) {
                showError("Not found", "This topic no longer exists.");
                return;
            }

            existing.setTitle(title);
            existing.setContent(content);
            existing.setCategory(category);

            postDao.update(existing);

            // Go back to the Post View (same post)
            PostViewController ctrl = Router.goWithController(Routes.FORUM_POST);
            if (ctrl != null) {
                ctrl.setPostId(postId);
                ctrl.loadData();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Could not save changes", e.getMessage());
        }
    }

    @FXML
    private void onBack() {
        PostViewController ctrl = Router.goWithController(Routes.FORUM_POST);
        if (ctrl != null) {
            ctrl.setPostId(postId);
            ctrl.loadData();
        }
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(header);
        a.setContentText(msg);
        a.show();
    }

    private boolean validateTitle() {
        if (txtTitle == null) return false;

        String title = txtTitle.getText() == null ? "" : txtTitle.getText().trim().replaceAll("\\s+", " ");
        int len = title.length();
        int min = 5;

        if (lblTitleCounter != null) {
            lblTitleCounter.setText(len + " / " + min + " characters minimum");
            lblTitleCounter.getStyleClass().remove("helper-error");
        }

        txtTitle.getStyleClass().removeAll("input-error", "input-valid");

        if (len < min) {
            txtTitle.getStyleClass().add("input-error");
            if (lblTitleCounter != null) lblTitleCounter.getStyleClass().add("helper-error");
            return false;
        } else {
            txtTitle.getStyleClass().add("input-valid");
            return true;
        }
    }

    private boolean validateContent() {
        if (txtContent == null) return false;

        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        int len = content.length();
        int min = 20;

        if (lblContentCounter != null) {
            lblContentCounter.setText(len + " / " + min + " characters minimum");
            lblContentCounter.getStyleClass().remove("helper-error");
        }

        txtContent.getStyleClass().removeAll("input-error", "input-valid");

        if (len < min) {
            txtContent.getStyleClass().add("input-error");
            if (lblContentCounter != null) lblContentCounter.getStyleClass().add("helper-error");
            return false;
        } else {
            txtContent.getStyleClass().add("input-valid");
            return true;
        }
    }
}
