package tn.esprit.crop.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;
import javafx.scene.Parent;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CropsController implements Initializable {

    @FXML
    private FlowPane cropContainer;

    private final CropDAO cropDAO = new CropDAO();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCrops();
    }

    private void loadCrops() {

        cropContainer.getChildren().clear();

        List<Crop> crops = cropDAO.getAllCrops();

        System.out.println("Crops found: " + crops.size());

        for (Crop crop : crops) {
            System.out.println("Loading crop: " + crop.getName());
            VBox card = createCropCard(crop);
            cropContainer.getChildren().add(card);
        }
    }


    private VBox createCropCard(Crop crop) {

        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPrefHeight(250);
        card.setStyle("-fx-background-color: #d4edda; " +   // light green
                "-fx-padding: 15; " +
                "-fx-border-color: #2e7d32;");


        // 🔥 Image
        ImageView imageView = new ImageView();

        try {
            Image image = new Image(getClass().getResourceAsStream(crop.getImagePath()));
            imageView.setImage(image);
        } catch (Exception e) {
            System.out.println("Image not found: " + crop.getImagePath());
        }

        imageView.setFitWidth(150);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        //  Crop Name
        Label name = new Label(crop.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
// 🔥 Premium Add Button
        Button addBtn = new Button("Add Task");

        addBtn.setStyle("""
    -fx-background-color: linear-gradient(to right, #2e7d32, #66bb6a);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-background-radius: 30;
    -fx-padding: 8 20;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);
""");

// 🔥 Hover Effect
        addBtn.setOnMouseEntered(e ->
                addBtn.setStyle("""
        -fx-background-color: linear-gradient(to right, #1b5e20, #43a047);
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 30;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 6);
    """)
        );

        addBtn.setOnMouseExited(e ->
                addBtn.setStyle("""
        -fx-background-color: linear-gradient(to right, #2e7d32, #66bb6a);
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-font-size: 13px;
        -fx-background-radius: 30;
        -fx-padding: 8 20;
        -fx-cursor: hand;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);
    """)
        );

// Click action
        addBtn.setOnAction(e -> openAddTaskPage(crop));

        card.getChildren().addAll(imageView, name, addBtn);

        return card;

    }

    private void openAddTaskPage(Crop crop) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddTask.fxml")
            );

            Parent root = loader.load();   // 🔥 LOAD FIRST

            AddTaskController controller = loader.getController(); // 🔥 THEN get controller
            controller.setCropId(crop.getCropId());

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Add Task - " + crop.getName());
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddCrop() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddCrop.fxml")
            );

            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Add Crop");
            stage.showAndWait();

            loadCrops(); // refresh cards after add

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

            stage.showAndWait();   // 🔥 WAIT until closed

            loadCrops();          // 🔥 refresh cards after admin closes

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
