package tn.esprit.user.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.user.service.UserCrud;
import tn.esprit.utils.PasswordUtil;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.TokenManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class UsersListController {
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, Role> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private Button refreshButton;
    @FXML private Button updateButton;
    @FXML private Button deleteButton;
    @FXML private Button banButton;
    @FXML private Button exportExcelButton;
    @FXML private Button logoutButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Button clearFiltersButton;
    @FXML private Label resultCountLabel;

    private UserCrud userCrud = new UserCrud();
    private ObservableList<User> usersList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadUsers();
    }

    private void setupFilters() {
        // Populate role filter with "All" + each role
        ObservableList<String> roles = FXCollections.observableArrayList("All Roles");
        for (Role r : Role.values()) roles.add(r.name());
        roleFilter.setItems(roles);
        roleFilter.setValue("All Roles");

        // Wrap master list in a FilteredList
        filteredList = new FilteredList<>(usersList, u -> true);

        // Re-apply predicate whenever search text or role filter changes
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        roleFilter.valueProperty().addListener((obs, o, n) -> applyFilters());

        // Wrap in SortedList and bind to table comparator so column-click sort works
        SortedList<User> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(usersTable.comparatorProperty());
        usersTable.setItems(sortedList);

        // Update count label whenever visible list changes
        sortedList.addListener((javafx.collections.ListChangeListener<User>) c ->
                updateResultCount(sortedList.size()));
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedRole = roleFilter.getValue();
        boolean filterByRole = selectedRole != null && !selectedRole.equals("All Roles");

        filteredList.setPredicate(user -> {
            boolean matchesSearch = search.isEmpty()
                    || user.getFullName().toLowerCase().contains(search)
                    || user.getEmail().toLowerCase().contains(search)
                    || user.getRole().name().toLowerCase().contains(search);
            boolean matchesRole = !filterByRole || user.getRole().name().equals(selectedRole);
            return matchesSearch && matchesRole;
        });

        updateResultCount(filteredList.size());
    }

    private void updateResultCount(int count) {
        resultCountLabel.setText("Showing " + count + " user" + (count == 1 ? "" : "s"));
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        roleFilter.setValue("All Roles");
    }

    private void setupTable() {
        // Enable multi-row selection
        usersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

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

        // Custom cell factory for status badges
        statusColumn.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    if ("Banned".equals(status)) {
                        badge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; " +
                                "-fx-padding: 4 12; -fx-background-radius: 12; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; " +
                                "-fx-padding: 4 12; -fx-background-radius: 12; " +
                                "-fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
    }

    private void loadUsers() {
        try {
            usersList.clear();
            List<User> users = userCrud.findAll();
            usersList.addAll(users);
            applyFilters();
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

        Label passHint = new Label("⚠ Leave empty to keep current password. New password requires: 8+ chars, uppercase, lowercase, digit, special char.");
        passHint.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11px; -fx-wrap-text: true;");
        passHint.setWrapText(true);
        passHint.setMaxWidth(280);

        Label passError = new Label("");
        passError.setStyle("-fx-text-fill: #EF5350; -fx-font-size: 11px; -fx-font-weight: bold;");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(passField, 1, 2);
        grid.add(passHint, 1, 3);
        grid.add(passError, 1, 4);
        grid.add(new Label("Role:"), 0, 5);
        grid.add(roleBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Validate password in real-time
        passField.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty()) {
                String msg = tn.esprit.utils.ValidationUtil.getPasswordValidationMessage(n);
                passError.setText(msg.isEmpty() ? "✔ Password is strong" : "✖ " + msg);
                passError.setStyle("-fx-text-fill: " + (msg.isEmpty() ? "#2E7D32" : "#EF5350") + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                passError.setText("");
            }
        });

        // Disable Update button if password entered but invalid
        javafx.scene.Node updateBtn = dialog.getDialogPane().lookupButton(updateButtonType);
        passField.textProperty().addListener((obs, o, n) -> {
            if (!n.isEmpty()) {
                String msg = tn.esprit.utils.ValidationUtil.getPasswordValidationMessage(n);
                updateBtn.setDisable(!msg.isEmpty());
            } else {
                updateBtn.setDisable(false);
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == updateButtonType) {
                String newName = nameField.getText().trim();
                String newEmail = emailField.getText().trim();
                String newPassword = passField.getText();

                if (newName.isEmpty() || newEmail.isEmpty()) return null;

                selected.setFullName(newName);
                selected.setEmail(newEmail);

                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    // Already validated via button disable, but double-check
                    String pwErr = tn.esprit.utils.ValidationUtil.getPasswordValidationMessage(newPassword);
                    if (!pwErr.isEmpty()) return null;
                    selected.setPasswordHash(tn.esprit.utils.PasswordUtil.hashPassword(newPassword));
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
        List<User> selected = new java.util.ArrayList<>(usersTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select at least one user");
            return;
        }

        int currentId = SessionManager.getInstance().getCurrentUser().getId();
        boolean selfIncluded = selected.stream().anyMatch(u -> u.getId() == currentId);
        if (selfIncluded) {
            showAlert(Alert.AlertType.ERROR, "Error", "You cannot delete your own account!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete " + selected.size() + " selected user(s)?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int failed = 0;
                for (User user : selected) {
                    try {
                        userCrud.delete(user.getId());
                    } catch (Exception ex) {
                        failed++;
                    }
                }
                loadUsers();
                if (failed == 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            selected.size() + " user(s) deleted successfully!");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Partial Success",
                            (selected.size() - failed) + " deleted, " + failed + " failed.");
                }
            }
        });
    }

    @FXML
    private void handleBanToggle() {
        List<User> selected = new java.util.ArrayList<>(usersTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select at least one user");
            return;
        }
        int currentId = SessionManager.getInstance().getCurrentUser().getId();
        if (selected.stream().anyMatch(u -> u.getId() == currentId)) {
            showAlert(Alert.AlertType.ERROR, "Error", "You cannot ban your own account!");
            return;
        }

        // Determine action: if any selected is not banned → ban all; if all banned → unban all
        boolean willBan = selected.stream().anyMatch(u -> !u.isBanned());
        String action = willBan ? "Ban" : "Unban";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm " + action);
        confirm.setHeaderText(action + " " + selected.size() + " selected user(s)?");
        confirm.setContentText(willBan
                ? "Selected users will not be able to log in."
                : "Selected users will be able to log in again.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int failed = 0;
                for (User user : selected) {
                    try {
                        userCrud.setBanned(user.getId(), willBan);
                    } catch (Exception ex) {
                        failed++;
                    }
                }
                loadUsers();
                if (failed == 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            selected.size() + " user(s) " + (willBan ? "banned" : "unbanned") + " successfully.");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Partial Success",
                            (selected.size() - failed) + " succeeded, " + failed + " failed.");
                }
            }
        });
    }

    @FXML
    private void handleExportExcel() {
        List<User> currentUsers = usersTable.getItems();
        if (currentUsers == null || currentUsers.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "There are no users to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Users Excel File");
        fileChooser.setInitialFileName("users_export.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files (*.xlsx)", "*.xlsx"));

        Stage stage = (Stage) exportExcelButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Full Name", "Email", "Role", "Email Verified", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (User user : currentUsers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getFullName());
                row.createCell(2).setCellValue(user.getEmail());
                row.createCell(3).setCellValue(user.getRole().name());
                row.createCell(4).setCellValue(user.isEmailVerified() ? "Yes" : "No");
                row.createCell(5).setCellValue(user.getStatus());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Users exported to:\n" + file.getAbsolutePath());

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not export: " + ex.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        TokenManager.clearToken();
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        tn.esprit.MainFX.loadLoginOnStage(stage);
    }


    private void loadScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Scene scene = new Scene(loader.load());
        Stage stage = new Stage();
        stage.setTitle("AgriNova - " + title);
        stage.setScene(scene);
        stage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
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
