package tn.esprit.crop.controller;

import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.crop.entity.Task;
import tn.esprit.crop.service.TaskService;

public class AddTaskController {

    @FXML private TextField txtCropId;
    @FXML private TextField txtTaskName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtTaskType;
    @FXML private DatePicker dpScheduledDate;
    @FXML private DatePicker dpCompletedDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextField txtAssignedTo;
    @FXML private TextField txtCost;
    @FXML
    private Label lblTitle;

    @FXML
    private Button btnSave;

    private TaskService taskService = new TaskService();
    private Task currentTask = null;

    @FXML
    public void initialize() {
        cbStatus.getItems().addAll(
                "pending",
                "in_progress",
                "completed",
                "cancelled"
        );
        cbStatus.setValue("pending");
    }

    @FXML
    private void handleAddTask() {

        // 🔥 Validate FIRST
        if (!validateTaskInputs()) {
            return;
        }

        try {

            int cropId = Integer.parseInt(txtCropId.getText().trim());
            String name = txtTaskName.getText().trim();
            String description = txtDescription.getText().trim();
            String type = txtTaskType.getText().trim();
            String status = cbStatus.getValue();
            String assigned = txtAssignedTo.getText().trim();
            double cost = Double.parseDouble(txtCost.getText().trim());

            if (currentTask == null) {

                // 🔥 ADD MODE
                Task task = new Task(
                        cropId,
                        name,
                        description,
                        type,
                        dpScheduledDate.getValue(),
                        dpCompletedDate.getValue(),
                        status,
                        assigned,
                        cost
                );

                taskService.addTask(task);

            } else {

                // 🔥 UPDATE MODE
                currentTask.setCropId(cropId);
                currentTask.setTaskName(name);
                currentTask.setDescription(description);
                currentTask.setTaskType(type);
                currentTask.setScheduledDate(dpScheduledDate.getValue());
                currentTask.setCompletedDate(dpCompletedDate.getValue());
                currentTask.setStatus(status);
                currentTask.setAssignedTo(assigned);
                currentTask.setCost(cost);

                taskService.updateTask(currentTask);
            }

            showSuccess("Task saved successfully.");

            Stage stage = (Stage) txtTaskName.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            showAlert("Error while saving task: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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



    public void setTaskForEdit(Task task) {

        this.currentTask = task;

        txtCropId.setText(String.valueOf(task.getCropId()));
        txtTaskName.setText(task.getTaskName());
        txtDescription.setText(task.getDescription());
        txtTaskType.setText(task.getTaskType());
        dpScheduledDate.setValue(task.getScheduledDate());
        dpCompletedDate.setValue(task.getCompletedDate());
        cbStatus.setValue(task.getStatus());
        txtAssignedTo.setText(task.getAssignedTo());
        txtCost.setText(String.valueOf(task.getCost()));

        // 🔥 Change UI to EDIT mode
        lblTitle.setText("Edit Task");
        btnSave.setText("Update Task");
    }
    private boolean validateTaskInputs() {

        // Crop ID
        if (txtCropId.getText().trim().isEmpty()) {
            showAlert("Crop ID cannot be empty.");
            return false;
        }

        try {
            int cropId = Integer.parseInt(txtCropId.getText().trim());
            if (cropId <= 0) {
                showAlert("Crop ID must be greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Crop ID must be a valid number.");
            return false;
        }

        // Task Name
        if (txtTaskName.getText().trim().isEmpty()) {
            showAlert("Task name cannot be empty.");
            return false;
        }

        // Description
        if (txtDescription.getText().trim().isEmpty()) {
            showAlert("Description cannot be empty.");
            return false;
        }

        // Task Type
        if (txtTaskType.getText().trim().isEmpty()) {
            showAlert("Task type cannot be empty.");
            return false;
        }

        // Scheduled Date
        if (dpScheduledDate.getValue() == null) {
            showAlert("Please select a scheduled date.");
            return false;
        }

        // Status must be selected
        if (cbStatus.getValue() == null) {
            showAlert("Please select a status.");
            return false;
        }

        // Completed Date Logic
        String status = cbStatus.getValue();

        if ("completed".equals(status)) {

            if (dpCompletedDate.getValue() == null) {
                showAlert("Completed date is required when status is completed.");
                return false;
            }

            if (dpCompletedDate.getValue()
                    .isBefore(dpScheduledDate.getValue())) {
                showAlert("Completed date cannot be before scheduled date.");
                return false;
            }

        } else {

            if (dpCompletedDate.getValue() != null) {
                showAlert("Completed date should only be set when status is completed.");
                return false;
            }
        }

        // Assigned To
        if (txtAssignedTo.getText().trim().isEmpty()) {
            showAlert("Assigned To cannot be empty.");
            return false;
        }

        // Cost
        try {
            double cost = Double.parseDouble(txtCost.getText().trim());
            if (cost < 0) {
                showAlert("Cost cannot be negative.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Cost must be a valid number.");
            return false;
        }

        return true;
    }
    public void setCropId(int cropId) {
        txtCropId.setText(String.valueOf(cropId));
        txtCropId.setDisable(true);
    }




}
