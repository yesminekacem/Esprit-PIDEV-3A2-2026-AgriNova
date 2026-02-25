package tn.esprit.crop.service;

import tn.esprit.crop.entity.Task;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private Connection cnx;

    public TaskService() {
        try {
            cnx = MyConnection.getInstance().getCnx();
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
        }
    }


    // ✅ ADD TASK
    public void addTask(Task task) {

        String sql = "INSERT INTO task (crop_id, task_name, description, task_type, " +
                "scheduled_date, completed_date, status, assigned_to, cost) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

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
            System.out.println("✅ Task added successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error adding task: " + e.getMessage());
        }
    }

    // ✅ GET ALL TASKS
    public List<Task> getAllTasks() {

        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM task";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                Task task = new Task(
                        rs.getInt("task_id"),
                        rs.getInt("crop_id"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getString("task_type"),
                        rs.getDate("scheduled_date").toLocalDate(),
                        rs.getDate("completed_date") != null
                                ? rs.getDate("completed_date").toLocalDate()
                                : null,
                        rs.getString("status"),
                        rs.getString("assigned_to"),
                        rs.getDouble("cost")
                );

                tasks.add(task);
            }

        } catch (SQLException e) {
            System.out.println("❌ Error fetching tasks: " + e.getMessage());
        }

        return tasks;
    }
    public void deleteTask(int id) {

        String sql = "DELETE FROM task WHERE task_id = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Task deleted!");
        } catch (SQLException e) {
            System.out.println("❌ Error deleting task: " + e.getMessage());
        }
    }
    public void updateTask(Task task) {

        String sql = "UPDATE task SET crop_id=?, task_name=?, description=?, " +
                "task_type=?, scheduled_date=?, completed_date=?, " +
                "status=?, assigned_to=?, cost=? WHERE task_id=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(sql);

            ps.setInt(1, task.getCropId());
            ps.setString(2, task.getTaskName());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getTaskType());
            ps.setDate(5, java.sql.Date.valueOf(task.getScheduledDate()));

            if (task.getCompletedDate() != null)
                ps.setDate(6, java.sql.Date.valueOf(task.getCompletedDate()));
            else
                ps.setNull(6, java.sql.Types.DATE);

            ps.setString(7, task.getStatus());
            ps.setString(8, task.getAssignedTo());
            ps.setDouble(9, task.getCost());

            ps.setInt(10, task.getTaskId());

            ps.executeUpdate();
            System.out.println("✅ Task updated!");

        } catch (SQLException e) {
            System.out.println("❌ Error updating task: " + e.getMessage());
        }
    }


}
