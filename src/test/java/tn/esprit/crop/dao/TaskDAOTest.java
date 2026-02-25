package tn.esprit.crop.dao;

import org.junit.jupiter.api.*;
import tn.esprit.crop.entity.Task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskDAOTest {

    private static TaskDAO dao;
    private static int insertedId;
    private static int existingCropId = 1; // ⚠ change if needed

    @BeforeAll
    static void setup() {
        dao = new TaskDAO();
    }

    @Test
    @Order(1)
    void testInsertTask() {

        Task task = new Task(
                0,
                existingCropId,
                "JUnit Task",
                "Testing insert",
                "maintenance",
                LocalDate.now(),
                null,
                "pending",
                "Worker1",
                100.0
        );

        dao.insertTask(task);

        List<Task> tasks = dao.getAllTasks();
        assertFalse(tasks.isEmpty());

        insertedId = tasks.get(tasks.size() - 1).getTaskId();
    }

    @Test
    @Order(2)
    void testUpdateTask() {

        Task updated = new Task(
                insertedId,
                existingCropId,
                "Updated JUnit Task",
                "Updated description",
                "maintenance",
                LocalDate.now(),
                null,
                "in_progress",
                "Worker2",
                150.0
        );

        dao.updateTask(updated);

        Task result = dao.getAllTasks()
                .stream()
                .filter(t -> t.getTaskId() == insertedId)
                .findFirst()
                .orElse(null);

        assertNotNull(result);
        assertEquals("Updated JUnit Task", result.getTaskName());
    }

    // 🔹 COMMENT THIS IF YOU WANT TO KEEP DATA
    /*
    @Test
    @Order(3)
    void testDeleteTask() {

        dao.deleteTask(insertedId);

        boolean exists = dao.getAllTasks()
                .stream()
                .anyMatch(t -> t.getTaskId() == insertedId);

        assertFalse(exists);
    }
    */
}
