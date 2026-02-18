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

import java.sql.SQLException;
import java.util.List;

public class ForumViewController {

    @FXML private ListView<Post> topicsList;
    @FXML private Label lblTotalTopics;
    @FXML private Label lblTotalReplies;

    private final PostDao postDao = new PostDao();
    private ObservableList<Post> allPosts = FXCollections.observableArrayList();
    private FilteredList<Post> filteredPosts;

    @FXML
    public void initialize() {
        topicsList.setCellFactory(lv -> new TopicCardCell());
        loadPosts();

        // IMPORTANT: This is what opens PostView when clicking on a post
        topicsList.setOnMouseClicked(event -> {
            Post selected = topicsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openSelectedPost();
            }
        });
    }

    private void loadPosts() {
        try {
            List<Post> posts = postDao.getAll();
            allPosts = FXCollections.observableArrayList(posts);

            // Initialize filtered list with all posts
            filteredPosts = new FilteredList<>(allPosts, p -> true);
            topicsList.setItems(filteredPosts);

            updateStats();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void filterByCategory(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String category = clickedButton.getText();

        filteredPosts.setPredicate(post -> {
            // Show all posts if "All" is selected
            if (category.equals("All")) {
                return true;
            }

            // Otherwise filter by the selected category
            return post.getCategory() != null && post.getCategory().equalsIgnoreCase(category);
        });

        updateStats();
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
}