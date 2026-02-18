package tn.esprit.crop.dao;

import org.junit.jupiter.api.*;
import tn.esprit.crop.dao.CropDAO;
import tn.esprit.crop.entity.Crop;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CropDAOTest {

    private static CropDAO dao;
    private static int insertedId;

    @BeforeAll
    static void setup() {
        dao = new CropDAO();
    }

    @Test
    @Order(1)
    void testInsertCrop() {

        Crop crop = new Crop(
                0,
                "JUnit Crop",
                "TestType",
                "TestVariety",
                LocalDate.now(),
                LocalDate.now().plusMonths(2),
                "Seedling",
                5.0,
                "active"
        );

        dao.insertCrop(crop);

        List<Crop> crops = dao.getAllCrops();

        assertFalse(crops.isEmpty());

        insertedId = crops.get(crops.size() - 1).getCropId();
    }

    @Test
    @Order(2)
    void testUpdateCrop() {

        Crop updated = new Crop(
                insertedId,
                "Updated JUnit Crop",
                "TestType",
                "TestVariety",
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                "Growing",
                6.0,
                "active"
        );

        dao.updateCrop(updated);

        Crop result = dao.getAllCrops()
                .stream()
                .filter(c -> c.getCropId() == insertedId)
                .findFirst()
                .orElse(null);

        assertNotNull(result);
        assertEquals("Updated JUnit Crop", result.getName());
    }

    @Test
    @Order(3)
    void testDeleteCrop() {

        dao.deleteCrop(insertedId);

        boolean exists = dao.getAllCrops()
                .stream()
                .anyMatch(c -> c.getCropId() == insertedId);

        assertFalse(exists);
    }
}


