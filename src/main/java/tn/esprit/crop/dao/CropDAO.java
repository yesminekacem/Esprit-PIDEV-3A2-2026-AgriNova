package tn.esprit.crop.dao;

import tn.esprit.crop.entity.Crop;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CropDAO {

    public List<Crop> getAllCrops() {

        List<Crop> crops = new ArrayList<>();

        String sql = """
            SELECT
                crop_id,
                name,
                type,
                variety,
                planting_date,
                expected_harvest_date,
                growth_stage,
                area_size,
                status,
               image_path
            FROM crop
        """;

        try (
                Connection cnx = MyConnection.getInstance().getCnx();
                PreparedStatement ps = cnx.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                LocalDate plantingDate =
                        rs.getDate("planting_date") != null
                                ? rs.getDate("planting_date").toLocalDate()
                                : null;

                LocalDate expectedHarvestDate =
                        rs.getDate("expected_harvest_date") != null
                                ? rs.getDate("expected_harvest_date").toLocalDate()
                                : null;

                Crop crop = new Crop(
                        rs.getInt("crop_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("variety"),
                        plantingDate,
                        expectedHarvestDate,
                        rs.getString("growth_stage"),
                        rs.getDouble("area_size"),
                        rs.getString("status"),
                        rs.getString("image_path")
                );

                crops.add(crop);
            }

            System.out.println("✅ DAO fetched " + crops.size() + " crops");

        } catch (SQLException e) {
            System.err.println("❌ Error loading crops from database");
            e.printStackTrace();
        }
        return crops;
    }
    public void insertCrop(Crop crop) {

        String sql = "INSERT INTO crop (name, type, variety, planting_date, expected_harvest_date, growth_stage, area_size, status, image_path) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";


        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, crop.getName());
            ps.setString(2, crop.getType());
            ps.setString(3, crop.getVariety());
            ps.setDate(4, Date.valueOf(crop.getPlantingDate()));
            ps.setDate(5, Date.valueOf(crop.getExpectedHarvestDate()));
            ps.setString(6, crop.getGrowthStage());
            ps.setDouble(7, crop.getAreaSize());
            ps.setString(8, crop.getStatus());
            ps.setString(9, crop.getImagePath());


            ps.executeUpdate();
            System.out.println("✅ Crop inserted successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error inserting crop");
            e.printStackTrace();
        }
    }
    public void deleteCrop(int id) {

        String sql = "DELETE FROM crop WHERE crop_id = ?";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✅ Crop deleted successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error deleting crop");
            e.printStackTrace();
        }
    }
    public void updateCrop(Crop crop) {

        String sql = """
        UPDATE crop SET
            name = ?,
            type = ?,
            variety = ?,
            planting_date = ?,
            expected_harvest_date = ?,
            growth_stage = ?,
            area_size = ?,
            status = ?,
            image_path = ?
        WHERE crop_id = ?
    """;

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, crop.getName());
            ps.setString(2, crop.getType());
            ps.setString(3, crop.getVariety());
            ps.setDate(4, Date.valueOf(crop.getPlantingDate()));
            ps.setDate(5, Date.valueOf(crop.getExpectedHarvestDate()));
            ps.setString(6, crop.getGrowthStage());
            ps.setDouble(7, crop.getAreaSize());
            ps.setString(8, crop.getStatus());
            ps.setString(9, crop.getImagePath());
            ps.setInt(10, crop.getCropId());

            ps.executeUpdate();

            System.out.println("✅ Crop updated successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error updating crop");
            e.printStackTrace();
        }
    }



}
