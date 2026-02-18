package tn.esprit.crop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.crop.entity.Task;
import tn.esprit.crop.service.TaskService;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TaskController {

    @FXML private VBox pendingBox;
    @FXML private VBox progressBox;
    @FXML private VBox completedBox;
    @FXML private VBox cancelledBox;

    private TaskService taskService = new TaskService();

    @FXML
    public void initialize() {
        loadTasks();
    }

    private void loadTasks() {

        // Clear old cards
        pendingBox.getChildren().clear();
        progressBox.getChildren().clear();
        completedBox.getChildren().clear();
        cancelledBox.getChildren().clear();

        // Keep column titles
        pendingBox.getChildren().add(new Label("Pending"));
        progressBox.getChildren().add(new Label("In Progress"));
        completedBox.getChildren().add(new Label("Completed"));
        cancelledBox.getChildren().add(new Label("Cancelled"));

        List<Task> tasks = taskService.getAllTasks();

        for (Task task : tasks) {

            VBox card = createTaskCard(task);

            switch (task.getStatus()) {
                case "pending":
                    pendingBox.getChildren().add(card);
                    break;
                case "in_progress":
                    progressBox.getChildren().add(card);
                    break;
                case "completed":
                    completedBox.getChildren().add(card);
                    break;
                case "cancelled":
                    cancelledBox.getChildren().add(card);
                    break;
            }
        }
    }

    private VBox createTaskCard(Task task) {

        VBox card = new VBox(10);
        card.setStyle("""
        -fx-background-color: white;
        -fx-background-radius: 12;
        -fx-padding: 15;
        -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 4);
    """);

        // 🔹 Task Name
        Label name = new Label(task.getTaskName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        // 🔹 Type
        Label type = new Label("Type: " + task.getTaskType());
        type.setStyle("-fx-text-fill: #555;");

        // 🔹 Assigned
        Label assigned = new Label("Assigned: " + task.getAssignedTo());
        assigned.setStyle("-fx-text-fill: #555;");

        // 🔹 Cost
        Label cost = new Label("Cost: " + task.getCost());
        cost.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");

        // 🔹 Buttons container
        HBox buttons = new HBox(10);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("""
        -fx-background-color: #1976d2;
        -fx-text-fill: white;
        -fx-background-radius: 8;
    """);

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("""
        -fx-background-color: #c62828;
        -fx-text-fill: white;
        -fx-background-radius: 8;
    """);

        editBtn.setOnAction(e -> openUpdatePage(task));

        deleteBtn.setOnAction(e -> {
            taskService.deleteTask(task.getTaskId());
            loadTasks();
        });

        buttons.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(name, type, assigned, cost, buttons);

        return card;
    }


    // 🔥 CHANGE STATUS FLOW
    private void changeStatus(Task task) {

        String current = task.getStatus();

        switch (current) {
            case "pending":
                task.setStatus("in_progress");
                break;
            case "in_progress":
                task.setStatus("completed");
                break;
            case "completed":
                task.setStatus("cancelled");
                break;
            case "cancelled":
                task.setStatus("pending");
                break;
        }

        taskService.updateTask(task);
    }

    @FXML
    private void handleOpenAddPage() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddTask.fxml")
            );

            Scene scene = new Scene(loader.load());

            // 🔥 ADD CSS HERE
            scene.getStylesheets().add(
                    getClass().getResource("/styles/styles.css").toExternalForm()
            );
            scene.getStylesheets().add(
                    getClass().getResource("/styles/crop.css").toExternalForm()
            );

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Add Task");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openUpdatePage(Task task) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddTask.fxml")
            );

            Scene scene = new Scene(loader.load());

            //  Get AddTaskController
            AddTaskController controller = loader.getController();

            //  Tell controller we are editing
            controller.setTaskForEdit(task);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Update Task");
            stage.showAndWait();

            loadTasks(); // refresh after update

        } catch (Exception e) {
            e.printStackTrace();
        }
    }






}
