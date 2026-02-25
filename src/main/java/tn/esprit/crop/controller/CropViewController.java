package tn.esprit.crop.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;

import java.time.LocalDate;

public class CropViewController {

    @FXML private TableView<Crop> cropsTable;

    @FXML private TableColumn<Crop, String> colName;
    @FXML private TableColumn<Crop, String> colType;
    @FXML private TableColumn<Crop, Double> colArea;
    @FXML private TableColumn<Crop, LocalDate> colPlanted;
    @FXML private TableColumn<Crop, LocalDate> colHarvest;
    @FXML private TableColumn<Crop, String> colGrowthStage;
    @FXML private TableColumn<Crop, String> colStatus;
    @FXML private TableColumn<Crop, Void> colAction;

    private final CropDAO cropDAO = new CropDAO();

    @FXML
    public void initialize() {

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaSize"));
        colPlanted.setCellValueFactory(new PropertyValueFactory<>("plantingDate"));
        colHarvest.setCellValueFactory(new PropertyValueFactory<>("expectedHarvestDate"));
        colGrowthStage.setCellValueFactory(new PropertyValueFactory<>("growthStage"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadCrops();
        addActionButtons();
    }

    private void loadCrops() {
        cropsTable.setItems(FXCollections.observableArrayList(
                cropDAO.getAllCrops()
        ));
    }

    private void addActionButtons() {

        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");

                editBtn.setOnAction(event -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    openEditWindow(crop);
                });

                deleteBtn.setOnAction(event -> {
                    Crop crop = getTableView().getItems().get(getIndex());
                    cropDAO.deleteCrop(crop.getCropId());
                    loadCrops();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void handleAddCrop() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddCrop.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Add Crop");
            stage.showAndWait();

            loadCrops();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditWindow(Crop crop) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/crop/AddCrop.fxml")
            );

            Parent root = loader.load();

            AddCropController controller = loader.getController();
            controller.setCrop(crop);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Edit Crop");
            stage.showAndWait();

            loadCrops();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
