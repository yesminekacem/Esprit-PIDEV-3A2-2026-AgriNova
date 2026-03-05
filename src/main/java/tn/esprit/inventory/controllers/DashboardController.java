package tn.esprit.inventory.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import tn.esprit.inventory.entities.ConditionStatus;
import tn.esprit.inventory.entities.Inventory;
import tn.esprit.inventory.entities.ItemType;
import tn.esprit.inventory.services.InventoryService;
import tn.esprit.inventory.services.RentalService;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label greetingLabel, dateLabel, roleBadge;
    @FXML private Label kpiTotalItems, kpiTotalValue, kpiAvailable, kpiRentable, kpiActiveRentals, kpiOverdue, kpiRevenue, kpiCompleted;
    @FXML private PieChart inventoryPieChart;
    @FXML private BarChart<String, Number> rentalBarChart;
    @FXML private ListView<String> maintenanceList;
    @FXML private Label maintenanceCount;
    @FXML private ProgressBar pbExcellent, pbGood, pbFair, pbPoor, pbRentedOut;
    @FXML private Label lblExcellent, lblGood, lblFair, lblPoor, rentedOutPct;

    private final InventoryService inventoryService = new InventoryService();
    private final RentalService rentalService = new RentalService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(() -> {
            loadHeader();
            loadKPIs();
            loadPieChart();
            loadBarChart();
            loadMaintenanceAlerts();
            loadConditionBars();
        });
    }

    private void loadHeader() {
        SessionManager s = SessionManager.getInstance();
        String icon = (s.isLoggedIn() && s.isAdmin()) ? "👑" : "🌾";
        greetingLabel.setText("Welcome, " + (s.isLoggedIn() ? s.getCurrentUserName() : "Guest") + " " + icon);
        roleBadge.setText(s.isLoggedIn() ? s.getCurrentUser().getRole().name() : "GUEST");
        roleBadge.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 3 10;");
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
    }

    private void loadKPIs() {
        var inv = inventoryService.getStatistics();
        var ren = rentalService.getStatistics();
        kpiTotalItems.setText(String.valueOf(inv.totalItems));
        kpiTotalValue.setText(String.format("%.0f TND", inv.totalValue));
        kpiAvailable.setText(String.valueOf(inv.availableForRent));
        kpiRentable.setText("Rentable: " + inv.rentableItems);
        kpiActiveRentals.setText(String.valueOf(ren.activeRentals));
        kpiOverdue.setText("⚠ Overdue: " + ren.overdueRentals);
        kpiRevenue.setText(String.format("%.0f TND", ren.totalRevenue));
        kpiCompleted.setText("Completed: " + ren.completedRentals);
    }

    private void loadPieChart() {
        List<Inventory> all = inventoryService.getAllInventory();
        if (all == null || all.isEmpty()) return;

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        data.add(new PieChart.Data("Equipment", all.stream().filter(i -> i.getItemType() == ItemType.EQUIPMENT).count()));
        data.add(new PieChart.Data("Tools", all.stream().filter(i -> i.getItemType() == ItemType.TOOL).count()));
        inventoryPieChart.setData(data);
    }

    private void loadBarChart() {
        var series = new BarChart.Series<String, Number>();
        series.getData().add(new BarChart.Data<>("Active", rentalService.getStatistics().activeRentals));
        rentalBarChart.getData().setAll(series);
    }

    private void loadMaintenanceAlerts() {
        List<Inventory> alerts = inventoryService.getMaintenanceAlerts();
        maintenanceCount.setText(String.valueOf(alerts.size()));
        maintenanceList.setItems(FXCollections.observableArrayList(alerts.stream().map(Inventory::getItemName).collect(Collectors.toList())));
    }

    private void loadConditionBars() {
        List<Inventory> all = inventoryService.getAllInventory();
        if (all == null || all.isEmpty()) return;
        double total = all.size();

        pbExcellent.setProgress(all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.EXCELLENT).count() / total);
        pbGood.setProgress(all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.GOOD).count() / total);
        pbFair.setProgress(all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.FAIR).count() / total);
        pbPoor.setProgress(all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.POOR).count() / total);

        lblExcellent.setText(String.valueOf((int)all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.EXCELLENT).count()));
        lblGood.setText(String.valueOf((int)all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.GOOD).count()));
        lblFair.setText(String.valueOf((int)all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.FAIR).count()));
        lblPoor.setText(String.valueOf((int)all.stream().filter(i -> i.getConditionStatus() == ConditionStatus.POOR).count()));
    }
}