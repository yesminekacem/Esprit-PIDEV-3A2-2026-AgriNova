package tn.esprit.crop.entity;

import java.time.LocalDate;

public class Crop {

    private int cropId;
    private String name;
    private String type;
    private String variety;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private String growthStage;
    private double areaSize;
    private String status;
    private String imagePath;


    public Crop(int cropId, String name, String type, String variety,
                LocalDate plantingDate, LocalDate expectedHarvestDate,
                String growthStage, double areaSize, String status,
                String imagePath) {

        this.cropId = cropId;
        this.name = name;
        this.type = type;
        this.variety = variety;
        this.plantingDate = plantingDate;
        this.expectedHarvestDate = expectedHarvestDate;
        this.growthStage = growthStage;
        this.areaSize = areaSize;
        this.status = status;
        this.imagePath = imagePath;
    }


    // ✅ Getters (REQUIRED by JavaFX)
    public int getCropId() { return cropId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getVariety() { return variety; }
    public LocalDate getPlantingDate() { return plantingDate; }
    public LocalDate getExpectedHarvestDate() { return expectedHarvestDate; }
    public String getGrowthStage() { return growthStage; }
    public double getAreaSize() { return areaSize; }
    public String getStatus() { return status; }
    public String getImagePath() {
        return imagePath; }


}
