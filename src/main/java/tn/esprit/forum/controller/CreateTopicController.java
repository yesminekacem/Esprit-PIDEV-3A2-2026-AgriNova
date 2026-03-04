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
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.sql.SQLException;
import tn.esprit.utils.GroqAiService;

public class CreateTopicController {

    @FXML private TextField txtTitle;
    @FXML private TextArea txtContent;
    @FXML private ComboBox<String> cbCategory;
    @FXML private Label lblContentCounter;
    @FXML private Button btnPublish;
    @FXML private Label lblTitleCounter;
    @FXML private Label lblPickedImage;
    @FXML private ImageView imgPreview;
    @FXML private ProgressIndicator aiLoading;
    @FXML private Label aiHint;
    private PostDao postDao;
    private String selectedImagePath;
    @FXML
    private void initialize() {
        System.out.println("GROQ KEY = " + System.getenv("GROQ_API_KEY"));
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
        p.setImagePath(selectedImagePath); // can be null if no image selected

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
            // 1) Ensure uploads folder exists: <project>/uploads/posts
            Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads", "posts");
            Files.createDirectories(uploadsDir);

            // 2) Copy the file with a unique name
            String ext = getFileExt(file.getName());
            String newName = "post_" + UUID.randomUUID() + ext;
            Path target = uploadsDir.resolve(newName);

            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            // 3) Store RELATIVE path in DB (portable)
            selectedImagePath = Paths.get("uploads", "posts", newName).toString();

            // UI feedback
            if (lblPickedImage != null) lblPickedImage.setText(file.getName());

            if (imgPreview != null) {
                imgPreview.setImage(new Image(target.toUri().toString()));
                imgPreview.setVisible(true);
                imgPreview.setManaged(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "File Error", "Could not save image: " + e.getMessage());
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

        new Thread(task, "groq-ai-title-tags").start();
    }

    private void setAiLoading(boolean on) {
        if (aiLoading != null) {
            aiLoading.setVisible(on);
            aiLoading.setManaged(on);
        }
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

            // Replace content with corrected version
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

        new Thread(task, "groq-ai-grammar").start();
    }

}
