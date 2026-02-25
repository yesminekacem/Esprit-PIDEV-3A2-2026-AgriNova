package tn.esprit.pidev.controllers;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;
import tn.esprit.pidev.entities.Inventory;
import tn.esprit.pidev.entities.InventoryRentalStatus;
import tn.esprit.pidev.entities.ItemType;
import tn.esprit.pidev.services.InventoryService;
import tn.esprit.pidev.services.WeatherService;        // ← NEW
import tn.esprit.pidev.utils.DialogUtils;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.pidev.services.PDFService;
import tn.esprit.pidev.services.QRCodeService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Inventory Management View
 * Implements modern card-based grid layout
 */
public class InventoryController implements Initializable {

    // FXML Components
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button gridViewBtn;
    @FXML private Button listViewBtn;
    @FXML private ScrollPane scrollPane;
    @FXML private GridPane inventoryGrid;
    @FXML private VBox inventoryList;
    @FXML private VBox emptyState;
    @FXML private Label totalItemsLabel;
    @FXML private Label inStockLabel;
    @FXML private Label lowStockLabel;

    // Services
    private final InventoryService inventoryService = new InventoryService();
    private final WeatherService weatherService = new WeatherService();  // ← NEW
    private final QRCodeService  qrCodeService  = new QRCodeService();   // API #2
    private final PDFService     pdfService     = new PDFService();      // PDF export
    // Data
    private List<Inventory> allInventory;
    private boolean isGridView = true;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✅ InventoryController initialized");

        // Load inventory data
        loadInventoryData();

        // Setup search listener
        setupSearchListener();

        // Update statistics
        updateStatistics();
    }

    /**
     * Load all inventory items from database
     */
    private void loadInventoryData() {
        try {
            allInventory = inventoryService.getAllInventory();
            System.out.println("📦 Loaded " + allInventory.size() + " inventory items");

            if (allInventory.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                displayInventoryGrid();
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading inventory: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Failed to load inventory data");
        }
    }

    /**
     * Display inventory in grid layout
     */
    private void displayInventoryGrid() {
        inventoryGrid.getChildren().clear();

        int column = 0;
        int row = 0;
        final int COLUMNS = 3;

        for (Inventory item : allInventory) {
            VBox card = createInventoryCard(item);
            inventoryGrid.add(card, column, row);

            column++;
            if (column == COLUMNS) {
                column = 0;
                row++;
            }
        }
    }

    /**
     * Create a modern card for inventory item
     * Includes View, Edit, Delete, and the NEW QR Code functionality.
     */
    private VBox createInventoryCard(Inventory item) {
        // 1. Root container for the card
        VBox card = new VBox(15);
        card.getStyleClass().add("inventory-card");
        card.setPadding(new Insets(20));
        card.setCursor(Cursor.HAND);

        // 2. TOP SECTION: Type Icon (🚜, 🔧, etc.) and the Status Badge (In Stock, Rented)
        HBox topSection = new HBox();
        topSection.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(topSection, Priority.ALWAYS);

        VBox iconBox = new VBox();
        iconBox.getStyleClass().addAll("card-icon-box");
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(56, 56);
        iconBox.setMaxSize(56, 56);

        Label iconLabel = new Label(getIconForType(item.getItemType()));
        iconLabel.getStyleClass().add("card-icon");
        iconBox.getChildren().add(iconLabel);

        Label statusBadge = new Label(getStatusText(item.getRentalStatus()));
        statusBadge.getStyleClass().addAll("status-badge", getStatusStyle(item.getRentalStatus()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topSection.getChildren().addAll(iconBox, spacer, statusBadge);

        // 3. MIDDLE SECTION: Item Name and ID
        VBox detailsBox = new VBox(5);
        Label nameLabel = new Label(item.getItemName());
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);

        Label typeLabel = new Label(item.getItemType().toString() + " • ID: INV-" + item.getInventoryId());
        typeLabel.getStyleClass().add("card-subtitle");

        detailsBox.getChildren().addAll(nameLabel, typeLabel);

        // 4. BOTTOM SECTION: Maintenance Info and Action Buttons
        VBox bottomSection = new VBox(10);
        bottomSection.getStyleClass().add("card-bottom");

        Separator divider = new Separator();
        divider.getStyleClass().add("card-divider");

        HBox maintenanceRow = new HBox();
        maintenanceRow.setAlignment(Pos.CENTER_LEFT);

        String maintenanceText = item.getLastMaintenanceDate() != null
                ? "LAST MAINTAINED: " + item.getLastMaintenanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")).toUpperCase()
                : "NO MAINTENANCE RECORD";

        Label maintenanceLabel = new Label(maintenanceText);
        maintenanceLabel.getStyleClass().add("card-maintenance");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        // --- START OF BUTTON ACTIONS ---
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        // NEW: Added QR Code button
        // This calls the handleGenerateQR method you created to open the popup
        Button qrBtn = createActionButton("📲", "Generate QR Code");
        qrBtn.setOnAction(e -> handleGenerateQR(item));

        Button editBtn = createActionButton("✏", "Edit Equipment");
        editBtn.setOnAction(e -> handleEditItem(item));

        Button viewBtn = createActionButton("👁", "View Details");
        viewBtn.setOnAction(e -> handleViewItem(item));

        Button deleteBtn = createActionButton("🗑", "Delete Item");
        deleteBtn.setOnAction(e -> handleDeleteItem(item));

        // IMPORTANT: Added qrBtn to the list of children so it appears on screen
        actionButtons.getChildren().addAll(qrBtn, editBtn, viewBtn, deleteBtn);
        // --- END OF BUTTON ACTIONS ---

        maintenanceRow.getChildren().addAll(maintenanceLabel, actionSpacer, actionButtons);
        bottomSection.getChildren().addAll(divider, maintenanceRow);

        // 5. ASSEMBLE the card
        card.getChildren().addAll(topSection, detailsBox, bottomSection);

        // 6. ANIMATION: Subtle hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-translate-y: -4px; -fx-effect: dropshadow(gaussian, rgba(76, 175, 79, 0.3), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(""));

        return card;
    }

    private Button createActionButton(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.getStyleClass().add("card-action-button");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private String getIconForType(ItemType type) {
        return switch (type) {
            case EQUIPMENT -> "🚜";
            case TOOL -> "🔧";
            case CONSUMABLE -> "🌱";
            case STORAGE -> "📦";
            default -> "📦";
        };
    }

    private String getStatusText(InventoryRentalStatus status) {
        return switch (status) {
            case AVAILABLE -> "In Stock";
            case RENTED_OUT -> "Rented Out";
            case IN_USE -> "In Use";
            case MAINTENANCE -> "Maintenance";
            case RETIRED -> "Retired";
            default -> "Unknown";
        };
    }

    private String getStatusStyle(InventoryRentalStatus status) {
        return switch (status) {
            case AVAILABLE -> "status-success";
            case RENTED_OUT -> "status-info";
            case MAINTENANCE -> "status-warning";
            case RETIRED -> "status-danger";
            default -> "status-default";
        };
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterInventory(newValue);
        });
    }

    private void filterInventory(String query) {
        if (query == null || query.trim().isEmpty()) {
            displayInventoryGrid();
            return;
        }

        List<Inventory> filtered = inventoryService.searchInventory(query);

        if (filtered.isEmpty()) {
            inventoryGrid.getChildren().clear();
            showEmptyState();
        } else {
            hideEmptyState();
            allInventory = filtered;
            displayInventoryGrid();
        }
    }

    private void updateStatistics() {
        InventoryService.InventoryStatistics stats = inventoryService.getStatistics();

        totalItemsLabel.setText(String.valueOf(stats.totalItems));
        inStockLabel.setText(String.valueOf(stats.availableForRent));

        int lowStock = (int) allInventory.stream()
                .filter(i -> i.getQuantity() < 10 || i.isMaintenanceDueSoon())
                .count();
        lowStockLabel.setText(String.valueOf(lowStock));
    }

    private void showEmptyState() {
        inventoryGrid.setVisible(false);
        inventoryGrid.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        inventoryGrid.setVisible(true);
        inventoryGrid.setManaged(true);
    }

    // ==================== EVENT HANDLERS ====================

    @FXML
    private void handleAddItem() {
        System.out.println("➕ Add Item clicked");

        Inventory newItem = DialogUtils.showAddInventoryDialog();

        if (newItem != null) {
            boolean success = inventoryService.addInventory(newItem);

            if (success) {
                showSuccessAlert("Equipment added successfully!");
                loadInventoryData();
                updateStatistics();
            } else {
                showErrorAlert("Failed to add equipment. Please check all fields.");
            }
        }
    }

    private void handleEditItem(Inventory item) {
        System.out.println("✏ Edit Item: " + item.getItemName());

        Inventory updatedItem = DialogUtils.showEditInventoryDialog(item);

        if (updatedItem != null) {
            boolean success = inventoryService.updateInventory(updatedItem);

            if (success) {
                showSuccessAlert("Equipment updated successfully!");
                loadInventoryData();
                updateStatistics();
            } else {
                showErrorAlert("Failed to update equipment.");
            }
        }
    }

    private void handleViewItem(Inventory item) {
        System.out.println("👁 View Item: " + item.getItemName());
        DialogUtils.showInventoryDetailsDialog(item);
    }

    private void handleDeleteItem(Inventory item) {
        System.out.println("🗑 Delete Item: " + item.getItemName());

        boolean confirmed = DialogUtils.showConfirmation(
                "Delete Equipment",
                "Are you sure you want to delete: " + item.getItemName() + "?",
                "This action cannot be undone."
        );

        if (confirmed) {
            boolean success = inventoryService.deleteInventory(item.getInventoryId());

            if (success) {
                showSuccessAlert("Equipment deleted successfully!");
                loadInventoryData();
                updateStatistics();
            } else {
                showErrorAlert("Cannot delete equipment that is currently rented out.");
            }
        }
    }

    @FXML
    private void switchToGridView() {
        isGridView = true;
        inventoryGrid.setVisible(true);
        inventoryGrid.setManaged(true);
        inventoryList.setVisible(false);
        inventoryList.setManaged(false);

        gridViewBtn.getStyleClass().add("toggle-active");
        listViewBtn.getStyleClass().remove("toggle-active");

        displayInventoryGrid();
    }

    @FXML
    private void switchToListView() {
        isGridView = false;
        inventoryList.setVisible(true);
        inventoryList.setManaged(true);
        inventoryGrid.setVisible(false);
        inventoryGrid.setManaged(false);

        listViewBtn.getStyleClass().add("toggle-active");
        gridViewBtn.getStyleClass().remove("toggle-active");

        displayInventoryList();
    }

    private void displayInventoryList() {
        inventoryList.getChildren().clear();

        for (Inventory item : allInventory) {
            HBox listItem = createInventoryListItem(item);
            inventoryList.getChildren().add(listItem);
        }
    }

    private HBox createInventoryListItem(Inventory item) {
        HBox listItem = new HBox(20);
        listItem.getStyleClass().add("list-item");
        listItem.setAlignment(Pos.CENTER_LEFT);
        listItem.setPadding(new Insets(15, 20, 15, 20));

        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("list-icon-box");
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(48, 48);
        Label icon = new Label(getIconForType(item.getItemType()));
        icon.getStyleClass().add("list-icon");
        iconBox.getChildren().add(icon);

        VBox details = new VBox(5);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label name = new Label(item.getItemName());
        name.getStyleClass().add("list-item-title");
        Label type = new Label(item.getItemType().toString());
        type.getStyleClass().add("list-item-subtitle");
        details.getChildren().addAll(name, type);

        Label status = new Label(getStatusText(item.getRentalStatus()));
        status.getStyleClass().addAll("status-badge", getStatusStyle(item.getRentalStatus()));

        HBox actions = new HBox(8);
        Button editBtn = createActionButton("✏", "Edit");
        editBtn.setOnAction(e -> handleEditItem(item));
        Button viewBtn = createActionButton("👁", "View");
        viewBtn.setOnAction(e -> handleViewItem(item));
        Button deleteBtn = createActionButton("🗑", "Delete");
        deleteBtn.setOnAction(e -> handleDeleteItem(item));
        actions.getChildren().addAll(editBtn, viewBtn, deleteBtn);

        listItem.getChildren().addAll(iconBox, details, status, actions);

        return listItem;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== NEW: WEATHER API (Inventory — API #1) ====================

    /**
     * Fetches live weather for Tunis and recommends whether today is
     * a good day to perform equipment maintenance.
     *
     * Connected to the "🌤 Weather" button in InventoryView.fxml via onAction="#handleWeatherCheck"
     * API used: OpenWeatherMap REST API → WeatherService.java
     *
     * ⚠ Set your free API key in WeatherService.java line 13 before using this.
     */
    @FXML
    private void handleWeatherCheck() {
        // Run in a background thread so the UI stays responsive while calling the API
        new Thread(() -> {
            WeatherService.WeatherInfo weather = weatherService.getCurrentWeather("Tunis");
            String recommendation = weatherService.getMaintenanceRecommendation("Tunis");

            // Come back to the JavaFX thread to update the UI
            Platform.runLater(() -> {
                if (weather != null) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("🌤 Weather — Maintenance Advisor");
                    alert.setHeaderText(weather.getCityName() + "  |  " + weather.getDescription());
                    alert.setContentText(
                            "🌡  Temperature:  " + weather.getTemperature() + "°C\n" +
                                    "💧  Humidity:      " + weather.getHumidity() + "%\n" +
                                    "💨  Wind speed:   " + weather.getWindSpeed() + " km/h\n\n" +
                                    "─────────────────────────────────\n" +
                                    "📋  Maintenance Advice:\n\n" +
                                    recommendation
                    );
                    alert.getDialogPane().setMinWidth(420);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Weather Service Error");
                    alert.setHeaderText(null);
                    alert.setContentText(
                            "Could not fetch weather data.\n\n" +
                                    "Make sure you have:\n" +
                                    "  1. Set your API key in WeatherService.java (line 13)\n" +
                                    "  2. An active internet connection\n\n" +
                                    "Get a free key at: openweathermap.org/api"
                    );
                    alert.showAndWait();
                }
            });
        }).start();
    }

    // ==================== EXISTING HANDLERS (unchanged) ====================

    @FXML
    private void handleRefresh() {
        loadInventoryData();
        updateStatistics();
        showSuccessAlert("Data refreshed!");
    }

    @FXML
    private void showDashboard() {
        InventoryService.InventoryStatistics stats = inventoryService.getStatistics();
        StringBuilder sb = new StringBuilder();
        sb.append("INVENTORY DASHBOARD\n\n");
        sb.append("Total items: ").append(stats.totalItems).append("\n");
        sb.append(String.format("Total value: %.2f TND\n", stats.totalValue));
        sb.append("Rentable items: ").append(stats.rentableItems).append("\n");
        sb.append("Currently rented: ").append(stats.currentlyRented).append("\n");
        sb.append("Available for rent: ").append(stats.availableForRent).append("\n");
        sb.append("Need maintenance: ").append(stats.needMaintenance).append("\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Dashboard - Inventory");
        alert.setHeaderText("Inventory Overview");
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinWidth(450);
        alert.showAndWait();
    }

    @FXML
    private void goToRentals() {
        Scene scene = searchField != null ? searchField.getScene() : null;
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        Object controller = scene.getRoot().getProperties().get("mainController");
        if (controller instanceof MainController mainController) {
            mainController.showRentals();
        } else {
            showErrorAlert("Main layout controller not found. Cannot navigate to rentals.");
        }
    }

    @FXML
    private void handleGenerateQR(Inventory item) {
        Image qrImage = qrCodeService.generateQRImage(item);
        String savedPath = qrCodeService.saveQRCode(item);

        if (qrImage != null) {
            // Display in a popup dialog
            Stage qrStage = new Stage();
            qrStage.setTitle("QR Code — " + item.getItemName());
            qrStage.initModality(Modality.APPLICATION_MODAL);

            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(300);
            imageView.setFitHeight(300);

            Label label = new Label("Scan to view equipment details");
            label.setStyle("-fx-font-size: 13; -fx-text-fill: #757575;");

            Label pathLabel = new Label("Saved to: " + savedPath);
            pathLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #9E9E9E;");
            pathLabel.setWrapText(true);

            VBox vbox = new VBox(12, imageView, label, pathLabel);
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(20));

            qrStage.setScene(new Scene(vbox, 360, 420));
            qrStage.show();
        }
    }
    @FXML
    private void handleExportInventoryPDF() {
        try {
            // 1. Load the data (You already had this)
            List<Inventory> inventoryList = inventoryService.getAllInventory();

            // CHANGE 1: Added a check. If the list is empty, don't try to generate a file.
            if (inventoryList == null || inventoryList.isEmpty()) {
                showErrorAlert("No inventory data available to export.");
                return;
            }

            // CHANGE 2: CALL THE SERVICE.
            // This actually triggers the 'buildInventoryReportHTML' and 'writeFile'
            // methods inside your PDFService.java.
            String savedPath = pdfService.generateInventoryReport(inventoryList);

            // CHANGE 3: OPEN THE FILE.
            // Your service has an 'openFile' method that uses your computer's
            // default browser to show the report immediately.
            pdfService.openFile(savedPath);

            // CHANGE 4: Improved the Alert.
            // It now tells the user exactly WHERE the file was saved on their computer.
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Success");
            alert.setHeaderText("Inventory Report Generated");
            alert.setContentText("Your report has been saved to:\n" + savedPath);
            alert.showAndWait();

            System.out.println("✅ PDF/HTML Export process completed for " + inventoryList.size() + " items.");

        } catch (Exception e) {
            // Log the full error to the console for debugging
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Could not generate report");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }
}

