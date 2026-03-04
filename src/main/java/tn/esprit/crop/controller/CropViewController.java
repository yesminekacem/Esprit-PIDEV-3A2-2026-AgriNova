package tn.esprit.crop.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.crop.service.DiseaseDetectionService;
import java.io.File;;
import java.util.Map;
import java.util.stream.Collectors;
public class CropViewController {
    @FXML private Label totalAreaLabel;
    @FXML private Label averageAreaLabel;
    @FXML private Label maxAreaLabel;

    @FXML private PieChart areaPieChart;

    private ObservableList<Crop> cropList = FXCollections.observableArrayList();
    @FXML private TableView<Crop> cropsTable;

    @FXML private TableColumn<Crop, String> colName;
    @FXML private TableColumn<Crop, String> colType;
    @FXML private TableColumn<Crop, Double> colArea;
    @FXML private TableColumn<Crop, LocalDate> colPlanted;
    @FXML private TableColumn<Crop, LocalDate> colHarvest;
    @FXML private TableColumn<Crop, String> colGrowthStage;
    @FXML private TableColumn<Crop, String> colStatus;
    @FXML private TableColumn<Crop, Void> colAction;


    @FXML private Label minAreaLabel;
    private final CropDAO cropDAO = new CropDAO();
    private final DiseaseDetectionService diseaseService = new DiseaseDetectionService();

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

        ObservableList<Crop> list = FXCollections.observableArrayList(
                cropDAO.getAllCrops()
        );

        cropsTable.setItems(list);

        colName.setSortType(TableColumn.SortType.ASCENDING);
        cropsTable.getSortOrder().clear();
        cropsTable.getSortOrder().add(colName);
        cropsTable.sort();

        calculateStatistics(list);
        updatePieChart(list);
    }
    private void calculateStatistics(ObservableList<Crop> crops) {

        if (crops.isEmpty()) return;

        double total = crops.stream()
                .mapToDouble(Crop::getAreaSize)
                .sum();

        double average = crops.stream()
                .mapToDouble(Crop::getAreaSize)
                .average()
                .orElse(0);

        double max = crops.stream()
                .mapToDouble(Crop::getAreaSize)
                .max()
                .orElse(0);

        totalAreaLabel.setText(String.format("%.2f ha", total));
        averageAreaLabel.setText(String.format("%.2f ha", average));
        maxAreaLabel.setText(String.format("%.2f ha", max));
    }
    private void updatePieChart(ObservableList<Crop> crops) {

        areaPieChart.getData().clear();

        // Group area by crop type
        Map<String, Double> areaByType = crops.stream()
                .collect(Collectors.groupingBy(
                        Crop::getType,
                        Collectors.summingDouble(Crop::getAreaSize)
                ));

        double totalArea = areaByType.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        for (Map.Entry<String, Double> entry : areaByType.entrySet()) {

            double percentage = (entry.getValue() / totalArea) * 100;

            PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " (" + String.format("%.1f", percentage) + "%)",
                    entry.getValue()
            );

            areaPieChart.getData().add(slice);
        }
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
