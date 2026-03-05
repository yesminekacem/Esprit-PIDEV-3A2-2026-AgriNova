package tn.esprit.inventory.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.inventory.dao.RentalDao;
import tn.esprit.inventory.entities.*;
import tn.esprit.inventory.services.EmailService;
import tn.esprit.inventory.services.InventoryService;
import tn.esprit.inventory.services.PDFService;
import tn.esprit.inventory.services.RentalService;
import tn.esprit.navigation.Router;
import tn.esprit.navigation.Routes;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * RentalController — Rental Management View.
 *
 * APIs integrated:
 *   API #1 — JavaMail  (EmailService) → send confirmation / overdue / completion emails
 *   API #2 — PDFService               → generate rental contracts
 */
public class RentalController implements Initializable {

    @FXML private Button dashboardBtn;
    // ── FXML fields ──────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML private Button addRentalBtn;
    @FXML private TabPane mainTabPane;
    @FXML private GridPane allRentalsGrid;
    @FXML private GridPane activeRentalsGrid;
    @FXML private GridPane pendingRentalsGrid;
    @FXML private GridPane overdueRentalsGrid;
    @FXML private VBox emptyStateAll;
    @FXML private VBox emptyStateActive;
    @FXML private VBox emptyStatePending;
    @FXML private VBox emptyStateOverdue;
    @FXML private HBox overdueWarningBanner;
    @FXML private Label overdueCountLabel;
    @FXML private Label totalRentalsLabel;
    @FXML private Label activeRentalsLabel;
    @FXML private Label pendingRentalsLabel;
    @FXML private Label overdueRentalsLabel;
    @FXML private Label totalRevenueLabel;

    // ── Services ─────────────────────────────────────────────────
    private final RentalService   rentalService   = new RentalService();
    private final InventoryService inventoryService = new InventoryService();
    private final RentalDao       rentalDao        = new RentalDao();
    private final EmailService    emailService     = new EmailService();   // API #1
    private final PDFService      pdfService       = new PDFService();     // API #2

    // ── Data ─────────────────────────────────────────────────────
    private List<Rental> allRentals = new ArrayList<>();
    private static final int GRID_COLUMNS = 2;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────────────────────
    //  INIT
    // ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✅ RentalController initialized");
        setupStatusFilter();
        setupSearchListener();
        setupTabListener();
        loadAllData();
        applyRoleBasedVisibility();
    }

    private void setupStatusFilter() {
        statusFilter.getItems().clear();
        statusFilter.getItems().add("All");
        for (RentalStatus status : RentalStatus.values())
            statusFilter.getItems().add(status.name());
        statusFilter.setValue("All");
        statusFilter.valueProperty().addListener((obs, o, n) -> applySearchAndFilter());
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, o, n) -> applySearchAndFilter());
    }

    private void setupTabListener() {
        mainTabPane.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> loadAllData());
    }

    // ─────────────────────────────────────────────────────────────
    //  DATA LOADING
    // ─────────────────────────────────────────────────────────────

    private void loadAllData() {
        try {
            allRentals = rentalService.getAllRentals();
            applySearchAndFilter();

            List<Rental> active  = rentalService.getActiveRentals();
            List<Rental> pending = allRentals.stream()
                    .filter(r -> r.getRentalStatus() == RentalStatus.PENDING)
                    .collect(Collectors.toList());
            List<Rental> overdue = rentalService.getOverdueRentals();

            populateGrid(activeRentalsGrid,  active,  emptyStateActive);
            populateGrid(pendingRentalsGrid, pending, emptyStatePending);
            populateGrid(overdueRentalsGrid, overdue, emptyStateOverdue);

            updateStats();
            updateOverdueBanner();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load rental data");
        }
    }

    private boolean isAdmin() {
        return SessionManager.getInstance().isAdmin();
    }

    private String currentUserEmail() {
        return SessionManager.getInstance().getCurrentUserEmail();
    }

    private void applyRoleBasedVisibility() {
        boolean admin = isAdmin();

        if (admin) {
            // Admin: no "request rental" button
            if (addRentalBtn != null) {
                addRentalBtn.setVisible(false);
                addRentalBtn.setManaged(false);
                dashboardBtn.setVisible(true);
                dashboardBtn.setManaged(true);
            }else{
                dashboardBtn.setVisible(false);
                dashboardBtn.setManaged(false);

            }
            return;
        }

        // USER: show only their own rentals in all tabs
        String email = currentUserEmail();
        if (email == null || email.isBlank() || allRentals == null) {
            return;
        }

        allRentals = allRentals.stream()
                .filter(r -> email.equalsIgnoreCase(r.getRenterContact()))
                .collect(Collectors.toList());

        applySearchAndFilter();
    }

    private void applySearchAndFilter() {
        if (allRentals == null) return;
        String query    = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
        String statusVal = statusFilter.getValue();

        List<Rental> filtered = allRentals.stream().filter(r -> {
            boolean matchSearch = true;
            if (!query.isEmpty()) {
                String renter = r.getRenterName() != null ? r.getRenterName().toLowerCase() : "";
                String equip  = (r.getInventory() != null && r.getInventory().getItemName() != null)
                        ? r.getInventory().getItemName().toLowerCase() : "";
                matchSearch = renter.contains(query) || equip.contains(query);
            }
            boolean matchStatus = true;
            if (statusVal != null && !"All".equalsIgnoreCase(statusVal)) {
                try {
                    matchStatus = r.getRentalStatus() == RentalStatus.valueOf(statusVal);
                } catch (IllegalArgumentException ignored) {}
            }
            return matchSearch && matchStatus;
        }).collect(Collectors.toList());

        populateGrid(allRentalsGrid, filtered, emptyStateAll);
    }

    // ─────────────────────────────────────────────────────────────
    //  GRID BUILDER
    // ─────────────────────────────────────────────────────────────

    private void populateGrid(GridPane grid, List<Rental> rentals, VBox emptyState) {
        grid.getChildren().clear();
        if (rentals == null || rentals.isEmpty()) {
            grid.setVisible(false); grid.setManaged(false);
            if (emptyState != null) { emptyState.setVisible(true); emptyState.setManaged(true); }
            return;
        }
        grid.setVisible(true); grid.setManaged(true);
        if (emptyState != null) { emptyState.setVisible(false); emptyState.setManaged(false); }

        if (grid.getColumnConstraints().size() < GRID_COLUMNS) {
            grid.getColumnConstraints().clear();
            for (int i = 0; i < GRID_COLUMNS; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / GRID_COLUMNS);
                grid.getColumnConstraints().add(cc);
            }
        }
        int col = 0, row = 0;
        for (Rental rental : rentals) {
            grid.add(createRentalCard(rental), col, row);
            col++;
            if (col == GRID_COLUMNS) { col = 0; row++; }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CARD BUILDER
    // ─────────────────────────────────────────────────────────────

    private VBox createRentalCard(Rental rental) {
        VBox card = new VBox(14);
        card.getStyleClass().add("rental-card");
        card.setPadding(new Insets(22));
        card.setPrefWidth(600);
        card.setCursor(Cursor.HAND);

        RentalStatus status = rental.getRentalStatus();
        if (rental.isOverdue()) card.getStyleClass().add("rental-card-overdue");
        else card.getStyleClass().add("rental-card-" + (status != null ? status.name().toLowerCase() : "pending"));

        card.setOnMouseEntered(e -> card.setStyle("-fx-translate-y:-3px; -fx-effect:dropshadow(gaussian, rgba(76,175,79,0.25), 20, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle(""));

        // Top row
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        VBox iconBox = new VBox();
        iconBox.getStyleClass().add("card-icon-box");
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(55, 55); iconBox.setMaxSize(55, 55);
        Label iconLabel = new Label(getIconForItemType(rental));
        iconLabel.setStyle("-fx-font-size:26px;");
        iconBox.getChildren().add(iconLabel);
        Label statusBadge = new Label(status != null ? status.name() : "PENDING");
        statusBadge.getStyleClass().addAll("status-badge", "badge-" + (status != null ? status.name().toLowerCase() : "pending"));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(iconBox, spacer, statusBadge);

        // Equipment info
        VBox equipmentBox = new VBox(3);
        String equipmentName = "Equipment #" + rental.getInventoryId();
        String typeText = "Unknown";
        Inventory inv = rental.getInventory();
        if (inv != null) {
            if (inv.getItemName() != null && !inv.getItemName().isEmpty()) equipmentName = inv.getItemName();
            if (inv.getItemType() != null) typeText = inv.getItemType().name();
        }
        Label nameLabel = new Label(equipmentName);
        nameLabel.getStyleClass().add("card-equipment-name");
        nameLabel.setWrapText(true);
        Label typeLabel = new Label(typeText + " • Rental #" + rental.getRentalId());
        typeLabel.getStyleClass().add("card-equipment-type");
        equipmentBox.getChildren().addAll(nameLabel, typeLabel);

        Separator divider = new Separator();
        divider.getStyleClass().add("divider-line");

        // Details grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(20); detailsGrid.setVgap(8);
        int drow = 0;
        addDetailRow(detailsGrid, drow++, "👤 Renter", rental.getRenterName(), "📅 Start", formatDate(rental.getStartDate()));
        addDetailRow(detailsGrid, drow++, "📞 Contact", rental.getRenterContact(), "📅 End", formatDate(rental.getEndDate()));
        addDetailRow(detailsGrid, drow, "💰 Rate", String.format("%.2f TND/day", rental.getDailyRate()), "📆 Days", rental.getTotalDays() + " days");

        // Cost row
        HBox costRow = new HBox();
        costRow.setAlignment(Pos.CENTER_LEFT);
        VBox leftInfo = new VBox(4);
        if (status == RentalStatus.ACTIVE && !rental.isOverdue()) {
            long daysLeft = rental.getDaysUntilReturn();
            Label dl = new Label("⏰ " + daysLeft + " days remaining");
            dl.getStyleClass().add(daysLeft <= 2 ? "card-days-remaining-urgent" : "card-days-remaining-ok");
            leftInfo.getChildren().add(dl);
        } else if (rental.isOverdue()) {
            long overdueDays = Math.abs(rental.getDaysUntilReturn());
            Label ol = new Label("⚠️ OVERDUE by " + overdueDays + " days");
            ol.getStyleClass().add("card-overdue-warning");
            Label lf = new Label("Late Fee: +" + String.format("%.2f", rental.calculateLateFee()) + " TND");
            lf.setStyle("-fx-text-fill:#F44336;-fx-font-size:12px;");
            leftInfo.getChildren().addAll(ol, lf);
        } else if (status == RentalStatus.COMPLETED && rental.getOwnerRating() != null) {
            leftInfo.getChildren().add(buildStars(rental.getOwnerRating()));
        }
        Region costSpacer = new Region(); HBox.setHgrow(costSpacer, Priority.ALWAYS);
        Label totalCostLabel = new Label(String.format("%.2f TND", rental.getTotalCost()));
        totalCostLabel.getStyleClass().add("card-total-cost");
        costRow.getChildren().addAll(leftInfo, costSpacer, totalCostLabel);

        // ── Action buttons ────────────────────────────────────
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        if (isAdmin()) {
            // ADMIN: full management actions
            switch (status) {
                case PENDING -> {
                    Button approveBtn = new Button("✓ Approve");
                    approveBtn.getStyleClass().add("btn-approve");
                    approveBtn.setOnAction(e -> handleApprove(rental));

                    Button editBtn = new Button("✏ Edit");
                    editBtn.getStyleClass().add("btn-details");
                    editBtn.setOnAction(e -> handleEditRental(rental));

                    Button cancelBtn = new Button("✗ Cancel");
                    cancelBtn.getStyleClass().add("btn-reject");
                    cancelBtn.setOnAction(e -> handleCancel(rental));

                    Button deleteBtn = new Button("🗑 Delete");
                    deleteBtn.getStyleClass().add("btn-reject");
                    deleteBtn.setOnAction(e -> handleDeleteRental(rental));

                    actionsRow.getChildren().addAll(approveBtn, editBtn, cancelBtn, deleteBtn);
                }
                case APPROVED -> {
                    Button activateBtn = new Button("▶ Activate");
                    activateBtn.getStyleClass().add("btn-approve");
                    activateBtn.setOnAction(e -> handleActivate(rental));

                    Button editBtn = new Button("✏ Edit");
                    editBtn.getStyleClass().add("btn-details");
                    editBtn.setOnAction(e -> handleEditRental(rental));

                    // ✅ API #2 — PDF contract on approved rentals
                    Button contractBtn = new Button("📄 Contract");
                    contractBtn.getStyleClass().add("btn-details");
                    contractBtn.setOnAction(e -> handleGenerateContract(rental));

                    Button cancelBtn = new Button("✗ Cancel");
                    cancelBtn.getStyleClass().add("btn-reject");
                    cancelBtn.setOnAction(e -> handleCancel(rental));

                    actionsRow.getChildren().addAll(activateBtn, editBtn, contractBtn, cancelBtn);
                }
                case ACTIVE -> {
                    Button completeBtn = new Button(rental.isOverdue() ? "✓ Mark Returned" : "✓ Complete");
                    completeBtn.getStyleClass().add("btn-complete");
                    completeBtn.setOnAction(e -> handleComplete(rental));

                    Button detailsBtn = new Button("👁 Details");
                    detailsBtn.getStyleClass().add("btn-details");
                    detailsBtn.setOnAction(e -> handleViewDetails(rental));

                    // ✅ API #1 — Email overdue reminder
                    if (rental.isOverdue()) {
                        Button emailBtn = new Button("📧 Remind");
                        emailBtn.getStyleClass().add("btn-details");
                        emailBtn.setStyle("-fx-background-color:#FFF3E0;-fx-text-fill:#E65100;");
                        emailBtn.setOnAction(e -> handleSendOverdueReminder(rental));
                        actionsRow.getChildren().addAll(completeBtn, emailBtn, detailsBtn);
                    } else {
                        actionsRow.getChildren().addAll(completeBtn, detailsBtn);
                    }
                }
                case COMPLETED, CANCELLED, RETURNED, DISPUTED -> {
                    Button detailsBtn = new Button("👁 View Details");
                    detailsBtn.getStyleClass().add("btn-details");
                    detailsBtn.setOnAction(e -> handleViewDetails(rental));

                    // ✅ API #2 — PDF contract for completed rentals too
                    Button contractBtn = new Button("📄 Contract");
                    contractBtn.getStyleClass().add("btn-details");
                    contractBtn.setOnAction(e -> handleGenerateContract(rental));

                    Button deleteBtn = new Button("🗑 Delete");
                    deleteBtn.getStyleClass().add("btn-reject");
                    deleteBtn.setOnAction(e -> handleDeleteRental(rental));

                    actionsRow.getChildren().addAll(detailsBtn, contractBtn, deleteBtn);
                }
                default -> {
                    Button detailsBtn = new Button("👁 View Details");
                    detailsBtn.getStyleClass().add("btn-details");
                    detailsBtn.setOnAction(e -> handleViewDetails(rental));
                    actionsRow.getChildren().add(detailsBtn);
                }
            }
        } else {
            // USER: read-only — can only view details
            Button detailsBtn = new Button("👁 View Details");
            detailsBtn.getStyleClass().add("btn-details");
            detailsBtn.setOnAction(e -> handleViewDetails(rental));
            actionsRow.getChildren().add(detailsBtn);
        }

        card.getChildren().addAll(topRow, equipmentBox, divider, detailsGrid, costRow, actionsRow);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    //  API #1 — EMAIL  (JavaMail)
    // ─────────────────────────────────────────────────────────────

    /**
     * Sends a rental confirmation email after a rental is approved.
     * Runs in background — UI stays responsive.
     * ⚠ renterContact must be a valid email address for this to work.
     */
    private void sendConfirmationEmail(Rental rental) {
        new Thread(() -> {
            boolean sent = emailService.sendRentalConfirmation(rental);
            Platform.runLater(() -> {
                if (sent) {
                    showSuccess("✅ Confirmation email sent to: " + rental.getRenterContact());
                } else {
                    System.err.println("⚠ Email could not be sent to: " + rental.getRenterContact()
                            + " — check SENDER_EMAIL/SENDER_PASSWORD in EmailService.java");
                }
            });
        }).start();
    }

    /**
     * Sends an overdue reminder email.
     * Triggered by the 📧 Remind button on overdue ACTIVE cards.
     */
    private void handleSendOverdueReminder(Rental rental) {
        new Thread(() -> {
            boolean sent = emailService.sendOverdueReminder(rental);
            Platform.runLater(() -> {
                if (sent) {
                    showSuccess("📧 Overdue reminder sent to: " + rental.getRenterContact());
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    a.setTitle("Email Failed");
                    a.setHeaderText(null);
                    a.setContentText(
                            "Could not send reminder email.\n\n" +
                                    "Open EmailService.java and set:\n" +
                                    "  SENDER_EMAIL    = your Gmail address\n" +
                                    "  SENDER_PASSWORD = your Gmail App Password\n\n" +
                                    "Get an App Password: Google Account → Security → App Passwords"
                    );
                    a.showAndWait();
                }
            });
        }).start();
    }

    /**
     * Sends a completion summary email after a rental is marked complete.
     */
    private void sendCompletionEmail(Rental rental) {
        new Thread(() -> {
            boolean sent = emailService.sendCompletionSummary(rental);
            Platform.runLater(() -> {
                if (sent) System.out.println("✅ Completion email sent to: " + rental.getRenterContact());
                else System.err.println("⚠ Completion email failed for: " + rental.getRenterContact());
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    //  API #2 — PDF CONTRACT  (PDFService)
    // ─────────────────────────────────────────────────────────────

    /**
     * Generates a rental contract as a styled HTML file and opens it in the browser.
     * Triggered by the 📄 Contract button on APPROVED and COMPLETED cards.
     * Use Ctrl+P in browser to print/save as PDF.
     */
    private void handleGenerateContract(Rental rental) {
        new Thread(() -> {
            String filePath = pdfService.generateRentalContract(rental);
            Platform.runLater(() -> {
                if (filePath != null) {
                    pdfService.openFile(filePath);
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Contract Generated 📄");
                    a.setHeaderText(null);
                    a.setContentText(
                            "Rental contract for " + rental.getRenterName() + " generated!\n\n" +
                                    "📁 Saved to:\n" + filePath + "\n\n" +
                                    "Tip: Press Ctrl+P in the browser to save as PDF."
                    );
                    a.getDialogPane().setMinWidth(460);
                    a.showAndWait();
                } else {
                    showError("Failed to generate contract.");
                }
            });
        }).start();
    }

    // ─────────────────────────────────────────────────────────────
    //  CRUD HANDLERS
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleAddRental() {
        addRentalBtn.setDisable(true);
        try {
            Dialog<Rental> dialog = new Dialog<>();
            dialog.setTitle("Create New Rental Request");
            dialog.setHeaderText("Fill the rental details below");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(20, 20, 10, 20));

            ComboBox<Inventory> equipmentCombo = new ComboBox<>();
            equipmentCombo.setPrefWidth(350);
            List<Inventory> available = inventoryService.getAvailableForRent();
            equipmentCombo.getItems().addAll(available);
            equipmentCombo.setCellFactory(cb -> new ListCell<>() {
                @Override protected void updateItem(Inventory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null :
                            item.getItemName() + " - " + String.format("%.2f TND/day", item.getRentalPricePerDay()));
                }
            });
            equipmentCombo.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Inventory item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null :
                            item.getItemName() + " - " + String.format("%.2f TND/day", item.getRentalPricePerDay()));
                }
            });

            TextField renterNameField    = new TextField();
            TextField renterContactField = new TextField();
            renterContactField.setPromptText("email@example.com (for notifications)");
            TextArea renterAddressArea   = new TextArea();
            renterAddressArea.setPrefRowCount(3);
            DatePicker startDatePicker = new DatePicker();
            DatePicker endDatePicker   = new DatePicker();

            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) return;
                    setDisable(item.isBefore(LocalDate.now()));
                }
            });

            Label durationLabel = new Label("Duration: 0 days");
            VBox pricingBox = new VBox(4);
            pricingBox.setPadding(new Insets(12));
            pricingBox.setStyle("-fx-background-color:#F9FAFB;-fx-background-radius:8;");
            Label dailyRateLabel      = new Label("Daily Rate: 0.00 TND");
            Label durationSummary     = new Label("Duration: 0 days");
            Label subtotalLabel       = new Label("Subtotal: 0.00 TND");
            Label depositLabel        = new Label("Security Deposit (50%): 0.00 TND");
            Label totalLabel          = new Label("TOTAL: 0.00 TND");
            totalLabel.setStyle("-fx-font-weight:bold;-fx-text-fill:#4CAF50;");
            pricingBox.getChildren().addAll(dailyRateLabel, durationSummary, subtotalLabel, depositLabel, new Separator(), totalLabel);

            CheckBox deliveryCheckBox = new CheckBox("Requires Delivery");
            TextField deliveryFeeField = new TextField();
            deliveryFeeField.setPromptText("Delivery fee (TND)");
            deliveryFeeField.setVisible(false);
            deliveryCheckBox.selectedProperty().addListener((obs, o, n) -> deliveryFeeField.setVisible(n));

            Runnable recalc = () -> {
                Inventory sel = equipmentCombo.getValue();
                LocalDate s = startDatePicker.getValue(), e = endDatePicker.getValue();
                double rate = sel != null ? sel.getRentalPricePerDay() : 0;
                int days = 0;
                if (s != null && e != null && !e.isBefore(s)) { days = (int) Rental.calculateDaysBetween(s, e); if (days <= 0) days = 1; }
                double delivFee = 0;
                if (deliveryCheckBox.isSelected()) { try { delivFee = Double.parseDouble(deliveryFeeField.getText()); } catch (Exception ignored) {} }
                double sub = rate * days, total = sub + delivFee, dep = total * 0.5;
                durationLabel.setText("Duration: " + days + " days");
                dailyRateLabel.setText(String.format("Daily Rate: %.2f TND", rate));
                durationSummary.setText("Duration: " + days + " days");
                subtotalLabel.setText(String.format("Subtotal: %.2f TND", sub));
                depositLabel.setText(String.format("Security Deposit (50%%): %.2f TND", dep));
                totalLabel.setText(String.format("TOTAL: %.2f TND", total));
            };
            equipmentCombo.valueProperty().addListener((obs, o, n) -> recalc.run());
            startDatePicker.valueProperty().addListener((obs, o, n) -> recalc.run());
            endDatePicker.valueProperty().addListener((obs, o, n) -> recalc.run());
            deliveryCheckBox.selectedProperty().addListener((obs, o, n) -> recalc.run());
            deliveryFeeField.textProperty().addListener((obs, o, n) -> recalc.run());

            int row = 0;
            grid.add(new Label("Equipment:"), 0, row); grid.add(equipmentCombo, 1, row++, 3, 1);
            grid.add(new Label("Renter Name:"), 0, row); grid.add(renterNameField, 1, row++);
            grid.add(new Label("Contact/Email:"), 0, row); grid.add(renterContactField, 1, row++);
            grid.add(new Label("Renter Address:"), 0, row); grid.add(renterAddressArea, 1, row++, 3, 1);
            grid.add(new Label("Start Date:"), 0, row); grid.add(startDatePicker, 1, row++);
            grid.add(new Label("End Date:"), 0, row); grid.add(endDatePicker, 1, row++);
            grid.add(durationLabel, 1, row++);
            grid.add(deliveryCheckBox, 0, row); grid.add(deliveryFeeField, 1, row++);
            grid.add(pricingBox, 0, row, 4, 1);
            dialog.getDialogPane().setContent(grid);

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                resetBorder(equipmentCombo, renterNameField, renterContactField, startDatePicker, endDatePicker);
                if (equipmentCombo.getValue() == null) { setErrorBorder(equipmentCombo); valid = false; }
                if (renterNameField.getText() == null || renterNameField.getText().trim().isEmpty()) { setErrorBorder(renterNameField); valid = false; }
                if (renterContactField.getText() == null || renterContactField.getText().trim().isEmpty()) { setErrorBorder(renterContactField); valid = false; }
                LocalDate s = startDatePicker.getValue(), e = endDatePicker.getValue();
                if (s == null || s.isBefore(LocalDate.now())) { setErrorBorder(startDatePicker); valid = false; }
                if (e == null || (s != null && !e.isAfter(s))) { setErrorBorder(endDatePicker); valid = false; }
                if (!valid) { event.consume(); showError("Please correct the highlighted fields."); }
            });

            dialog.setResultConverter(btn -> {
                if (btn == saveButtonType) {
                    Inventory sel = equipmentCombo.getValue();
                    if (sel == null) return null;
                    Rental rental = new Rental();
                    rental.setInventoryId(sel.getInventoryId());
                    rental.setOwnerName(sel.getOwnerName());
                    rental.setRenterName(renterNameField.getText());
                    rental.setRenterContact(renterContactField.getText());
                    rental.setRenterAddress(renterAddressArea.getText());
                    rental.setStartDate(startDatePicker.getValue());
                    rental.setEndDate(endDatePicker.getValue());
                    rental.setDailyRate(sel.getRentalPricePerDay());
                    rental.setRequiresDelivery(deliveryCheckBox.isSelected());
                    double df = 0;
                    if (deliveryCheckBox.isSelected()) { try { df = Double.parseDouble(deliveryFeeField.getText()); } catch (Exception ignored) {} }
                    rental.setDeliveryFee(df);
                    return rental;
                }
                return null;
            });

            Optional<Rental> result = dialog.showAndWait();
            result.ifPresent(rental -> {
                boolean ok = rentalService.requestRental(rental);
                if (ok) {
                    showSuccess("Rental request submitted!");
                    loadAllData();
                } else {
                    showError("Failed to create rental");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open rental dialog");
        } finally {
            addRentalBtn.setDisable(false);
        }
    }

    private void handleApprove(Rental rental) {
        boolean confirmed = showConfirm("Approve Rental",
                "Approve rental for " + rental.getRenterName() + "?");
        if (!confirmed) return;

        boolean ok = rentalService.approveRental(rental.getRentalId());
        if (ok) {
            showSuccess("Rental approved!");
            loadAllData();
            // ✅ API #1 — send confirmation email on approval
            sendConfirmationEmail(rental);
        } else {
            showError("Cannot approve this rental");
        }
    }

    private void handleActivate(Rental rental) {
        boolean confirmed = showConfirm("Activate Rental",
                "Activate rental? Equipment has been picked up.");
        if (!confirmed) return;

        boolean ok = rentalService.activateRental(rental.getRentalId());
        if (ok) {
            showSuccess("Rental activated!");
            loadAllData();
        } else {
            showError("Failed to activate rental.");
        }
    }

    private void handleComplete(Rental rental) {
        Dialog<CompleteRentalData> dialog = new Dialog<>();
        dialog.setTitle("Complete Rental #" + rental.getRentalId());
        dialog.setHeaderText("Confirm return condition and rating");

        ButtonType completeButtonType = new ButtonType("Complete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(completeButtonType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        TextArea returnConditionArea = new TextArea();
        returnConditionArea.setPromptText("Describe return condition...");
        returnConditionArea.setPrefRowCount(3);
        Label ratingLabel = new Label("Rate the renter (1–5):");
        HBox starsBox = new HBox(5);
        starsBox.setAlignment(Pos.CENTER_LEFT);
        List<ToggleButton> starButtons = new ArrayList<>();
        final int[] selectedRating = {5};
        for (int i = 1; i <= 5; i++) {
            ToggleButton star = new ToggleButton("★");
            star.setUserData(i);
            star.setStyle("-fx-font-size:18px;-fx-text-fill:#FBBF24;-fx-background-color:transparent;");
            int rv = i;
            star.setOnAction(e -> {
                selectedRating[0] = rv;
                for (ToggleButton b : starButtons) {
                    int v = (int) b.getUserData();
                    b.setSelected(v <= rv);
                    b.setText(v <= rv ? "★" : "☆");
                }
            });
            starButtons.add(star);
            starsBox.getChildren().add(star);
        }
        for (ToggleButton b : starButtons) b.setSelected(true);
        box.getChildren().addAll(new Label("Return Condition:"), returnConditionArea, ratingLabel, starsBox);
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == completeButtonType) {
                CompleteRentalData d = new CompleteRentalData();
                d.returnCondition = returnConditionArea.getText();
                d.rating = selectedRating[0];
                return d;
            }
            return null;
        });

        Optional<CompleteRentalData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            boolean ok = rentalService.completeRental(rental.getRentalId(), data.returnCondition, data.rating);
            if (ok) {
                showSuccess("Rental completed!");
                loadAllData();
                // ✅ API #1 — send completion email
                sendCompletionEmail(rental);
            } else {
                showError("Cannot complete this rental");
            }
        });
    }

    private void handleEditRental(Rental rental) {
        if (rental.getRentalStatus() != RentalStatus.PENDING &&
                rental.getRentalStatus() != RentalStatus.APPROVED) {
            showError("Only PENDING or APPROVED rentals can be edited.");
            return;
        }
        Dialog<Rental> dialog = new Dialog<>();
        dialog.setTitle("Edit Rental #" + rental.getRentalId());
        dialog.setHeaderText("Update rental details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        TextField renterNameField    = new TextField(rental.getRenterName());
        TextField renterContactField = new TextField(rental.getRenterContact());
        TextArea renterAddressArea   = new TextArea(rental.getRenterAddress());
        renterAddressArea.setPrefRowCount(3);
        DatePicker startDatePicker = new DatePicker(rental.getStartDate());
        DatePicker endDatePicker   = new DatePicker(rental.getEndDate());

        startDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) return;
                setDisable(item.isBefore(LocalDate.now()));
            }
        });

        Label durationLabel = new Label("Duration: " + rental.getTotalDays() + " days");
        Runnable recalc = () -> {
            LocalDate s = startDatePicker.getValue(), e = endDatePicker.getValue();
            int days = 0;
            if (s != null && e != null && !e.isBefore(s)) { days = (int) Rental.calculateDaysBetween(s, e); if (days <= 0) days = 1; }
            durationLabel.setText("Duration: " + days + " days");
        };
        startDatePicker.valueProperty().addListener((obs, o, n) -> recalc.run());
        endDatePicker.valueProperty().addListener((obs, o, n) -> recalc.run());

        int row = 0;
        grid.add(new Label("Renter Name:"), 0, row); grid.add(renterNameField, 1, row++);
        grid.add(new Label("Contact/Email:"), 0, row); grid.add(renterContactField, 1, row++);
        grid.add(new Label("Renter Address:"), 0, row); grid.add(renterAddressArea, 1, row++, 2, 1);
        grid.add(new Label("Start Date:"), 0, row); grid.add(startDatePicker, 1, row++);
        grid.add(new Label("End Date:"), 0, row); grid.add(endDatePicker, 1, row++);
        grid.add(durationLabel, 1, row);
        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;
            resetBorder(renterNameField, renterContactField, startDatePicker, endDatePicker);
            if (renterNameField.getText() == null || renterNameField.getText().trim().isEmpty()) { setErrorBorder(renterNameField); valid = false; }
            if (renterContactField.getText() == null || renterContactField.getText().trim().isEmpty()) { setErrorBorder(renterContactField); valid = false; }
            LocalDate s = startDatePicker.getValue(), e = endDatePicker.getValue();
            if (s == null || s.isBefore(LocalDate.now())) { setErrorBorder(startDatePicker); valid = false; }
            if (e == null || (s != null && !e.isAfter(s))) { setErrorBorder(endDatePicker); valid = false; }
            if (!valid) { event.consume(); showError("Please correct the highlighted fields."); }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                rental.setRenterName(renterNameField.getText());
                rental.setRenterContact(renterContactField.getText());
                rental.setRenterAddress(renterAddressArea.getText());
                rental.setStartDate(startDatePicker.getValue());
                rental.setEndDate(endDatePicker.getValue());
                int days = (int) Rental.calculateDaysBetween(rental.getStartDate(), rental.getEndDate());
                if (days <= 0) days = 1;
                rental.setTotalDays(days);
                double tc = rental.getDailyRate() * days + (rental.isRequiresDelivery() ? rental.getDeliveryFee() : 0);
                rental.setTotalCost(tc);
                rental.setSecurityDeposit(tc * 0.5);
                return rental;
            }
            return null;
        });

        Optional<Rental> updated = dialog.showAndWait();
        updated.ifPresent(r -> {
            boolean ok = rentalService.updateRental(r);
            if (ok) { showSuccess("Rental updated."); loadAllData(); }
            else showError("Failed to update rental.");
        });
    }

    private void handleDeleteRental(Rental rental) {
        boolean confirmed = showConfirm("Delete Rental",
                "Delete rental #" + rental.getRentalId() + "? This cannot be undone.");
        if (!confirmed) return;
        boolean ok = rentalService.deleteRental(rental.getRentalId());
        if (ok) { showSuccess("Rental deleted."); loadAllData(); }
        else showError("Failed to delete rental.");
    }

    private void handleCancel(Rental rental) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cancel Rental #" + rental.getRentalId());
        dialog.setHeaderText("Provide an optional reason");
        ButtonType cancelBtnType = new ButtonType("Cancel Rental", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelBtnType, ButtonType.CANCEL);
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Reason for cancellation (optional)...");
        reasonArea.setPrefRowCount(3);
        dialog.getDialogPane().setContent(reasonArea);
        dialog.setResultConverter(btn -> btn == cancelBtnType ? reasonArea.getText() : null);

        Optional<String> reasonOpt = dialog.showAndWait();
        reasonOpt.ifPresent(reason -> {
            boolean confirmed = showConfirm("Confirm", "Are you sure you want to cancel this rental?");
            if (!confirmed) return;
            boolean ok = rentalService.cancelRental(rental.getRentalId(), reason);
            if (ok) { showSuccess("Rental cancelled."); loadAllData(); }
            else showError("Cannot cancel.");
        });
    }

    private void handleViewDetails(Rental rental) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rental Details #" + rental.getRentalId());
        alert.setHeaderText("Rental Details");
        StringBuilder sb = new StringBuilder();

        sb.append("EQUIPMENT\n");
        Inventory inv = rental.getInventory();
        if (inv != null) {
            sb.append("  Name: ").append(nvl(inv.getItemName())).append("\n");
            sb.append("  Type: ").append(inv.getItemType() != null ? inv.getItemType() : "-").append("\n");
            sb.append("  Owner: ").append(nvl(inv.getOwnerName())).append("\n\n");
        } else {
            sb.append("  Inventory ID: ").append(rental.getInventoryId()).append("\n\n");
        }
        sb.append("RENTAL PERIOD\n");
        sb.append("  Start: ").append(formatDate(rental.getStartDate())).append("\n");
        sb.append("  End: ").append(formatDate(rental.getEndDate())).append("\n");
        sb.append("  Duration: ").append(rental.getTotalDays()).append(" days\n\n");
        sb.append("RENTER\n");
        sb.append("  Name: ").append(nvl(rental.getRenterName())).append("\n");
        sb.append("  Contact: ").append(nvl(rental.getRenterContact())).append("\n\n");
        sb.append("FINANCIALS\n");
        sb.append(String.format("  Daily Rate: %.2f TND\n", rental.getDailyRate()));
        sb.append(String.format("  Total: %.2f TND\n", rental.getTotalCost()));
        sb.append(String.format("  Late Fee: %.2f TND\n", rental.getLateFee()));
        sb.append("  Payment: ").append(rental.getPaymentStatus()).append("\n\n");
        sb.append("STATUS: ").append(rental.getRentalStatus()).append("\n");

        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false); ta.setWrapText(true);
        ta.setPrefRowCount(20); ta.setPrefColumnCount(55);
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setMinWidth(560);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────
    //  STATS & BANNER
    // ─────────────────────────────────────────────────────────────

    private void updateStats() {
        RentalService.RentalStatistics stats = rentalService.getStatistics();
        totalRentalsLabel.setText(String.valueOf(stats.totalRentals));
        activeRentalsLabel.setText(String.valueOf(stats.activeRentals));
        overdueRentalsLabel.setText(String.valueOf(stats.overdueRentals));
        long pendingCount = allRentals.stream()
                .filter(r -> r.getRentalStatus() == RentalStatus.PENDING).count();
        pendingRentalsLabel.setText(String.valueOf(pendingCount));
        totalRevenueLabel.setText(String.format("%.2f TND", stats.totalRevenue));
    }

    private void updateOverdueBanner() {
        int overdueCount = rentalService.getOverdueRentals().size();
        if (overdueCount > 0) {
            overdueWarningBanner.setVisible(true); overdueWarningBanner.setManaged(true);
            overdueCountLabel.setText("⚠️ " + overdueCount + " rental(s) are overdue!");
        } else {
            overdueWarningBanner.setVisible(false); overdueWarningBanner.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  DASHBOARD & NAVIGATION
    // ─────────────────────────────────────────────────────────────



    @FXML
    private void goToInventory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/InventoryView.fxml"));
            Parent root = loader.load();
            Scene scene = searchField.getScene();
            Stage stage = (Stage) scene.getWindow();
            scene.setRoot(root);
            scene.getStylesheets().clear();
            try {
                String inventoryCss = getClass()
                        .getResource("/styles/inventory-styles.css")
                        .toExternalForm();
                scene.getStylesheets().add(inventoryCss);
            } catch (Exception ignored) {
                // If the stylesheet isn't found we still show the view.
            }
            stage.setTitle("AgriRent - Inventory Management");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Inventory view.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  CALENDAR NAVIGATION (from Rentals header)
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleOpenCalendar() {
        // Preferred: go through the shared Router / MainLayoutController
        try {
            Router.go(Routes.CALENDAR);
            return;
        } catch (Exception ignored) {
            // If Router is not initialized (e.g. Rentals running standalone),
            // fall back to replacing the current Scene root.
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/inventory/CalendarView.fxml"));
            Parent calendarRoot = loader.load();
            Scene scene = searchField != null ? searchField.getScene() : null;
            if (scene == null) {
                return;
            }
            scene.setRoot(calendarRoot);
            // Use dedicated calendar styles if present
            try {
                String calendarCss = getClass()
                        .getResource("/styles/Calendar-styles.css")
                        .toExternalForm();
                if (!scene.getStylesheets().contains(calendarCss)) {
                    scene.getStylesheets().add(calendarCss);
                }
            } catch (Exception ignored) {
                // If calendar stylesheet is missing, view still works.
            }
            if (scene.getWindow() instanceof Stage stage) {
                stage.setTitle("AgriRent - Calendar");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open Calendar view.");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private void addDetailRow(GridPane grid, int row, String l1, String v1, String l2, String v2) {
        Label la1 = new Label(l1); la1.getStyleClass().add("card-detail-label");
        Label va1 = new Label(v1 != null ? v1 : "-"); va1.getStyleClass().add("card-detail-value");
        Label la2 = new Label(l2); la2.getStyleClass().add("card-detail-label");
        Label va2 = new Label(v2 != null ? v2 : "-"); va2.getStyleClass().add("card-detail-value");
        grid.add(la1, 0, row); grid.add(va1, 1, row);
        grid.add(la2, 2, row); grid.add(va2, 3, row);
    }

    private String formatDate(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "-";
    }

    private String getIconForItemType(Rental rental) {
        Inventory inv = rental.getInventory();
        if (inv != null && inv.getItemType() != null) {
            return switch (inv.getItemType()) {
                case EQUIPMENT -> "🚜"; case TOOL -> "🔧";
                case CONSUMABLE -> "🌱"; case STORAGE -> "📦"; default -> "📦";
            };
        }
        return "📦";
    }

    private HBox buildStars(int rating) {
        int safe = Math.max(1, Math.min(5, rating));
        HBox box = new HBox(2);
        for (int i = 1; i <= 5; i++) {
            Label s = new Label(i <= safe ? "⭐" : "☆");
            s.setStyle("-fx-font-size:14px;");
            box.getChildren().add(s);
        }
        return box;
    }

    private String nvl(String s) { return s != null ? s : "-"; }

    private void resetBorder(Control... controls) {
        for (Control c : controls) if (c != null) c.setStyle("");
    }

    private void setErrorBorder(Control c) {
        if (c != null) c.setStyle("-fx-border-color:#F44336;");
    }

    private void showSuccess(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Success"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private boolean showConfirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static class CompleteRentalData {
        String returnCondition;
        int rating;
    }
    @FXML
    private void showDashboard() throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/inventory/DashboardView.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Dashboard");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

