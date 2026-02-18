package tn.esprit.forum.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.forum.dao.CommentDao;
import tn.esprit.forum.dao.PostDao;
import tn.esprit.forum.entity.Comment;
import tn.esprit.forum.entity.Post;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;

import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class PostViewController {

    // ====== Header (Figma-like) ======
    @FXML private Label lblTitle;
    @FXML private Label lblPopular;
    @FXML private Label lblCategoryBadge;

    @FXML private Label lblAvatarInitial;
    @FXML private Label lblAuthorName;
    @FXML private Label lblAuthorRole;
    @FXML private Label lblTimeAgo;

    @FXML private Label lblReplies;
    @FXML private Label lblPostLikes;

    // ====== Content card ======
    @FXML private Label lblContent;
    @FXML private Button btnLikePost;

    // ====== Comments ======
    @FXML private Label lblCommentsCount;
    @FXML private ListView<Comment> commentList;
    @FXML private TextArea txtNewComment;

    private int postId;
    private PostDao postDao;

    private final CommentDao commentDao = new CommentDao();

    @FXML
    private void initialize() {
        commentList.setCellFactory(lv -> new CommentMenuCell(this));
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void loadData() {
        loadPost();
        loadComments();
    }

    private void loadPost() {
        try {
            Post p = postDao.getById(postId);
            if (p == null) return;

            lblTitle.setText(p.getTitle());

            String author = (p.getAuthor() == null || p.getAuthor().isBlank()) ? "Unknown" : p.getAuthor();
            lblAuthorName.setText(author);
            lblAvatarInitial.setText(("" + Character.toUpperCase(author.charAt(0))));

            // You don't have role in DB yet -> placeholder
            lblAuthorRole.setText("Farm Manager");

            lblTimeAgo.setText(p.getCreatedAt() != null ? timeAgo(p.getCreatedAt()) : "just now");

            String cat = (p.getCategory() == null || p.getCategory().isBlank()) ? "General" : p.getCategory();
            lblCategoryBadge.setText(cat);

            lblContent.setText(p.getContent());

            // DB doesn't store post likes/replies -> compute replies from comments
            int replies = commentDao.getByPost(postId).size();
            lblReplies.setText(replies + " replies");

            lblPostLikes.setText("0 likes");
            btnLikePost.setText("Like (0)");

            // Popular badge (example rule: 3+ comments)
            boolean popular = replies >= 3;
            lblPopular.setVisible(popular);
            lblPopular.setManaged(popular);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        try {
            var items = FXCollections.observableArrayList(commentDao.getByPost(postId));
            commentList.setItems(items);
            lblCommentsCount.setText("Comments (" + items.size() + ")");
            lblReplies.setText(items.size() + " replies");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddComment() {
        try {
            String text = txtNewComment.getText() == null ? "" : txtNewComment.getText().trim();

            tn.esprit.utils.Validators.requireNotBlank(text, "Comment");
            tn.esprit.utils.Validators.requireLength(text, 2, 1000, "Comment");

            Comment c = new Comment();
            c.setIdPost(postId);
            c.setContent(text);
            c.setAuthor("Current User");
            c.setAuthorId(0);
            c.setLikes(0);

            commentDao.add(c);
            txtNewComment.clear();
            loadComments();

        } catch (IllegalArgumentException ex) {
            tn.esprit.utils.Alerts.error("Validation", "Invalid comment", ex.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            tn.esprit.utils.Alerts.error("DB Error", "Could not add comment", e.getMessage());
        }
    }


    @FXML
    private void onBack() {
        Router.go(Routes.FORUM_LIST);
    }

    @FXML
    private void onEditTopic() {
        EditTopicController ctrl = Router.goWithController(Routes.FORUM_EDIT);
        ctrl.setPostId(postId);
        ctrl.loadData();
    }


    @FXML
    private void onDeleteTopic() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Topic");
        confirm.setHeaderText("Delete this topic?");
        confirm.setContentText("This action cannot be undone.");

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBtn, cancelBtn);

        confirm.showAndWait().ifPresent(result -> {
            if (result == deleteBtn) {
                try {
                    postDao.delete(postId);           // ✅ delete from DB (comments cascade)
                    Router.go(Routes.FORUM_LIST);     // ✅ go back to forum list
                } catch (SQLException e) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error");
                    err.setHeaderText("Could not delete the topic");
                    err.setContentText(e.getMessage());
                    err.show();
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onLikeTopic() {
        // you don’t have post likes in DB -> keep as UI only or remove
    }

    private String timeAgo(LocalDateTime dt) {
        long minutes = Duration.between(dt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " minutes ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        long weeks = days / 7;
        if (weeks < 4) return weeks + " weeks ago";
        long months = days / 30;
        if (months < 12) return months + " months ago";
        long years = days / 365;
        return years + " years ago";
    }
    void deleteComment(int commentId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Comment");
        confirm.setHeaderText("Delete this comment?");
        confirm.setContentText("This action cannot be undone.");

        ButtonType deleteBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteBtn, cancelBtn);

        confirm.showAndWait().ifPresent(result -> {
            if (result == deleteBtn) {
                try {
                    commentDao.delete(commentId);
                    loadComments();
                } catch (SQLException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        });
    }

    void editComment(Comment c) {
        TextInputDialog dialog = new TextInputDialog(c.getContent());
        dialog.setTitle("Edit Comment");
        dialog.setHeaderText("Edit your comment");
        dialog.setContentText("Content:");

        dialog.showAndWait().ifPresent(newText -> {
            if (newText == null || newText.isBlank()) return;
            try {
                c.setContent(newText.trim());
                commentDao.update(c);
                loadComments();
            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            }
        });
    }
    private static class CommentMenuCell extends ListCell<Comment> {

        private final PostViewController parent;

        private final HBox root = new HBox();
        private final VBox body = new VBox(8);

        private final Label lblAuthor = new Label();
        private final Label lblDot = new Label("•");
        private final Label lblTime = new Label();
        private final Label lblContent = new Label();

        private final MenuButton menu = new MenuButton("⋮");
        private final MenuItem editItem = new MenuItem("Edit");
        private final MenuItem deleteItem = new MenuItem("Delete");

        CommentMenuCell(PostViewController parent) {
            this.parent = parent;

            // Basic layout styling
            root.setPadding(new Insets(12));
            root.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #E5E7EB;");

            lblAuthor.setStyle("-fx-font-weight: bold;");
            lblDot.setStyle("-fx-text-fill: #9CA3AF;");
            lblTime.setStyle("-fx-text-fill: #6B7280;");
            lblContent.setWrapText(true);
            lblContent.setStyle("-fx-text-fill: #374151;");

            menu.getItems().addAll(editItem, deleteItem);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox topRow = new HBox(8, lblAuthor, lblDot, lblTime, spacer, menu);

            body.getChildren().addAll(topRow, lblContent);
            root.getChildren().add(body);
        }

        @Override
        protected void updateItem(Comment item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            lblAuthor.setText(item.getAuthor() == null ? "Unknown" : item.getAuthor());
            lblTime.setText(item.getCreatedAt() == null ? "just now" : parent.timeAgo(item.getCreatedAt()));
            lblContent.setText(item.getContent() == null ? "" : item.getContent());

            editItem.setOnAction(e -> parent.editComment(item));
            deleteItem.setOnAction(e -> parent.deleteComment(item.getIdComment()));

            setText(null);
            setGraphic(root);
        }
    }


}
