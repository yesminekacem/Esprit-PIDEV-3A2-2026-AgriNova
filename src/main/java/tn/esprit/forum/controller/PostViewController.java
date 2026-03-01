package tn.esprit.forum.controller;
import tn.esprit.forum.dao.NotificationDao;
import tn.esprit.forum.entity.Notification;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.forum.dao.CommentDao;
import tn.esprit.forum.dao.PostDao;
import tn.esprit.forum.entity.Comment;
import tn.esprit.forum.entity.Post;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;
import tn.esprit.user.entity.User;
import tn.esprit.utils.ProfanityFilterService;
import tn.esprit.utils.SessionManager;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import javafx.stage.Popup;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tn.esprit.forum.entity.ReactionType;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import tn.esprit.user.entity.Role; // add this import
public class PostViewController {

    // ====== Header ======
    @FXML
    private ScrollPane spMain;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblPopular;
    @FXML
    private Label lblCategoryBadge;

    @FXML
    private Label lblAvatarInitial;
    @FXML
    private Label lblAuthorName;
    @FXML
    private Label lblAuthorRole;
    @FXML
    private Label lblTimeAgo;

    @FXML
    private Label lblReplies;
    @FXML
    private Label lblPostLikes;

    // ====== Content card ======
    @FXML
    private Label lblContent;
    @FXML
    private Button btnLikePost;

    // ✅ Menu actions (from your updated PostView.fxml)
    @FXML
    private MenuButton menuPostActions;
    @FXML
    private MenuItem itemEditTopic;
    @FXML
    private MenuItem itemDeleteTopic;

    // ====== Comments ======
    @FXML
    private Label lblCommentsCount;
    @FXML
    private ListView<Comment> commentList;
    @FXML
    private TextArea txtNewComment;
    @FXML
    private ImageView imgPost;
    private int postId;
    private NotificationDao notificationDao;
    private PostDao postDao;
    private CommentDao commentDao;
    private ReactionType myReaction = null;
    private int reactionCount = 0;
    // store loaded post
    private Post currentPost;
    private CommentMenuCell currentlyEditingCell;
    private final Label badgeFlagged = new Label("🚩 FLAGGED");
    private Popup reactionPopup;
    private PauseTransition hidePopupDelay;
    @FXML
    private void initialize() {
        try {
            postDao = new PostDao();
            commentDao = new CommentDao();
            notificationDao = new NotificationDao();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "DB Error: " + e.getMessage()).show();
            return;
        }

        commentList.setCellFactory(lv -> new CommentMenuCell(this));
        setupReactionPopup();
        spMain.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (reactionPopup == null || !reactionPopup.isShowing()) return;

            Object t = e.getTarget();
            if (t instanceof javafx.scene.Node n) {

                // ✅ if click is on Like button => don't hide
                if (isInside(n, btnLikePost)) return;

                // ✅ if click is inside popup => don't hide
                for (var c : reactionPopup.getContent()) {
                    if (isInside(n, c)) return;
                }
            }

            // click outside => close
            reactionPopup.hide();
        });
        // Ctrl + Enter to post new comment
        txtNewComment.setOnKeyPressed(ev -> {
            switch (ev.getCode()) {
                case ENTER -> {
                    if (ev.isControlDown()) {
                        ev.consume();
                        onAddComment();
                    }
                }
            }
        });

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
            currentPost = postDao.getById(postId);
            if (currentPost == null) return;

            Post p = currentPost;

            lblTitle.setText(p.getTitle());

            String author = (p.getAuthor() == null || p.getAuthor().isBlank()) ? "Unknown" : p.getAuthor();
            lblAuthorName.setText(author);
            lblAvatarInitial.setText("" + Character.toUpperCase(author.charAt(0)));

            lblAuthorRole.setText("Farm Manager");
            lblTimeAgo.setText(p.getCreatedAt() != null ? timeAgo(p.getCreatedAt()) : "just now");

            String cat = (p.getCategory() == null || p.getCategory().isBlank()) ? "General" : p.getCategory();
            lblCategoryBadge.setText(cat);

            lblContent.setText(p.getContent());
// ✅ show image if exists
            String path = p.getImagePath();
            if (path != null && !path.isBlank()) {
                File f = new File(path);

                // if stored relative like "uploads/posts/xyz.png"
                if (!f.isAbsolute()) {
                    f = new File(System.getProperty("user.dir"), path);
                }

                if (f.exists()) {
                    imgPost.setImage(new Image(f.toURI().toString()));
                    imgPost.setVisible(true);
                    imgPost.setManaged(true);
                } else {
                    imgPost.setVisible(false);
                    imgPost.setManaged(false);
                }
            } else {
                imgPost.setVisible(false);
                imgPost.setManaged(false);
            }
            int replies = commentDao.getByPost(postId).size();
            lblReplies.setText(replies + " replies");

// ✅ Facebook reactions/likes refresh (count + my reaction + button text)
            refreshPostReactionsUI();
            boolean popular = replies >= 3;
            lblPopular.setVisible(popular);
            lblPopular.setManaged(popular);

            // ✅ Show Edit/Delete menu only for owner
            boolean canEdit = canEdit(p);
            boolean canDelete = canDelete(p);

            boolean showMenu = canEdit || canDelete;
            menuPostActions.setVisible(showMenu);
            menuPostActions.setManaged(showMenu);

// ✅ Admin sees delete only, owner sees both
            itemEditTopic.setVisible(canEdit);
            itemEditTopic.setDisable(!canEdit);

            itemDeleteTopic.setVisible(canDelete);
            itemDeleteTopic.setDisable(!canDelete);

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "DB Error: " + e.getMessage()).show();
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
            new Alert(Alert.AlertType.ERROR, "DB Error: " + e.getMessage()).show();
        }
    }

    @FXML
    private void onAddComment() {
        try {
            String rawText = txtNewComment.getText().trim();
            String text = ProfanityFilterService.clean(rawText);
            tn.esprit.utils.Validators.requireNotBlank(text, "Comment");
            tn.esprit.utils.Validators.requireLength(text, 2, 1000, "Comment");

            User u = SessionManager.getInstance().getCurrentUser();
            if (u == null) {
                new Alert(Alert.AlertType.ERROR, "Please login first").show();
                return;
            }

            Comment c = new Comment();
            c.setIdPost(postId);
            c.setContent(text);
            c.setAuthor(u.getFullName());
            c.setAuthorId(u.getId());
            c.setLikes(0);

            commentDao.add(c);
            // 🔔 Notify post owner (COMMENT)
            if (currentPost != null && u.getId() != currentPost.getAuthorId()) {
                Notification n = new Notification();
                n.setRecipientId(currentPost.getAuthorId());
                n.setActorId(u.getId());
                n.setPostId(postId);
                n.setType("COMMENT");
                n.setMessage(u.getFullName() + " commented on your post: " + currentPost.getTitle());
                n.setRead(false);

                notificationDao.add(n);
            }

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
        if (currentPost == null || !canEdit(currentPost)) {
            new Alert(Alert.AlertType.WARNING, "You can only edit your own post.").show();
            return;
        }

        EditTopicController ctrl = Router.goWithController(Routes.FORUM_EDIT);
        if (ctrl == null) return;

        ctrl.setPostId(postId);
        ctrl.loadData();
    }

    @FXML
    private void onDeleteTopic() {
        if (currentPost == null || !canDelete(currentPost)) {
            new Alert(Alert.AlertType.WARNING, "You are not allowed to delete this post.").show();
            return;
        }

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
                    postDao.delete(postId); // UI is protected already
                    Router.go(Routes.FORUM_LIST);
                } catch (SQLException e) {
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        });
    }


    @FXML
    private void onLikeTopic() {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u == null) {
            new Alert(Alert.AlertType.ERROR, "Please login first").show();
            return;
        }

        try {
            ReactionType current = postDao.getUserReaction(postId, u.getId());

            if (current == ReactionType.LIKE) {
                postDao.removeReaction(postId, u.getId());     // unlike
            } else {
                postDao.setReaction(postId, u.getId(), ReactionType.LIKE); // default like
            }

            refreshPostReactionsUI();

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }
    // ✅ owner check for posts
    private boolean isOwner(Post p) {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && p != null && p.getAuthorId() == u.getId();
    }

    // ✅ owner check for comments
    private boolean isCommentOwner(Comment c) {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && c != null && c.getAuthorId() == u.getId();
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

    // ========= Comment cell =========

    private class CommentMenuCell extends ListCell<Comment> {
        private final PostViewController parent;

        private final HBox root = new HBox();
        private final VBox body = new VBox(8);

        private final Label lblAuthor = new Label();
        private final Label lblDot = new Label("•");
        private final Label lblTime = new Label();
        // avatar (initial)
        private final StackPane avatar = new StackPane();
        private final Label lblInitial = new Label();

        // view vs edit
        private final StackPane contentPane = new StackPane();
        private final Label lblContent = new Label();
        private final TextArea txtEdit = new TextArea();

        // save/cancel row
        private final HBox editActions = new HBox(10);
        private final Button btnSave = new Button("Save");
        private final Button btnCancel = new Button("Cancel");

        private final MenuButton menu = new MenuButton("⋮");
        private final MenuItem editItem = new MenuItem("Edit");
        private final MenuItem deleteItem = new MenuItem("Delete");

        private boolean editing = false;

        CommentMenuCell(PostViewController parent) {
            this.parent = parent;

            // ===== Root row (avatar + bubble) =====
            root.setSpacing(12);
            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(10, 8, 10, 8));
            root.setStyle("-fx-background-color: transparent;");

            // ===== Avatar (FB style) =====
            avatar.setMinSize(40, 40);
            avatar.setMaxSize(40, 40);
            avatar.setStyle("""
        -fx-background-color: #dcfce7;
        -fx-border-color: rgba(0,0,0,0.10);
        -fx-border-radius: 999;
        -fx-background-radius: 999;
        -fx-alignment: center;
    """);

            lblInitial.setStyle("""
        -fx-font-size: 14px;
        -fx-font-weight: 900;
        -fx-text-fill: #166534;
    """);
            avatar.getChildren().setAll(lblInitial);

            // ===== Bubble card =====
            body.setSpacing(8);
            body.setPadding(new Insets(12, 12, 10, 12));
            body.setStyle("""
        -fx-background-color: white;
        -fx-background-radius: 16;
        -fx-border-radius: 16;
        -fx-border-color: rgba(0,0,0,0.10);
    """);

            // hover lift (small but nice)
            body.setOnMouseEntered(e -> body.setTranslateY(-2));
            body.setOnMouseExited(e -> body.setTranslateY(0));

            // ===== Top row (name • time + menu) =====
            lblAuthor.setStyle("""
        -fx-font-weight: 900;
        -fx-text-fill: #111827;
        -fx-font-size: 13px;
    """);

            lblDot.setStyle("-fx-text-fill: #d1d5db;");
            lblTime.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");

            // Menu button: modern round icon (not default grey)
            menu.setText("⋯");
            menu.setStyle("""
        -fx-background-color: rgba(17,24,39,0.06);
        -fx-background-radius: 999;
        -fx-border-radius: 999;
        -fx-padding: 4 12;
        -fx-font-size: 16px;
        -fx-cursor: hand;
    """);
            menu.setOnMouseEntered(e -> menu.setStyle("""
        -fx-background-color: rgba(37,99,235,0.12);
        -fx-background-radius: 999;
        -fx-border-radius: 999;
        -fx-padding: 4 12;
        -fx-font-size: 16px;
        -fx-cursor: hand;
    """));
            menu.setOnMouseExited(e -> menu.setStyle("""
        -fx-background-color: rgba(17,24,39,0.06);
        -fx-background-radius: 999;
        -fx-border-radius: 999;
        -fx-padding: 4 12;
        -fx-font-size: 16px;
        -fx-cursor: hand;
    """));

            // Make the dropdown items feel modern (menu items)
            editItem.setStyle("-fx-font-weight: 800;");
            deleteItem.setStyle("-fx-font-weight: 800;");

            menu.getItems().setAll(editItem, deleteItem);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox topRow = new HBox(10);
            topRow.setAlignment(Pos.CENTER_LEFT);
            topRow.getChildren().setAll(lblAuthor, lblDot, lblTime, spacer, menu);

            // ===== Content view =====
            lblContent.setWrapText(true);
            lblContent.setStyle("""
        -fx-text-fill: #374151;
        -fx-font-size: 13px;
        -fx-line-spacing: 2;
    """);

            // Edit box (soft, FB-like)
            txtEdit.setWrapText(true);
            txtEdit.setPrefRowCount(3);
            txtEdit.setStyle("""
        -fx-background-color: white;
        -fx-text-fill: #111827;
        -fx-background-radius: 14;
        -fx-border-radius: 14;
        -fx-border-color: rgba(0,0,0,0.12);
        -fx-padding: 10;
        -fx-highlight-fill: rgba(34,197,94,0.35);
        -fx-highlight-text-fill: #111827;
    """);

            contentPane.getChildren().setAll(lblContent, txtEdit);

            // ===== Edit actions (Cancel/Save like FB) =====
            editActions.setAlignment(Pos.CENTER_RIGHT);
            editActions.setSpacing(10);

            btnCancel.setText("Cancel");
            btnCancel.setStyle("""
        -fx-background-color: rgba(17,24,39,0.06);
        -fx-text-fill: #111827;
        -fx-font-weight: 900;
        -fx-background-radius: 999;
        -fx-padding: 8 14;
        -fx-cursor: hand;
    """);

            btnSave.setText("Save");
            btnSave.setStyle("""
        -fx-background-color: linear-gradient(to bottom, #22c55e, #16a34a);
        -fx-text-fill: white;
        -fx-font-weight: 900;
        -fx-background-radius: 999;
        -fx-padding: 8 16;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0, 0, 4);
    """);

            // tiny press feel
            btnSave.setOnMousePressed(e -> { btnSave.setScaleX(0.98); btnSave.setScaleY(0.98); });
            btnSave.setOnMouseReleased(e -> { btnSave.setScaleX(1.0); btnSave.setScaleY(1.0); });
            btnCancel.setOnMousePressed(e -> { btnCancel.setScaleX(0.98); btnCancel.setScaleY(0.98); });
            btnCancel.setOnMouseReleased(e -> { btnCancel.setScaleX(1.0); btnCancel.setScaleY(1.0); });

            editActions.getChildren().setAll(btnCancel, btnSave);
            editActions.setVisible(false);
            editActions.setManaged(false);

            // ===== Build body =====
            body.getChildren().setAll(topRow, contentPane, editActions);

            // ===== Final layout: avatar + bubble =====
            root.getChildren().setAll(avatar, body);
            HBox.setHgrow(body, Priority.ALWAYS);

            // keyboard: ESC cancel, CTRL+ENTER save
            txtEdit.setOnKeyPressed(ev -> {
                switch (ev.getCode()) {
                    case ESCAPE -> {
                        ev.consume();
                        exitEditMode(true);
                    }
                    case ENTER -> {
                        if (ev.isControlDown()) {
                            ev.consume();
                            saveEdit();
                        }
                    }
                }
            });

            btnCancel.setOnAction(e -> exitEditMode(true));
            btnSave.setOnAction(e -> saveEdit());

            editItem.setOnAction(e -> enterEditMode());
            deleteItem.setOnAction(e -> {
                Comment c = getItem();
                if (c != null) parent.deleteComment(c.getIdComment());
            });
        }

        @Override
        protected void updateItem(Comment item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            // ===== Basic data =====
            String author = (item.getAuthor() == null || item.getAuthor().isBlank())
                    ? "Unknown"
                    : item.getAuthor().trim();

            lblAuthor.setText(author);

            String initial = author.isBlank() ? "?" : ("" + Character.toUpperCase(author.charAt(0)));
            lblInitial.setText(initial);

            lblTime.setText(item.getCreatedAt() == null ? "just now" : parent.timeAgo(item.getCreatedAt()));
            String contentText = (item.getContent() == null) ? "" : item.getContent();
            lblContent.setText(contentText);

            // ===== FLAGGED badge (admin only + contains ***) =====
            boolean flagged = parent.isFlaggedText(contentText);
            boolean showFlag = parent.isAdmin() && flagged;

            badgeFlagged.setVisible(showFlag);
            badgeFlagged.setManaged(showFlag);

            // Optional: highlight bubble if flagged (admin only)
            if (showFlag) {
                body.setStyle("""
            -fx-background-color: rgba(239,68,68,0.06);
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: rgba(239,68,68,0.25);
        """);
            } else {
                body.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-border-radius: 16;
            -fx-border-color: rgba(0,0,0,0.10);
        """);
            }

            // ===== Permissions =====
            boolean canEdit = parent.canEditComment(item);      // only owner
            boolean canDelete = parent.canDeleteComment(item);  // owner OR admin

            boolean showMenu = canEdit || canDelete;
            menu.setVisible(showMenu);
            menu.setManaged(showMenu);

            // show/hide menu items
            editItem.setVisible(canEdit);
            editItem.setDisable(!canEdit);

            deleteItem.setVisible(canDelete);
            deleteItem.setDisable(!canDelete);

            // safety: if user cannot edit, ensure we exit edit mode
            if (!canEdit && editing) exitEditMode(true);

            setEditUIVisible(editing);

            setText(null);
            setGraphic(root);
        }

        private void enterEditMode() {
            Comment c = getItem();
            if (c == null) return;

            // ✅ Only one comment editing at a time (Facebook style)
            if (parent.currentlyEditingCell != null && parent.currentlyEditingCell != this) {
                parent.currentlyEditingCell.exitEditMode(true);
            }
            parent.currentlyEditingCell = this;

            editing = true;
            txtEdit.setText(c.getContent() == null ? "" : c.getContent());
            setEditUIVisible(true);

            txtEdit.requestFocus();
            txtEdit.selectAll();
        }

        private void exitEditMode(boolean discard) {
            editing = false;

            if (parent.currentlyEditingCell == this) parent.currentlyEditingCell = null;

            if (discard) {
                Comment c = getItem();
                if (c != null) lblContent.setText(c.getContent() == null ? "" : c.getContent());
            }

            setEditUIVisible(false);
        }

        private void saveEdit() {
            Comment c = getItem();
            if (c == null) return;

            String newText = txtEdit.getText() == null ? "" : txtEdit.getText().trim();
            if (newText.isBlank()) return;

            try {
                c.setContent(newText);
                parent.commentDao.update(c); // updates DB

                lblContent.setText(newText);
                exitEditMode(false);

                // refresh list (optional but keeps UI consistent)
                parent.loadComments();

            } catch (SQLException e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
            }
        }

        private void setEditUIVisible(boolean isEditing) {
            lblContent.setVisible(!isEditing);
            lblContent.setManaged(!isEditing);

            txtEdit.setVisible(isEditing);
            txtEdit.setManaged(isEditing);

            editActions.setVisible(isEditing);
            editActions.setManaged(isEditing);

            menu.setDisable(isEditing);
            menu.setOpacity(isEditing ? 0.4 : 1.0);
        }
    }

    void deleteComment(int commentId) {
        // ✅ security check (not only UI)
        Comment target = null;
        for (Comment c : commentList.getItems()) {
            if (c.getIdComment() == commentId) {
                target = c;
                break;
            }
        }

        if (target == null || !canDeleteComment(target)) {
            new Alert(Alert.AlertType.WARNING, "You are not allowed to delete this comment.").show();
            return;
        }

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
    @FXML
    private void onClearComment() {
        txtNewComment.clear();
    }
    @FXML
    private void scrollToComposer() {
        // simple + reliable: focus the text area
        txtNewComment.requestFocus();

        // move scroll down near bottom (good enough for most UIs)
        if (spMain != null) {
            spMain.setVvalue(1.0);
        }
    }
    private void setupReactionPopup() {

        reactionPopup = new Popup();
        reactionPopup.setAutoHide(false);
        reactionPopup.setAutoFix(true);

        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER);
        bar.setPickOnBounds(true);
        bar.getStyleClass().add("reaction-bar");

        for (ReactionType t : ReactionType.values()) {

            String fileName = t.name().toLowerCase() + ".png";

            Image img = new Image(
                    getClass().getResource("/images/reactions/" + fileName).toExternalForm()
            );

            // ✅ 1) Force same icon size
            ImageView icon = new ImageView(img);
            icon.setFitWidth(28);
            icon.setFitHeight(28);
            icon.setPreserveRatio(true);
            icon.setSmooth(true);

            // ✅ 2) Force same button size (so icons align)
            Button btn = new Button();
            btn.setGraphic(icon);
            btn.setText(null);
            btn.setFocusTraversable(false);

            btn.setMinSize(42, 42);
            btn.setPrefSize(42, 42);
            btn.setMaxSize(42, 42);

            // ✅ 3) Remove extra padding/background that makes some look bigger
            btn.setStyle("""
            -fx-background-color: transparent;
            -fx-padding: 0;
            -fx-background-radius: 999;
            -fx-cursor: hand;
        """);

            // Click
            btn.setOnAction(e -> {
                applyReaction(t);
                reactionPopup.hide();
            });

            // Hover animation (still works)
            btn.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), icon);
                st.setToX(1.35);
                st.setToY(1.35);
                st.play();
            });

            btn.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(javafx.util.Duration.millis(120), icon);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });

            bar.getChildren().add(btn);
        }

        reactionPopup.getContent().add(bar);

        hidePopupDelay = new PauseTransition(javafx.util.Duration.millis(600));
        hidePopupDelay.setOnFinished(e -> reactionPopup.hide());

        btnLikePost.setOnMouseEntered(e -> {
            hidePopupDelay.stop();
            showReactionPopup();
        });

        btnLikePost.setOnMouseExited(e -> hidePopupDelay.playFromStart());
        bar.setOnMouseEntered(e -> hidePopupDelay.stop());
        bar.setOnMouseExited(e -> hidePopupDelay.playFromStart());
    }
    private void showReactionPopup() {
        if (reactionPopup.isShowing()) return;

        // show above the Like button
        var p = btnLikePost.localToScreen(0, 0);
        if (p == null) return;

        double x = p.getX() + 8;
        double y = p.getY() - 55; // above button

        reactionPopup.show(btnLikePost, x, y);
    }

    private void scheduleHidePopup() {
        hidePopupDelay.playFromStart();
    }
    private void applyReaction(ReactionType reaction) {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u == null) {
            new Alert(Alert.AlertType.ERROR, "Please login first").show();
            return;
        }

        try {
            postDao.setReaction(postId, u.getId(), reaction);
            // 🔔 Notify post owner (LIKE/REACTION)
            if (currentPost != null && u.getId() != currentPost.getAuthorId()) {
                Notification n = new Notification();
                n.setRecipientId(currentPost.getAuthorId());
                n.setActorId(u.getId());
                n.setPostId(postId);
                n.setType("LIKE");
                n.setMessage(u.getFullName() + " reacted (" + reaction.name() + ") to your post: " + currentPost.getTitle());
                n.setRead(false);

                notificationDao.add(n);
            }
            refreshPostReactionsUI();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Reaction error: " + e.getMessage()).show();
        }
        refreshPostReactionsUI();
    }


    private boolean isInside(javafx.scene.Node child, javafx.scene.Node parent) {
        javafx.scene.Node n = child;
        while (n != null) {
            if (n == parent) return true;
            n = n.getParent();
        }
        return false;
    }
    private void setLikeButtonUI(ReactionType reaction, int total) {
        // Build: [icon] Label (count)
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);

        if (reaction != null) {
            String fileName = reaction.name().toLowerCase() + ".png";
            Image img = new Image(getClass().getResource("/images/reactions/" + fileName).toExternalForm());
            ImageView iv = new ImageView(img);
            iv.setFitWidth(18);
            iv.setFitHeight(18);
            iv.setPreserveRatio(true);

            box.getChildren().add(iv);
        }

        String label = (reaction == null) ? "Like" : reaction.displayName; // we’ll set displayName in enum
        Label txt = new Label(label + " (" + total + ")");
        txt.getStyleClass().add("like-btn-text");

        box.getChildren().add(txt);

        btnLikePost.setText(null);
        btnLikePost.setGraphic(box);

        // style on/off
        btnLikePost.getStyleClass().remove("liked");
        if (reaction != null) btnLikePost.getStyleClass().add("liked");
    }
    private void refreshPostReactionsUI() {
        try {
            User u = SessionManager.getInstance().getCurrentUser();

            int total = postDao.countAllReactions(postId);
            lblPostLikes.setText(total + " reactions");

            ReactionType myReaction = null;
            if (u != null) {
                myReaction = postDao.getUserReaction(postId, u.getId());
            }

            setLikeButtonUI(myReaction, total);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private boolean isAdmin() {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && u.getRole() == Role.ADMIN;
    }

    private boolean canEdit(Post p) {
        return isOwner(p);              // only owner edits
    }

    private boolean canDelete(Post p) {
        return isOwner(p) || isAdmin(); // owner OR admin deletes
    }
    private boolean canEditComment(Comment c) {
        return isCommentOwner(c);              // only owner edits
    }

    private boolean canDeleteComment(Comment c) {
        return isCommentOwner(c) || isAdmin(); // owner OR admin deletes
    }
    private boolean isFlaggedText(String text) {
        return text != null && text.contains("***");
    }

}