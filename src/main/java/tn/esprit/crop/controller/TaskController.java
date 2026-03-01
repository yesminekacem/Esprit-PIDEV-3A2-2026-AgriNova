package tn.esprit.crop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import tn.esprit.crop.entity.AITaskDTO;
import tn.esprit.crop.entity.Task;
import tn.esprit.crop.entity.Crop;
import tn.esprit.crop.service.AIService;
import tn.esprit.crop.service.TaskService;
import tn.esprit.crop.service.CropService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

public class TaskController {

    @FXML private VBox pendingBox;
    @FXML private VBox progressBox;
    @FXML private VBox completedBox;
    @FXML private VBox cancelledBox;
    @FXML private TextField searchField;

    private final TaskService taskService = new TaskService();
    private final CropService cropService = new CropService();
    private final AIService aiService = new AIService();

    @FXML
    public void initialize() {
        loadTasks();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTasks(newVal);
        });

        setupDragAndDrop(pendingBox, "pending");
        setupDragAndDrop(progressBox, "in_progress");
        setupDragAndDrop(completedBox, "completed");
        setupDragAndDrop(cancelledBox, "cancelled");
    }

    // ================= LOAD TASKS =================
    private void loadTasks() {

        pendingBox.getChildren().clear();
        progressBox.getChildren().clear();
        completedBox.getChildren().clear();
        cancelledBox.getChildren().clear();

        Label pendingTitle = new Label("Pending");
        Label progressTitle = new Label("In Progress");
        Label completedTitle = new Label("Completed");
        Label cancelledTitle = new Label("Cancelled");

        pendingBox.getChildren().add(pendingTitle);
        progressBox.getChildren().add(progressTitle);
        completedBox.getChildren().add(completedTitle);
        cancelledBox.getChildren().add(cancelledTitle);

        List<Task> tasks = taskService.getAllTasks();

        for (Task task : tasks) {

            if (task == null || task.getStatus() == null) continue;

            String status = task.getStatus().trim().toLowerCase();
            VBox card = createTaskCard(task);

            switch (status) {
                case "pending" -> pendingBox.getChildren().add(card);
                case "in_progress" -> progressBox.getChildren().add(card);
                case "completed" -> completedBox.getChildren().add(card);
                case "cancelled" -> cancelledBox.getChildren().add(card);
            }
        }
    }


    // ================= CREATE CARD =================
    private VBox createTaskCard(Task task) {

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15;");

        Label name = new Label(task.getTaskName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label type = new Label("Type: " + task.getTaskType());
        Label assigned = new Label("Assigned: " + task.getAssignedTo());
        Label cost = new Label("Cost: " + task.getCost());

        // 🔵 EDIT BUTTON
        Button editBtn = new Button("Edit");
        editBtn.setStyle(
                "-fx-background-color: #2196f3;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        editBtn.setOnAction(e -> openEditTask(task));

        // 🔴 DELETE BUTTON
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(
                "-fx-background-color: #f44336;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        deleteBtn.setOnAction(e -> {
            TaskService taskService = new TaskService();
            taskService.deleteTask(task.getTaskId());
            loadTasks(); // refresh
        });

        // ✅ Put both buttons in an HBox
        HBox buttonBox = new HBox(10, editBtn, deleteBtn);

        card.getChildren().addAll(name, type, assigned, cost, buttonBox);

        // Drag support
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(task.getTaskId()));
            db.setContent(content);
            event.consume();
        });

        return card;
    }
    // ================= DRAG DROP =================
    private void setupDragAndDrop(VBox targetBox, String newStatus) {

        targetBox.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        targetBox.setOnDragDropped(event -> {

            Dragboard db = event.getDragboard();

            if (db.hasString()) {

                int taskId = Integer.parseInt(db.getString());

                taskService.updateStatus(taskId, newStatus);
                loadTasks();
            }

            event.setDropCompleted(true);
            event.consume();
        });
    }

    // ================= FILTER =================
    private void filterTasks(String keyword) {
        loadTasks();
    }

    @FXML
    private void handleExportPDF() {
        taskService.exportTasksToPDF();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText("PDF exported successfully!");
        alert.showAndWait();
    }
    private void openEditTask(Task task) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddTask.fxml")
            );

            Parent root = loader.load();

            AddTaskController controller = loader.getController();
            controller.setTaskForEdit(task); // 🔥 THIS is important

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Task");
            stage.showAndWait();

            loadTasks(); // refresh after update

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}