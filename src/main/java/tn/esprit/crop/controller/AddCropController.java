package tn.esprit.crop.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AddCropController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField typeField;
    @FXML private TextField varietyField;
    @FXML private DatePicker plantingDatePicker;
    @FXML private DatePicker harvestDatePicker;
    @FXML private TextField growthStageField;
    @FXML private TextField areaField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label lblTitle;
    @FXML private Label imageLabel;

    private String selectedImagePath ;
    private Crop currentCrop;

    private final CropDAO cropDAO = new CropDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        statusCombo.getItems().addAll(
                "planned", "active", "harvested", "failed"
        );

        statusCombo.setValue("planned");
    }

    // 🔥 Handle Image Selection
    @FXML
    private void handleChooseImage() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Crop Image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            imageLabel.setText(file.getName());
        }
    }

    // 🔥 Edit Mode
    public void setCrop(Crop crop) {

        this.currentCrop = crop;

        nameField.setText(crop.getName());
        typeField.setText(crop.getType());
        varietyField.setText(crop.getVariety());
        plantingDatePicker.setValue(crop.getPlantingDate());
        harvestDatePicker.setValue(crop.getExpectedHarvestDate());
        growthStageField.setText(crop.getGrowthStage());
        areaField.setText(String.valueOf(crop.getAreaSize()));
        statusCombo.setValue(crop.getStatus());

        selectedImagePath = crop.getImagePath();
        imageLabel.setText(
                crop.getImagePath() != null ? crop.getImagePath() : "No image selected"
        );

        lblTitle.setText("Edit Crop");
    }

    @FXML
    private void handleSave(ActionEvent event) {

        if (!validateCropInputs()) {
            return;
        }

        try {

            String finalImagePath =
                    selectedImagePath != null ? selectedImagePath :
                            (currentCrop != null ? currentCrop.getImagePath() : "/images/default.png");

            Crop crop = new Crop(
                    currentCrop == null ? 0 : currentCrop.getCropId(),
                    nameField.getText().trim(),
                    typeField.getText().trim(),
                    varietyField.getText().trim(),
                    plantingDatePicker.getValue(),
                    harvestDatePicker.getValue(),
                    growthStageField.getText().trim(),
                    Double.parseDouble(areaField.getText().trim()),
                    statusCombo.getValue(),
                    finalImagePath
            );

            if (currentCrop == null) {
                cropDAO.insertCrop(crop);
            } else {
                cropDAO.updateCrop(crop);
            }

            showSuccess("Crop saved successfully.");
            ((Stage) nameField.getScene().getWindow()).close();

        } catch (Exception e) {
            showAlert("Error while saving crop: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    // 🔥 Validation
    private boolean validateCropInputs() {

        if (nameField.getText().trim().isEmpty()) {
            showAlert("Name cannot be empty.");
            return false;
        }

        if (typeField.getText().trim().isEmpty()) {
            showAlert("Type cannot be empty.");
            return false;
        }

        if (varietyField.getText().trim().isEmpty()) {
            showAlert("Variety cannot be empty.");
            return false;
        }

        if (growthStageField.getText().trim().isEmpty()) {
            showAlert("Growth stage cannot be empty.");
            return false;
        }

        if (plantingDatePicker.getValue() == null) {
            showAlert("Please select a planting date.");
            return false;
        }

        if (harvestDatePicker.getValue() == null) {
            showAlert("Please select expected harvest date.");
            return false;
        }

        if (harvestDatePicker.getValue().isBefore(plantingDatePicker.getValue())) {
            showAlert("Harvest date must be after planting date.");
            return false;
        }

        try {
            double area = Double.parseDouble(areaField.getText().trim());
            if (area <= 0) {
                showAlert("Area size must be greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Area size must be a valid number.");
            return false;
        }

        if (statusCombo.getValue() == null) {
            showAlert("Please select a status.");
            return false;
        }

        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
