package tn.esprit.utils;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import tn.esprit.inventory.entities.*;

import java.time.LocalDate;
import java.util.Optional;

public class DialogUtils {

    private static final String CSS_PATH = "/styles/inventory-style.css";

    private static void applyCss(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(DialogUtils.class.getResource(CSS_PATH).toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
    }

    private static void applyButtonStyle(Dialog<?> dialog, ButtonType... buttons) {
        for (ButtonType btnType : buttons) {
            Button btn = (Button) dialog.getDialogPane().lookupButton(btnType);
            if (btn != null) {
                btn.getStyleClass().add("button");
            }
        }
    }

    public static Inventory showAddInventoryDialog() {
        Dialog<Inventory> dialog = new Dialog<>();
        dialog.setTitle("Add New Equipment");
        dialog.setHeaderText("Enter equipment details");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        applyCss(dialog);
        applyButtonStyle(dialog, addButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField(); nameField.setPromptText("Equipment Name");
        ComboBox<ItemType> typeCombo = new ComboBox<>(); typeCombo.getItems().addAll(ItemType.values()); typeCombo.setPromptText("Select Type");
        TextArea descArea = new TextArea(); descArea.setPromptText("Description"); descArea.setPrefRowCount(3);
        TextField quantityField = new TextField(); quantityField.setPromptText("Quantity");
        TextField priceField = new TextField(); priceField.setPromptText("Unit Price (TND)");
        CheckBox rentableCheck = new CheckBox("Available for Rent");
        TextField rentalPriceField = new TextField(); rentalPriceField.setPromptText("Rental Price per Day"); rentalPriceField.setDisable(true);
        rentableCheck.selectedProperty().addListener((obs, old, newVal) -> rentalPriceField.setDisable(!newVal));
        ComboBox<ConditionStatus> conditionCombo = new ComboBox<>(); conditionCombo.getItems().addAll(ConditionStatus.values()); conditionCombo.setValue(ConditionStatus.GOOD);
        TextField ownerField = new TextField(); ownerField.setPromptText("Owner Name");
        TextField contactField = new TextField(); contactField.setPromptText("Owner Contact");

        grid.add(new Label("Name:*"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Type:*"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Description:"), 0, 2); grid.add(descArea, 1, 2);
        grid.add(new Label("Quantity:*"), 0, 3); grid.add(quantityField, 1, 3);
        grid.add(new Label("Unit Price:*"), 0, 4); grid.add(priceField, 1, 4);
        grid.add(rentableCheck, 0, 5, 2, 1);
        grid.add(new Label("Rental Price/Day:"), 0, 6); grid.add(rentalPriceField, 1, 6);
        grid.add(new Label("Condition:"), 0, 7); grid.add(conditionCombo, 1, 7);
        grid.add(new Label("Owner:"), 0, 8); grid.add(ownerField, 1, 8);
        grid.add(new Label("Contact:"), 0, 9); grid.add(contactField, 1, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    Inventory inv = new Inventory();
                    inv.setItemName(nameField.getText());
                    inv.setItemType(typeCombo.getValue());
                    inv.setDescription(descArea.getText());
                    inv.setQuantity(Integer.parseInt(quantityField.getText()));
                    inv.setUnitPrice(Double.parseDouble(priceField.getText()));
                    inv.setRentable(rentableCheck.isSelected());
                    if (rentableCheck.isSelected()) {
                        inv.setRentalPricePerDay(Double.parseDouble(rentalPriceField.getText()));
                    }
                    inv.setConditionStatus(conditionCombo.getValue());
                    inv.setOwnerName(ownerField.getText());
                    inv.setOwnerContact(contactField.getText());
                    inv.setPurchaseDate(LocalDate.now());
                    inv.setRentalStatus(InventoryRentalStatus.AVAILABLE);
                    return inv;
                } catch (Exception e) {
                    showError("Invalid input: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    public static Inventory showEditInventoryDialog(Inventory item) {
        if (item == null) return null;

        Dialog<Inventory> dialog = new Dialog<>();
        dialog.setTitle("Edit Equipment");
        dialog.setHeaderText("Update equipment details");
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        applyCss(dialog);
        applyButtonStyle(dialog, saveButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(item.getItemName());
        ComboBox<ItemType> typeCombo = new ComboBox<>(); typeCombo.getItems().addAll(ItemType.values()); typeCombo.setValue(item.getItemType());
        TextArea descArea = new TextArea(item.getDescription()); descArea.setPrefRowCount(3);
        TextField quantityField = new TextField(String.valueOf(item.getQuantity()));
        TextField priceField = new TextField(String.valueOf(item.getUnitPrice()));
        CheckBox rentableCheck = new CheckBox("Available for Rent"); rentableCheck.setSelected(item.isRentable());
        TextField rentalPriceField = new TextField(String.valueOf(item.getRentalPricePerDay())); rentalPriceField.setDisable(!item.isRentable());
        rentableCheck.selectedProperty().addListener((obs, old, newVal) -> rentalPriceField.setDisable(!newVal));
        ComboBox<ConditionStatus> conditionCombo = new ComboBox<>(); conditionCombo.getItems().addAll(ConditionStatus.values()); conditionCombo.setValue(item.getConditionStatus() != null ? item.getConditionStatus() : ConditionStatus.GOOD);
        TextField ownerField = new TextField(item.getOwnerName());
        TextField contactField = new TextField(item.getOwnerContact());

        grid.add(new Label("Name:*"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Type:*"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Description:"), 0, 2); grid.add(descArea, 1, 2);
        grid.add(new Label("Quantity:*"), 0, 3); grid.add(quantityField, 1, 3);
        grid.add(new Label("Unit Price:*"), 0, 4); grid.add(priceField, 1, 4);
        grid.add(rentableCheck, 0, 5, 2, 1);
        grid.add(new Label("Rental Price/Day:"), 0, 6); grid.add(rentalPriceField, 1, 6);
        grid.add(new Label("Condition:"), 0, 7); grid.add(conditionCombo, 1, 7);
        grid.add(new Label("Owner:"), 0, 8); grid.add(ownerField, 1, 8);
        grid.add(new Label("Contact:"), 0, 9); grid.add(contactField, 1, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    item.setItemName(nameField.getText());
                    item.setItemType(typeCombo.getValue());
                    item.setDescription(descArea.getText());
                    item.setQuantity(Integer.parseInt(quantityField.getText()));
                    item.setUnitPrice(Double.parseDouble(priceField.getText()));
                    item.setRentable(rentableCheck.isSelected());
                    if (rentableCheck.isSelected()) {
                        item.setRentalPricePerDay(Double.parseDouble(rentalPriceField.getText()));
                    } else {
                        item.setRentalPricePerDay(0);
                    }
                    item.setConditionStatus(conditionCombo.getValue());
                    item.setOwnerName(ownerField.getText());
                    item.setOwnerContact(contactField.getText());
                    return item;
                } catch (Exception e) {
                    showError("Invalid input: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    public static void showInventoryDetailsDialog(Inventory item) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Equipment Details");
        alert.setHeaderText(item.getItemName());
        applyCss(alert);

        String details = String.format("""
            Type: %s
            Quantity: %d
            Unit Price: %.2f TND
            Condition: %s
            Status: %s
            Rentable: %s
            %s
            Owner: %s
            Contact: %s
            """,
                item.getItemType(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getConditionStatus(),
                item.getRentalStatus(),
                item.isRentable() ? "Yes" : "No",
                item.isRentable() ? "Rental Price: " + item.getRentalPricePerDay() + " TND/day" : "",
                item.getOwnerName(),
                item.getOwnerContact()
        );

        alert.setContentText(details);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyCss(alert);
        return alert.showAndWait().filter(btn -> btn == ButtonType.OK).isPresent();
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyCss(alert);
        alert.showAndWait();
    }

    public static void showInfo(String s, String s1) {
    }
}
