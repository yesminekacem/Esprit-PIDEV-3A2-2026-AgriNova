package tn.esprit.crop.dao;

import tn.esprit.crop.entity.Task;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskDAO {

    // 🔹 GET ALL TASKS
    public List<Task> getAllTasks() {

        List<Task> tasks = new ArrayList<>();

        String sql = "SELECT * FROM task";

        try (
                Connection cnx = MyConnection.getInstance().getCnx();
                PreparedStatement ps = cnx.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {

                LocalDate scheduled =
                        rs.getDate("scheduled_date") != null
                                ? rs.getDate("scheduled_date").toLocalDate()
                                : null;

                LocalDate completed =
                        rs.getDate("completed_date") != null
                                ? rs.getDate("completed_date").toLocalDate()
                                : null;

                Task task = new Task(
                        rs.getInt("task_id"),
                        rs.getInt("crop_id"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getString("task_type"),
                        scheduled,
                        completed,
                        rs.getString("status"),
                        rs.getString("assigned_to"),
                        rs.getDouble("cost")
                );

                tasks.add(task);
            }

            System.out.println("✅ DAO fetched " + tasks.size() + " tasks");

        } catch (SQLException e) {
            System.err.println("❌ Error loading tasks");
            e.printStackTrace();
        }

        return tasks;
    }

    // 🔹 INSERT
    public void insertTask(Task task) {

        String sql = """
            INSERT INTO task
            (crop_id, task_name, description, task_type,
             scheduled_date, completed_date, status,
             assigned_to, cost)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, task.getCropId());
            ps.setString(2, task.getTaskName());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getTaskType());
            ps.setDate(5, Date.valueOf(task.getScheduledDate()));

            if (task.getCompletedDate() != null)
                ps.setDate(6, Date.valueOf(task.getCompletedDate()));
            else
                ps.setNull(6, Types.DATE);

            ps.setString(7, task.getStatus());
            ps.setString(8, task.getAssignedTo());
            ps.setDouble(9, task.getCost());

            ps.executeUpdate();

            System.out.println("✅ Task inserted successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error inserting task");
            e.printStackTrace();
        }
    }

    // 🔹 UPDATE
    public void updateTask(Task task) {

        String sql = """
            UPDATE task SET
                crop_id = ?,
                task_name = ?,
                description = ?,
                task_type = ?,
                scheduled_date = ?,
                completed_date = ?,
                status = ?,
                assigned_to = ?,
                cost = ?
            WHERE task_id = ?
        """;

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, task.getCropId());
            ps.setString(2, task.getTaskName());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getTaskType());
            ps.setDate(5, Date.valueOf(task.getScheduledDate()));

            if (task.getCompletedDate() != null)
                ps.setDate(6, Date.valueOf(task.getCompletedDate()));
            else
                ps.setNull(6, Types.DATE);

            ps.setString(7, task.getStatus());
            ps.setString(8, task.getAssignedTo());
            ps.setDouble(9, task.getCost());
            ps.setInt(10, task.getTaskId());

            ps.executeUpdate();

            System.out.println("✅ Task updated successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error updating task");
            e.printStackTrace();
        }
    }

    // 🔹 DELETE
    public void deleteTask(int id) {

        String sql = "DELETE FROM task WHERE task_id = ?";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("✅ Task deleted successfully");

        } catch (SQLException e) {
            System.err.println("❌ Error deleting task");
            e.printStackTrace();
        }
    }
}
