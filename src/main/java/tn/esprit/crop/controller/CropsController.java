package tn.esprit.crop.controller;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.AITaskDTO;
import tn.esprit.crop.entity.Crop;
import tn.esprit.crop.entity.Task;
import tn.esprit.crop.service.AIService;
import tn.esprit.crop.service.DiseaseDetectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Parent;
import tn.esprit.crop.service.TaskService;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CropsController implements Initializable {

    @FXML
    private FlowPane cropContainer;

    @FXML
    private TextField searchField;

    private final CropDAO cropDAO = new CropDAO();
    private final DiseaseDetectionService diseaseService = new DiseaseDetectionService();

    private ObservableList<Crop> cropList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCrops();
        setupSearch();
    }

    // =========================
    // LOAD CROPS
    // =========================
    private void loadCrops() {
        cropContainer.getChildren().clear();
        cropList.clear();

        List<Crop> crops = cropDAO.getAllCrops();
        cropList.addAll(crops);

        displayCrops(cropList);
    }

    // =========================
    // SEARCH
    // =========================
    private void setupSearch() {

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {

            FilteredList<Crop> filteredList = new FilteredList<>(cropList, crop -> {

                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String keyword = newValue.toLowerCase();

                return crop.getName().toLowerCase().contains(keyword)
                        || crop.getType().toLowerCase().contains(keyword)
                        || crop.getStatus().toLowerCase().contains(keyword);
            });

            displayCrops(filteredList);
        });
    }

    // =========================
    // DISPLAY CROPS
    // =========================
    private void displayCrops(List<Crop> crops) {
        cropContainer.getChildren().clear();

        for (Crop crop : crops) {
            VBox card = createCropCard(crop);
            cropContainer.getChildren().add(card);
        }
    }
    // =========================
// CREATE CARD
// =========================
    private VBox createCropCard(Crop crop) {

        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPrefHeight(320);
        card.setStyle(
                "-fx-background-color: #d4edda;" +
                        "-fx-padding: 15;" +
                        "-fx-border-color: #2e7d32;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;"
        );

        ImageView imageView = new ImageView();

        if (crop.getImagePath() != null && !crop.getImagePath().isEmpty()) {
            File file = new File(crop.getImagePath());
            if (file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
            }
        }

        imageView.setFitWidth(150);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        Label name = new Label(crop.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // ================= ADD TASK BUTTON =================
        Button addBtn = new Button("Add Task");
        addBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #2e7d32, #66bb6a);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 20;" +
                        "-fx-cursor: hand;"
        );
        addBtn.setOnAction(e -> openAddTaskPage(crop));

        // ================= AI GENERATE BUTTON =================
        Button aiBtn = new Button("🤖 Generate AI Tasks");
        aiBtn.setStyle(
                "-fx-background-color: linear-gradient(to right, #1565c0, #42a5f5);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 30;" +
                        "-fx-padding: 8 15;" +
                        "-fx-cursor: hand;"
        );

        aiBtn.setOnAction(e -> generateAITasksForCrop(crop));

        card.getChildren().addAll(imageView, name, addBtn, aiBtn);

        return card;
    }

    // =========================
    // OPEN ADD TASK
    // =========================
    private void openAddTaskPage(Crop crop) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddTask.fxml")
            );

            Parent root = loader.load();
            AddTaskController controller = loader.getController();
            controller.setCropId(crop.getCropId());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Add Task - " + crop.getName());
            stage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // HEADER BUTTONS
    // =========================
    @FXML
    private void handleAddCrop() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddCrop.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Add Crop");
            stage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
            stage.showAndWait();

            loadCrops();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTaskPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/TaskPage.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Task Management");
            stage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleManageCrops() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/CropView.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Crop Management - Admin");
            stage.getIcons().setAll(tn.esprit.MainFX.getAppIcon());
            stage.showAndWait();
            loadCrops();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // =========================
// DISEASE DETECTION
// =========================
    private void handleDetectDisease(Crop crop) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Leaf Image");

            File file = fileChooser.showOpenDialog(null);
            if (file == null) return;

            String result = diseaseService.sendImageToAI(file);

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);

            String disease = root.get("disease").asText();
            double confidence = root.get("confidence").asDouble() * 100;

            // Format values
            String formattedConfidence = String.format("%.2f%%", confidence);
            String formattedDisease = disease.replace("_", " ");

            // Create alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Plant Disease Detection");
            alert.setHeaderText("Crop: " + crop.getName());

            // Create styled content
            Label diseaseLabel = new Label("Detected Disease:");
            diseaseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label diseaseValue = new Label(formattedDisease);
            diseaseValue.setStyle("-fx-font-size: 16px; -fx-text-fill: "
                    + (confidence < 40 ? "#c62828;" : "#2e7d32;"));

            Label confidenceLabel = new Label("Confidence Level:");
            confidenceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label confidenceValue = new Label(formattedConfidence);
            confidenceValue.setStyle("-fx-font-size: 15px; -fx-text-fill: #1565c0;");

            VBox content = new VBox(12,
                    diseaseLabel,
                    diseaseValue,
                    confidenceLabel,
                    confidenceValue
            );

            content.setStyle("-fx-padding: 15;");

            alert.getDialogPane().setContent(content);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleGlobalDetectDisease() {

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Leaf Image");

            File file = fileChooser.showOpenDialog(null);
            if (file == null) return;

            String result = diseaseService.sendImageToAI(file);

            if (result == null || result.isEmpty()) {
                showErrorAlert("AI returned empty response.");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(result);

            if (!root.has("disease") || !root.has("confidence")) {
                showErrorAlert("Invalid AI response format.");
                return;
            }

            String disease = root.get("disease").asText();
            double confidence = root.get("confidence").asDouble() * 100;

            String formattedConfidence = String.format("%.2f%%", confidence);
            String formattedDisease = disease.replace("_", " ");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Plant Disease Detection");
            alert.setHeaderText(null);

            alert.getDialogPane().setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #2e7d32;" +
                            "-fx-border-width: 2;"
            );

            Label title = new Label("Plant Disease Detection");
            title.setStyle(
                    "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #2e7d32;"
            );

            Label diseaseLabel = new Label("Detected Disease");
            diseaseLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

            Label diseaseValue = new Label(formattedDisease);
            diseaseValue.setStyle(
                    "-fx-font-size: 20px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #1b5e20;"
            );

            Label confidenceLabel = new Label("Confidence Level");
            confidenceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");

            ProgressBar progressBar = new ProgressBar(confidence / 100.0);
            progressBar.setPrefWidth(250);
            progressBar.setStyle("-fx-accent: #43a047;");

            Label confidenceValue = new Label(formattedConfidence);
            confidenceValue.setStyle(
                    "-fx-font-size: 16px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #2e7d32;"
            );

            VBox content = new VBox(15,
                    title,
                    new Separator(),
                    diseaseLabel,
                    diseaseValue,
                    confidenceLabel,
                    progressBar,
                    confidenceValue
            );

            content.setStyle("-fx-padding: 20; -fx-background-color: white;");

            alert.getDialogPane().setContent(content);
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error detecting plant disease.");
        }
    }
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void generateAITasksForCrop(Crop selectedCrop) {

        try {

            AIService aiService = new AIService();

            String json = String.format("""
        {
          "crop_name": "%s",
          "growth_stage": "%s",
          "temperature": 28,
          "soil_moisture": "Medium",
          "location": "Tunisia"
        }
        """,
                    selectedCrop.getName(),
                    selectedCrop.getGrowthStage());

            String response = aiService.generateTasks(json);

            if (response == null || response.isBlank()) {
                showError("AI returned empty response.");
                return;
            }

            if (!response.trim().startsWith("[")) {
                showError("AI returned invalid format:\n" + response);
                return;
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<List<AITaskDTO>>(){}.getType();
            List<AITaskDTO> aiTasks = gson.fromJson(response, listType);

            if (aiTasks == null || aiTasks.isEmpty()) {
                showError("AI returned no tasks.");
                return;
            }

            showAITasksPreview(aiTasks, selectedCrop);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error generating AI tasks.");
        }
    }
    private void showAITasksPreview(List<AITaskDTO> aiTasks, Crop crop) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("AI Task Suggestions");

        VBox container = new VBox(10);

        List<CheckBox> checkBoxes = new ArrayList<>();

        for (AITaskDTO aiTask : aiTasks) {

            CheckBox checkBox = new CheckBox(
                    aiTask.getTitle() + " (" + aiTask.getPriority() + ")"
            );

            Label desc = new Label(aiTask.getDescription());
            desc.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

            VBox box = new VBox(5, checkBox, desc);
            box.setStyle("-fx-padding: 5;");

            container.getChildren().add(box);
            checkBoxes.add(checkBox);
        }

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setPrefHeight(300);

        dialog.getDialogPane().setContent(scrollPane);

        ButtonType confirmBtn = new ButtonType("Add Selected");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, cancelBtn);

        dialog.showAndWait().ifPresent(response -> {

            if (response == confirmBtn) {

                TaskService taskService = new TaskService();

                for (int i = 0; i < checkBoxes.size(); i++) {

                    if (checkBoxes.get(i).isSelected()) {

                        AITaskDTO aiTask = aiTasks.get(i);

                        Task task = new Task(
                                crop.getCropId(),
                                aiTask.getTitle(),
                                aiTask.getDescription(),
                                mapPriority(aiTask.getPriority()),
                                LocalDate.now().plusDays(1),
                                null,
                                "pending",
                                "Farmer",
                                0.0
                        );

                        if (taskService.validateTask(task, crop)) {
                            taskService.addTask(task);
                        }
                    }
                }
            }
        });
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("AI Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private String mapPriority(String priority) {

        if(priority == null) return "Normal";

        switch(priority.toLowerCase()) {
            case "high": return "Urgent";
            case "medium": return "Normal";
            case "low": return "Optional";
            default: return "Normal";
        }
    }
}