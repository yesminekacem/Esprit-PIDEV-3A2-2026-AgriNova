package tn.esprit.user.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.IOException;
import java.util.List;

public class AdminDashboardController {
    @FXML private Label adminNameLabel;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, Role> roleColumn;
    @FXML private Button refreshButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button logoutButton;

    private UserCrud userCrud = new UserCrud();
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        adminNameLabel.setText(SessionManager.getInstance().getCurrentUserName());
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Custom cell factory for role badges
        roleColumn.setCellFactory(column -> new TableCell<User, Role>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(role.toString());
                    if (role == Role.ADMIN) {
                        badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; " +
                                "-fx-padding: 4 12; -fx-background-radius: 12; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; " +
                                "-fx-padding: 4 12; -fx-background-radius: 12; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        usersTable.setItems(usersList);
    }

    private void loadUsers() {
        try {
            usersList.clear();
            List<User> users = userCrud.findAll();
            usersList.addAll(users);
            System.out.println("Loaded " + users.size() + " users");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + ex.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleUpdate() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a user first");
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Update User");
        dialog.setHeaderText("Update user: " + selected.getFullName());

        ButtonType updateButtonType = new ButtonType("Update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(updateButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(selected.getFullName());
        TextField emailField = new TextField(selected.getEmail());
        PasswordField passField = new PasswordField();
        passField.setPromptText("Leave empty to keep current password");
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        roleBox.setValue(selected.getRole());

        Label passHint = new Label("⚠ Leave password empty to keep current password");
        passHint.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11px;");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(passField, 1, 2);
        grid.add(passHint, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                selected.setFullName(nameField.getText().trim());
                selected.setEmail(emailField.getText().trim());

                String newPassword = passField.getText();
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    if (newPassword.length() < 6) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Password must be at least 6 characters");
                        return null;
                    }
                    String hashedPassword = PasswordUtil.hashPassword(newPassword);
                    selected.setPasswordHash(hashedPassword);
                }

                selected.setRole(roleBox.getValue());
                return selected;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            try {
                userCrud.update(user);

                // Update session if admin edited their own profile
                if (user.getId() == SessionManager.getInstance().getCurrentUser().getId()) {
                    SessionManager.getInstance().updateCurrentUser(user);
                    adminNameLabel.setText(user.getFullName());
                }

                loadUsers();
                showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", "Update failed: " + ex.getMessage());
            }
        });
    }

    @FXML
    private void handleDelete() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a user first");
            return;
        }

        // Prevent self-deletion
        if (selected.getId() == SessionManager.getInstance().getCurrentUser().getId()) {
            showAlert(Alert.AlertType.ERROR, "Error", "You cannot delete your own account!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete user: " + selected.getFullName() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userCrud.delete(selected.getId());
                    loadUsers();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "User deleted successfully!");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Delete failed: " + ex.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken(); // Clear token on logout

        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.close();
            loadScene("/fxml/user/login.fxml", "Login");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }


    private void loadScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("Digital Farm - " + title);
        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
