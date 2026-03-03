package tn.esprit.pidev.controllers;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.pidev.entities.Inventory;
import tn.esprit.pidev.entities.Rental;
import tn.esprit.pidev.entities.RentalStatus;
import tn.esprit.pidev.services.InventoryService;
import tn.esprit.pidev.services.RentalService;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CalendarController — Rental Availability Calendar.
 * Shows a monthly grid where each day is colour-coded:
 *   🟢 Green  = nothing booked that day
 *   🟠 Orange = 1 or more rentals active (partially booked)
 *   🔴 Red    = overdue rental(s) active
 *
 * Click any day to see the rentals active on that date in the side panel.
 * No SessionManager dependency.
 */
public class CalendarController implements Initializable {

    // ── Header ────────────────────────────────────────────────────
    @FXML private Label   monthYearLabel;
    @FXML private Button  prevMonthBtn;
    @FXML private Button  nextMonthBtn;
    @FXML private Button  todayBtn;
    @FXML private ComboBox<String> equipmentFilter;

    // ── Calendar Grid ─────────────────────────────────────────────
    @FXML private GridPane calendarGrid;

    // ── Side Panel ────────────────────────────────────────────────
    @FXML private Label    selectedDateLabel;
    @FXML private VBox     rentalDetailBox;
    @FXML private Label    noRentalsLabel;

    // ── Legend & Summary ──────────────────────────────────────────
    @FXML private Label    summaryActive;
    @FXML private Label    summaryOverdue;
    @FXML private Label    summaryPending;

    // ── State ─────────────────────────────────────────────────────
    private YearMonth currentMonth = YearMonth.now();
    private List<Rental>    allRentals    = new ArrayList<>();
    private List<Inventory> allInventory  = new ArrayList<>();
    private String          filterItem    = "All Equipment";

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");

    private final RentalService    rentalService    = new RentalService();
    private final InventoryService inventoryService = new InventoryService();

    // ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
        buildEquipmentFilter();
        renderCalendar();
        updateSummary();
        showDayDetail(LocalDate.now());
    }

    // ── DATA ──────────────────────────────────────────────────────
    private void loadData() {
        try {
            allRentals   = rentalService.getAllRentals();
            allInventory = inventoryService.getAllInventory();
        } catch (Exception e) {
            System.err.println("[Calendar] Data load error: " + e.getMessage());
        }
    }

    private void buildEquipmentFilter() {
        List<String> options = new ArrayList<>();
        options.add("All Equipment");
        allInventory.stream()
                .filter(Inventory::isRentable)
                .map(Inventory::getItemName)
                .sorted()
                .forEach(options::add);
        equipmentFilter.getItems().setAll(options);
        equipmentFilter.setValue("All Equipment");
        equipmentFilter.setOnAction(e -> {
            filterItem = equipmentFilter.getValue();
            renderCalendar();
        });
    }

    // ── NAVIGATION ────────────────────────────────────────────────
    @FXML private void goPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        renderCalendar();
        updateSummary();
    }

    @FXML private void goNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        renderCalendar();
        updateSummary();
    }

    @FXML private void goToday() {
        currentMonth = YearMonth.now();
        renderCalendar();
        updateSummary();
        showDayDetail(LocalDate.now());
    }

    // ── RENDER CALENDAR ───────────────────────────────────────────
    private void renderCalendar() {
        monthYearLabel.setText(currentMonth.format(MONTH_FMT));
        calendarGrid.getChildren().clear();

        // Day-of-week headers
        String[] headers = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(headers[i]);
            h.getStyleClass().add("cal-header");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            calendarGrid.add(h, i, 0);
        }

        // First day of month
        LocalDate firstDay  = currentMonth.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1; // Mon=0
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int col = startCol;
        int row = 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            List<Rental> dayRentals = getRentalsForDay(date);

            VBox cell = buildDayCell(date, dayRentals, today);
            calendarGrid.add(cell, col, row);

            col++;
            if (col > 6) { col = 0; row++; }
        }
    }

    private VBox buildDayCell(LocalDate date, List<Rental> dayRentals, LocalDate today) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6, 6, 6, 6));
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setMinHeight(72);
        cell.setMaxWidth(Double.MAX_VALUE);

        // Day number label
        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.getStyleClass().add("cal-day-num");

        boolean isToday    = date.equals(today);
        boolean hasOverdue = dayRentals.stream().anyMatch(r ->
                r.getRentalStatus() == RentalStatus.ACTIVE && date.isAfter(r.getEndDate()));
        boolean hasActive  = !dayRentals.isEmpty();

        // Cell colour
        if (isToday) {
            cell.getStyleClass().add("cal-day-today");
        } else if (hasOverdue) {
            cell.getStyleClass().add("cal-day-overdue");
        } else if (hasActive) {
            cell.getStyleClass().add("cal-day-booked");
        } else {
            cell.getStyleClass().add("cal-day-free");
        }

        cell.getChildren().add(dayNum);

        // Rental chips (max 2)
        int shown = 0;
        for (Rental r : dayRentals) {
            if (shown >= 2) {
                Label more = new Label("+" + (dayRentals.size() - 2) + " more");
                more.getStyleClass().add("cal-chip-more");
                cell.getChildren().add(more);
                break;
            }
            String name = r.getInventory() != null
                    ? shorten(r.getInventory().getItemName(), 14) : "Rental #" + r.getRentalId();
            Label chip = new Label(name);
            chip.getStyleClass().add(hasOverdue ? "cal-chip-red" : "cal-chip-green");
            chip.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(chip);
            shown++;
        }

        // Click handler
        LocalDate clickDate = date;
        cell.setOnMouseClicked(e -> showDayDetail(clickDate));
        cell.setStyle(cell.getStyle() + " -fx-cursor: hand;");

        return cell;
    }

    // ── SIDE PANEL ────────────────────────────────────────────────
    private void showDayDetail(LocalDate date) {
        selectedDateLabel.setText(date.format(DAY_FMT));
        rentalDetailBox.getChildren().clear();

        List<Rental> dayRentals = getRentalsForDay(date);

        if (dayRentals.isEmpty()) {
            noRentalsLabel.setVisible(true);
            noRentalsLabel.setManaged(true);
        } else {
            noRentalsLabel.setVisible(false);
            noRentalsLabel.setManaged(false);

            for (Rental r : dayRentals) {
                VBox card = buildRentalCard(r, date);
                rentalDetailBox.getChildren().add(card);
            }
        }
    }

    private VBox buildRentalCard(Rental r, LocalDate viewDate) {
        VBox card = new VBox(6);
        card.getStyleClass().add("detail-card");

        String itemName = r.getInventory() != null
                ? r.getInventory().getItemName() : "Equipment #" + r.getInventoryId();

        boolean isOverdue = r.getRentalStatus() == RentalStatus.ACTIVE
                && viewDate.isAfter(r.getEndDate());

        // Header row
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(isOverdue ? "🔴" : "🟢");
        icon.setStyle("-fx-font-size: 14px;");
        Label name = new Label(itemName);
        name.getStyleClass().add("detail-item-name");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(r.getRentalStatus().name());
        status.getStyleClass().add(isOverdue ? "detail-badge-red" : "detail-badge-green");
        header.getChildren().addAll(icon, name, spacer, status);

        // Details
        Label renter = new Label("👤  " + r.getRenterName() + "  ·  " + r.getRenterContact());
        renter.getStyleClass().add("detail-row");
        Label dates = new Label("📅  " + fmt(r.getStartDate()) + "  →  " + fmt(r.getEndDate()));
        dates.getStyleClass().add("detail-row");
        Label cost = new Label("💰  " + String.format("%.0f TND", r.getTotalCost())
                + "   (" + r.getTotalDays() + " days @ " + String.format("%.0f", r.getDailyRate()) + " TND/day)");
        cost.getStyleClass().add("detail-row");

        if (isOverdue) {
            long lateDays = java.time.temporal.ChronoUnit.DAYS.between(r.getEndDate(), viewDate);
            Label late = new Label("⚠  Overdue by " + lateDays + " day(s) — Late fee: "
                    + String.format("%.0f TND", r.getDailyRate() * lateDays * 1.5));
            late.getStyleClass().add("detail-row-danger");
            card.getChildren().addAll(header, renter, dates, cost, late);
        } else {
            long remaining = java.time.temporal.ChronoUnit.DAYS.between(viewDate, r.getEndDate());
            Label rem = new Label("⏳  " + remaining + " day(s) remaining");
            rem.getStyleClass().add("detail-row-info");
            card.getChildren().addAll(header, renter, dates, cost, rem);
        }

        return card;
    }

    // ── SUMMARY ───────────────────────────────────────────────────
    private void updateSummary() {
        try {
            LocalDate first = currentMonth.atDay(1);
            LocalDate last  = currentMonth.atEndOfMonth();

            long active  = allRentals.stream().filter(r ->
                    r.getStartDate() != null && r.getEndDate() != null &&
                            !r.getStartDate().isAfter(last) && !r.getEndDate().isBefore(first) &&
                            r.getRentalStatus() == RentalStatus.ACTIVE).count();

            long overdue = allRentals.stream().filter(r ->
                    r.getRentalStatus() == RentalStatus.ACTIVE &&
                            r.getEndDate() != null && r.getEndDate().isBefore(LocalDate.now())).count();

            long pending = allRentals.stream().filter(r ->
                    r.getRentalStatus() == RentalStatus.PENDING).count();

            summaryActive.setText(String.valueOf(active));
            summaryOverdue.setText(String.valueOf(overdue));
            summaryPending.setText(String.valueOf(pending));
        } catch (Exception e) {
            System.err.println("[Calendar] Summary error: " + e.getMessage());
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private List<Rental> getRentalsForDay(LocalDate date) {
        return allRentals.stream()
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
                .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                .filter(r -> r.getRentalStatus() == RentalStatus.ACTIVE
                        || r.getRentalStatus() == RentalStatus.APPROVED
                        || r.getRentalStatus() == RentalStatus.PENDING)
                .filter(r -> {
                    if ("All Equipment".equals(filterItem)) return true;
                    return r.getInventory() != null &&
                            filterItem.equals(r.getInventory().getItemName());
                })
                .collect(Collectors.toList());
    }

    private String fmt(LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}