package tn.esprit.forum.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import tn.esprit.forum.dao.PostDao;
import tn.esprit.forum.entity.Post;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.forum.dao.NotificationDao;
import tn.esprit.forum.entity.Notification;
import tn.esprit.user.entity.User;
import tn.esprit.utils.SessionManager;
import java.sql.SQLException;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.user.entity.Role;
import java.util.HashMap;
import java.util.Map;
import tn.esprit.forum.dao.CommentDao;
public class ForumViewController {

    @FXML private ListView<Post> topicsList;
    @FXML private Label lblTotalTopics;
    @FXML private Label lblTotalReplies;
    @FXML private Label lblNotifBadge;
    @FXML private Button btnNotifications;
    @FXML private TextField txtSearch;
    private String selectedCategory = "All";
    private NotificationDao notificationDao;
    private PostDao postDao;
    private CommentDao commentDao;
    private final Map<Integer, Boolean> flaggedMap = new HashMap<>();

    private ObservableList<Post> allPosts = FXCollections.observableArrayList();
    private FilteredList<Post> filteredPosts;

    @FXML

    public void initialize() {
        try {
            commentDao = new CommentDao();
            postDao = new PostDao();
            notificationDao = new NotificationDao();
        } catch (SQLException e) {
            showError("DB error: " + e.getMessage());
            return;
        }

        topicsList.setCellFactory(lv -> new TopicCardCell(flaggedMap));
        loadPosts();

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        }
        refreshNotificationBadge();

        topicsList.setOnMouseClicked(event -> {
            Post selected = topicsList.getSelectionModel().getSelectedItem();
            if (selected != null) openSelectedPost();
        });
    }

    private void loadPosts() {
        if (postDao == null) return;

        try {
            List<Post> posts = postDao.getAll();
            flaggedMap.clear();
            for (Post p : posts) {
                boolean flagged = commentDao.hasFlaggedComments(p.getIdPost());
                flaggedMap.put(p.getIdPost(), flagged);
            }
            allPosts = FXCollections.observableArrayList(posts);
            filteredPosts = new FilteredList<>(allPosts, p -> true);
            topicsList.setItems(filteredPosts);
            updateStats();
            applyFilters();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void filterByCategory(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        selectedCategory = clickedButton.getText(); // store selected category
        applyFilters();
    }
    private void updateStats() {
        // Update total topics count based on filtered results
        lblTotalTopics.setText(String.valueOf(filteredPosts.size()));

        // For total replies, you'll need to implement a reply count in your Post entity
        // For now, it's set to 0
        lblTotalReplies.setText("0");
    }

    @FXML
    private void onAdd() {
        Router.go(Routes.FORUM_CREATE);
    }

    @FXML
    private void onOpen() {
        openSelectedPost();
    }

    private void openSelectedPost() {
        Post selected = topicsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("No post selected");
            return;
        }

        System.out.println("Opening post: " + selected.getIdPost() + " - " + selected.getTitle());

        try {
            // Navigate to PostView and pass the post ID
            PostViewController controller = Router.goWithController(Routes.FORUM_POST);
            if (controller != null) {
                controller.setPostId(selected.getIdPost());
                controller.loadData();
            } else {
                System.out.println("Controller is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error opening post: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
    private void refreshNotificationBadge() {
        try {
            User u = SessionManager.getInstance().getCurrentUser();
            if (u == null || notificationDao == null) {
                if (lblNotifBadge != null) {
                    lblNotifBadge.setVisible(false);
                    lblNotifBadge.setManaged(false);
                }
                return;
            }

            int unread = notificationDao.countUnread(u.getId());

            if (lblNotifBadge != null) {
                if (unread > 0) {
                    lblNotifBadge.setText(String.valueOf(unread));
                    lblNotifBadge.setVisible(true);
                    lblNotifBadge.setManaged(true);
                } else {
                    lblNotifBadge.setVisible(false);
                    lblNotifBadge.setManaged(false);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onOpenNotifications() {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u == null) {
            showError("Please login first.");
            return;
        }
        if (notificationDao == null) {
            showError("Notifications service not available.");
            return;
        }

        try {
            List<Notification> list = notificationDao.findByRecipient(u.getId());

            // Root
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(14));
            root.setStyle("-fx-background-color: white;");

            // Header
            Label title = new Label("Notifications");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #111827;");

            Label subtitle = new Label("Click an item to open the post.");
            subtitle.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

            VBox header = new VBox(2, title, subtitle);

            // List
            ListView<Notification> lv = new ListView<>();
            lv.setItems(FXCollections.observableArrayList(list));
            lv.setFocusTraversable(false);
            lv.setStyle("-fx-background-insets: 0; -fx-padding: 0;");

            lv.setCellFactory(x -> new ListCell<>() {
                @Override
                protected void updateItem(Notification n, boolean empty) {
                    super.updateItem(n, empty);
                    if (empty || n == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    // unread dot
                    Label dot = new Label("●");
                    dot.setStyle(n.isRead()
                            ? "-fx-text-fill: transparent;"
                            : "-fx-text-fill: #22C55E; -fx-font-size: 10px;");

                    Label msg = new Label(n.getMessage());
                    msg.setWrapText(true);
                    msg.setStyle("-fx-text-fill: #111827; -fx-font-size: 13px; -fx-font-weight: 600;");

                    Label time = new Label(n.getCreatedAt() == null ? "" : n.getCreatedAt().toString().replace('T',' '));
                    time.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");

                    VBox textBox = new VBox(4, msg, time);

                    HBox row = new HBox(10, dot, textBox);
                    row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10));
                    row.setStyle("""
                    -fx-background-color: #F9FAFB;
                    -fx-background-radius: 12;
                    -fx-border-radius: 12;
                    -fx-border-color: #E5E7EB;
                """);

                    setPadding(new javafx.geometry.Insets(6));
                    setGraphic(row);
                }
            });

            // Buttons
            Button markAll = new Button("Mark all read");
            markAll.setStyle("""
            -fx-background-color: #111827;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-padding: 8 14;
            -fx-font-weight: bold;
        """);

            Button close = new Button("Close");
            close.setStyle("""
            -fx-background-color: transparent;
            -fx-border-color: #D1D5DB;
            -fx-text-fill: #111827;
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-padding: 8 14;
            -fx-font-weight: bold;
        """);

            HBox actions = new HBox(10, markAll, new Region(), close);
            HBox.setHgrow(actions.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

            root.getChildren().addAll(header, lv, actions);
            VBox.setVgrow(lv, javafx.scene.layout.Priority.ALWAYS);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Notifications");
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().getButtonTypes().clear(); // we use our own buttons
            dialog.getDialogPane().setPrefSize(520, 520);

            // Close button
            // Close button (reliable)
            close.setOnAction(e -> {
                var window = dialog.getDialogPane().getScene().getWindow();
                if (window != null) window.hide();
            });

            // Mark all read
            markAll.setOnAction(e -> {
                try {
                    notificationDao.markAllAsRead(u.getId());

                    // IMPORTANT: update the ListView items (not only "list")
                    for (Notification n : lv.getItems()) {
                        n.setRead(true);
                    }

                    lv.refresh();
                    refreshNotificationBadge();

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showError("Could not mark all as read: " + ex.getMessage());
                }
            });

            // Open notification (single click marks read + double click opens)
            lv.setOnMouseClicked(e -> {
                Notification selected = lv.getSelectionModel().getSelectedItem();
                if (selected == null) return;

                try {
                    if (!selected.isRead()) {
                        notificationDao.markAsRead(selected.getId());
                        selected.setRead(true);
                        lv.refresh();
                        refreshNotificationBadge();
                    }

                    if (e.getClickCount() == 2) {
                        PostViewController controller = Router.goWithController(Routes.FORUM_POST);
                        if (controller != null) {
                            controller.setPostId(selected.getPostId());
                            controller.loadData();
                        }
                        // Close dialog properly
                        var window = dialog.getDialogPane().getScene().getWindow();
                        if (window != null) {
                            window.hide();
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Could not open notification: " + ex.getMessage());
                }
            });

            dialog.show();
            refreshNotificationBadge();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error loading notifications: " + e.getMessage());
        }
    }
    private void applyFilters() {
        if (filteredPosts == null) return;

        String q = (txtSearch == null || txtSearch.getText() == null) ? "" : txtSearch.getText().trim().toLowerCase();

        filteredPosts.setPredicate(post -> {

            // 1) Category filter
            boolean categoryOk = selectedCategory.equalsIgnoreCase("All")
                    || (post.getCategory() != null && post.getCategory().equalsIgnoreCase(selectedCategory));

            if (!categoryOk) return false;

            // 2) Search filter (title/content/author)
            if (q.isEmpty()) return true;

            String title = post.getTitle() == null ? "" : post.getTitle().toLowerCase();
            String content = post.getContent() == null ? "" : post.getContent().toLowerCase();
            String author = post.getAuthor() == null ? "" : post.getAuthor().toLowerCase();

            return title.contains(q) || content.contains(q) || author.contains(q);
        });

        updateStats();
    }

    @FXML
    private void onClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        applyFilters();
    }


    private boolean isAdmin() {
        User u = SessionManager.getInstance().getCurrentUser();
        return u != null && u.getRole() == Role.ADMIN;
    }

    private boolean isFlagged(Post p) {
        if (p == null) return false;

        String t = p.getTitle() == null ? "" : p.getTitle();
        String c = p.getContent() == null ? "" : p.getContent();

        // detect "***" or longer
        return t.contains("***") || c.contains("***");
    }
}
