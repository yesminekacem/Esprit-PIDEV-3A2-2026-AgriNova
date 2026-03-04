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
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import tn.esprit.utils.GroqAiService;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.sql.SQLException;
import java.util.List;

public class EditTopicController {

    @FXML private TextField txtTitle;
    @FXML private ComboBox<String> cbCategory;

    @FXML private TextArea txtContent;
    @FXML private Label lblTitleCounter;
    @FXML private Label lblContentCounter;
    @FXML private Label lblPickedImage;
    @FXML private ImageView imgPreview;
    @FXML private ProgressIndicator aiLoading;
    @FXML private Label aiHint;


    private int postId;
    private PostDao postDao;
    private String selectedImagePath;
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
            selectedImagePath = p.getImagePath();

            if (selectedImagePath != null && !selectedImagePath.isBlank()) {
                Path imgFile = Paths.get(System.getProperty("user.dir"), selectedImagePath);
                if (Files.exists(imgFile) && imgPreview != null) {
                    imgPreview.setImage(new Image(imgFile.toUri().toString()));
                    imgPreview.setVisible(true);
                    imgPreview.setManaged(true);
                }
                if (lblPickedImage != null) lblPickedImage.setText(Paths.get(selectedImagePath).getFileName().toString());
            } else {
                if (imgPreview != null) {
                    imgPreview.setImage(null);
                    imgPreview.setVisible(false);
                    imgPreview.setManaged(false);
                }
                if (lblPickedImage != null) lblPickedImage.setText("No image");
            }
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
            existing.setImagePath(selectedImagePath);

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
    @FXML
    private void onPickPostImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose a post image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fc.showOpenDialog(txtTitle.getScene().getWindow());
        if (file == null) return;

        try {
            Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads", "posts");
            Files.createDirectories(uploadsDir);

            String ext = getFileExt(file.getName());
            String newName = "post_" + UUID.randomUUID() + ext;
            Path target = uploadsDir.resolve(newName);

            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            selectedImagePath = Paths.get("uploads", "posts", newName).toString();

            if (lblPickedImage != null) lblPickedImage.setText(file.getName());

            if (imgPreview != null) {
                imgPreview.setImage(new Image(target.toUri().toString()));
                imgPreview.setVisible(true);
                imgPreview.setManaged(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showError("File Error", "Could not save image: " + e.getMessage());
        }
    }

    private String getFileExt(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0) ? name.substring(dot) : "";
    }
    @FXML
    private void onGenerateTitleAndTags() {
        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        if (content.length() < 20) {
            if (aiHint != null) aiHint.setText("Write at least 20 characters so AI can understand your topic.");
            return;
        }

        setAiLoading(true);
        if (aiHint != null) aiHint.setText("Generating...");

        Task<GroqAiService.Suggestion> task = new Task<>() {
            @Override
            protected GroqAiService.Suggestion call() throws Exception {
                return GroqAiService.generateTitleAndTags(content);
            }
        };

        task.setOnSucceeded(e -> {
            setAiLoading(false);
            GroqAiService.Suggestion s = task.getValue();
            if (s == null) {
                if (aiHint != null) aiHint.setText("No suggestion returned.");
                return;
            }

            if (s.title() != null && !s.title().isBlank()) {
                txtTitle.setText(s.title().trim());
                validateTitle();
            }
            if (aiHint != null) {
                aiHint.setText("Suggested tags: " + String.join(", ", s.tags()));
            }
        });

        task.setOnFailed(e -> {
            setAiLoading(false);
            Throwable ex = task.getException();
            if (aiHint != null) aiHint.setText("AI error: " + (ex == null ? "unknown" : ex.getMessage()));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task, "groq-ai-title-tags-edit").start();
    }

    @FXML
    private void onFixGrammar() {
        String content = txtContent.getText() == null ? "" : txtContent.getText().trim();
        if (content.length() < 20) {
            if (aiHint != null) aiHint.setText("Write at least 20 characters so AI can correct properly.");
            return;
        }

        setAiLoading(true);
        if (aiHint != null) aiHint.setText("Correcting grammar...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return GroqAiService.correctGrammar(content);
            }
        };

        task.setOnSucceeded(e -> {
            setAiLoading(false);
            String fixed = task.getValue();
            if (fixed == null || fixed.isBlank()) {
                if (aiHint != null) aiHint.setText("No correction returned.");
                return;
            }

            txtContent.setText(fixed.trim());
            validateContent();

            if (aiHint != null) aiHint.setText("Grammar corrected ✅");
        });

        task.setOnFailed(e -> {
            setAiLoading(false);
            Throwable ex = task.getException();
            if (aiHint != null) aiHint.setText("AI error: " + (ex == null ? "unknown" : ex.getMessage()));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task, "groq-ai-grammar-edit").start();
    }

    private void setAiLoading(boolean on) {
        if (aiLoading != null) {
            aiLoading.setVisible(on);
            aiLoading.setManaged(on);
        }
    }
}
