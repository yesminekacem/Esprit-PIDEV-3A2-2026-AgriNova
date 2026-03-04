package tn.esprit.inventory.controllers;
import tn.esprit.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import tn.esprit.inventory.entities.ConditionStatus;
import tn.esprit.inventory.entities.Inventory;
import tn.esprit.inventory.entities.ItemType;
import tn.esprit.inventory.services.InventoryService;
import tn.esprit.inventory.services.RentalService;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.Collectors;

/**
 * DashboardController — Live statistics dashboard.
 * NO SessionManager dependency — works standalone.
 */
public class DashboardController implements Initializable {

    // ── Header ────────────────────────────────────────────────────
    @FXML private Label greetingLabel;
    @FXML private Label dateLabel;
    @FXML private Label roleBadge;

    // ── KPI Cards ─────────────────────────────────────────────────
    @FXML private Label kpiTotalItems;
    @FXML private Label kpiTotalValue;
    @FXML private Label kpiAvailable;
    @FXML private Label kpiRentable;
    @FXML private Label kpiActiveRentals;
    @FXML private Label kpiOverdue;
    @FXML private Label kpiRevenue;
    @FXML private Label kpiCompleted;

    // ── Charts ────────────────────────────────────────────────────
    @FXML private PieChart inventoryPieChart;
    @FXML private BarChart<String, Number> rentalBarChart;

    // ── Maintenance List ──────────────────────────────────────────
    @FXML private ListView<String> maintenanceList;
    @FXML private Label maintenanceCount;

    // ── Condition Progress Bars ───────────────────────────────────
    @FXML private ProgressBar pbExcellent;
    @FXML private ProgressBar pbGood;
    @FXML private ProgressBar pbFair;
    @FXML private ProgressBar pbPoor;
    @FXML private ProgressBar pbRentedOut;
    @FXML private Label lblExcellent;
    @FXML private Label lblGood;
    @FXML private Label lblFair;
    @FXML private Label lblPoor;
    @FXML private Label rentedOutPct;

    private final InventoryService inventoryService = new InventoryService();
    private final RentalService    rentalService    = new RentalService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadHeader();
        loadKPIs();
        loadPieChart();
        loadBarChart();
        loadMaintenanceAlerts();
        loadConditionBars();
    }

    private void loadHeader() {
        SessionManager s = SessionManager.getInstance();
        if (s.isLoggedIn()) {
            String icon = s.isAdmin() ? "👑" : "🌾";
            greetingLabel.setText("Welcome, " + s.getCurrentUserName() + " " + icon);
            roleBadge.setText(s.getCurrentUser().getRole().name());
        } else {
            greetingLabel.setText("AgriRent Dashboard 🌾");
            roleBadge.setText("INVENTORY");
        }
        roleBadge.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-padding: 3 10 3 10;" +
                        "-fx-font-weight: bold; -fx-font-size: 11px;");
        dateLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
    }

    private void loadKPIs() {
        try {
            InventoryService.InventoryStatistics inv = inventoryService.getStatistics();
            RentalService.RentalStatistics       ren = rentalService.getStatistics();

            kpiTotalItems.setText(String.valueOf(inv.totalItems));
            kpiTotalValue.setText(String.format("Value: %.0f TND", inv.totalValue));

            kpiAvailable.setText(String.valueOf(inv.availableForRent));
            kpiRentable.setText("Rentable: " + inv.rentableItems);

            kpiActiveRentals.setText(String.valueOf(ren.activeRentals));
            kpiOverdue.setText("⚠ Overdue: " + ren.overdueRentals);

            kpiRevenue.setText(String.format("%.0f TND", ren.totalRevenue));
            kpiCompleted.setText("Completed: " + ren.completedRentals);

        } catch (Exception e) {
            System.err.println("[Dashboard] KPI error: " + e.getMessage());
        }
    }

    private void loadPieChart() {
        try {
            List<Inventory> all = inventoryService.getAllInventory();
            if (all.isEmpty()) return;

            long equipment  = all.stream().filter(i -> i.getItemType() == ItemType.EQUIPMENT).count();
            long tool       = all.stream().filter(i -> i.getItemType() == ItemType.TOOL).count();
            long consumable = all.stream().filter(i -> i.getItemType() == ItemType.CONSUMABLE).count();
            long storage    = all.stream().filter(i -> i.getItemType() == ItemType.STORAGE).count();

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            if (equipment  > 0) data.add(new PieChart.Data("Equipment ("  + equipment  + ")", equipment));
            if (tool       > 0) data.add(new PieChart.Data("Tools ("      + tool       + ")", tool));
            if (consumable > 0) data.add(new PieChart.Data("Consumables (" + consumable + ")", consumable));
            if (storage    > 0) data.add(new PieChart.Data("Storage ("    + storage    + ")", storage));

            inventoryPieChart.setData(data);

            String[] colours = {"#2E7D32", "#0277BD", "#E65100", "#7B1FA2"};
            for (int i = 0; i < data.size(); i++) {
                String col = colours[i % colours.length];
                data.get(i).getNode().setStyle("-fx-pie-color: " + col + ";");
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Pie error: " + e.getMessage());
        }
    }

    private void loadBarChart() {
        try {
            RentalService.RentalStatistics ren = rentalService.getStatistics();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rentals");
            series.getData().add(new XYChart.Data<>("Pending",   countByStatus("PENDING")));
            series.getData().add(new XYChart.Data<>("Active",    ren.activeRentals));
            series.getData().add(new XYChart.Data<>("Completed", ren.completedRentals));
            series.getData().add(new XYChart.Data<>("Overdue",   ren.overdueRentals));
            series.getData().add(new XYChart.Data<>("Cancelled", countByStatus("CANCELLED")));

            rentalBarChart.getData().clear();
            rentalBarChart.getData().add(series);

            // colour bars after nodes are created
            rentalBarChart.getData().get(0).getData().forEach(d -> {
                if (d.getNode() != null) applyBarColour(d);
            });
            rentalBarChart.sceneProperty().addListener((obs, o, n) -> {
                if (n != null) colourBars(series);
            });
            colourBars(series);

        } catch (Exception e) {
            System.err.println("[Dashboard] Bar error: " + e.getMessage());
        }
    }



    private void colourBars(@org.jetbrains.annotations.NotNull XYChart.Series<String, Number> series) {
        AtomicReferenceArray<String> cols = new AtomicReferenceArray<>(new String[]{"#FF9800", "#2E7D32", "#9E9E9E", "#F44336", "#607D8B"});
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> d = series.getData().get(i);
            if (d.getNode() != null)
                d.getNode().setStyle("-fx-bar-fill: " + cols.get(i % cols.length()) + ";");
        }
    }

    private void applyBarColour(XYChart.Data<String, Number> d) {
        String[] cols = {"#FF9800", "#2E7D32", "#9E9E9E", "#F44336", "#607D8B"};
        // fallback — will be overridden by colourBars
        if (d.getNode() != null)
            d.getNode().setStyle("-fx-bar-fill: #2E7D32;");
    }

    private int countByStatus(String status) {
        try {
            return (int) rentalService.getAllRentals().stream()
                    .filter(r -> r.getRentalStatus().name().equals(status))
                    .count();
        } catch (Exception e) { return 0; }
    }

    private void loadMaintenanceAlerts() {
        try {
            List<Inventory> alerts = inventoryService.getMaintenanceAlerts();
            maintenanceCount.setText(alerts.size() + " item" + (alerts.size() != 1 ? "s" : ""));

            if (alerts.isEmpty()) {
                maintenanceList.setItems(FXCollections.observableArrayList(
                        "✅  All equipment is up to date!"));
            } else {
                ObservableList<String> rows = FXCollections.observableArrayList(
                        alerts.stream().map(i -> {
                            String next = i.getNextMaintenanceDate() != null
                                    ? i.getNextMaintenanceDate().toString() : "—";
                            return "⚠  " + i.getItemName()
                                    + "  [" + i.getConditionStatus() + "]"
                                    + "  — Due: " + next;
                        }).collect(Collectors.toList()));
                maintenanceList.setItems(rows);
            }
        } catch (Exception e) {
            System.err.println("[Dashboard] Maintenance error: " + e.getMessage());
        }
    }

    private void loadConditionBars() {
        try {
            List<Inventory> all = inventoryService.getAllInventory();
            if (all.isEmpty()) return;

            int total     = all.size();
            int excellent = (int) all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.EXCELLENT).count();
            int good      = (int) all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.GOOD).count();
            int fair      = (int) all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.FAIR).count();
            int poor      = (int) all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.POOR).count();
            int rented    = (int) all.stream().filter(i ->
                    i.getRentalStatus() != null &&
                            i.getRentalStatus().name().equals("RENTED_OUT")).count();

            pbExcellent.setProgress((double) excellent / total);
            pbGood.setProgress((double) good      / total);
            pbFair.setProgress((double) fair      / total);
            pbPoor.setProgress((double) poor      / total);
            pbRentedOut.setProgress((double) rented / total);

            lblExcellent.setText(excellent + " items");
            lblGood.setText(good      + " items");
            lblFair.setText(fair      + " items");
            lblPoor.setText(poor      + " items");
            rentedOutPct.setText(String.format("%.0f%%", (double) rented / total * 100));

        } catch (Exception e) {
            System.err.println("[Dashboard] Condition bars error: " + e.getMessage());
        }
    }
}