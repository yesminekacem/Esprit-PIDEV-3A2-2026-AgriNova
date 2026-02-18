package tn.esprit.crop.service;

import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;
import java.time.LocalDate;
import java.util.List;

public class CropService {
    private CropDAO cropDAO = new CropDAO();

    public List<Crop> getAllCrops() {
        try {
            return cropDAO.getAllCrops();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public int computeHealth(Crop crop) {
        if (crop.getPlantingDate() == null || crop.getExpectedHarvestDate() == null) {
            return 0;
        }
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(crop.getPlantingDate(), crop.getExpectedHarvestDate());
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(crop.getPlantingDate(), LocalDate.now());
        if (totalDays <= 0) return 100;
        int health = (int) ((daysPassed * 100) / totalDays);
        return Math.min(100, Math.max(0, health));
    }

    public String mapToUiStatus(Crop crop) {
        if (crop.getStatus() == null) return "unknown";
        switch (crop.getStatus()) {
            case "harvested":
                return "harvested";
            case "active":
                if (crop.getExpectedHarvestDate() != null &&
                        crop.getExpectedHarvestDate().minusDays(7).isBefore(LocalDate.now())) {
                    return "ready";
                }
                return "growing";
            default:
                return "planned";
        }
    }
}