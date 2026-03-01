package tn.esprit.crop.service;

import tn.esprit.crop.entity.Crop;
import tn.esprit.crop.entity.Task;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;

public class TaskService {

    // ================= ADD TASK =================
    public void addTask(Task task) {

        String sql = "INSERT INTO task (crop_id, task_name, description, task_type, " +
                "scheduled_date, completed_date, status, assigned_to, cost) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= GET ALL TASKS =================
    public List<Task> getAllTasks() {

        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM task";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

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
            e.printStackTrace();
        }

        return tasks;
    }

    // ================= DELETE TASK =================
    public void deleteTask(int id) {

        String sql = "DELETE FROM task WHERE task_id = ?";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE TASK =================
    public void updateTask(Task task) {

        String sql = "UPDATE task SET crop_id=?, task_name=?, description=?, " +
                "task_type=?, scheduled_date=?, completed_date=?, " +
                "status=?, assigned_to=?, cost=? WHERE task_id=?";

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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= UPDATE STATUS =================
    public void updateStatus(int taskId, String status) {

        String sql = "UPDATE task SET status = ? WHERE task_id = ?";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, taskId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ================= GET TASK BY ID =================
    public Task getTaskById(int id) {

        String sql = "SELECT * FROM task WHERE task_id = ?";

        try (Connection cnx = MyConnection.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Task(
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
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ================= EXPORT PDF =================
    public void exportTasksToPDF() {
        try {

            List<Task> tasks = getAllTasks();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream("tasks_report.pdf"));

            document.open();

            // ===== Title =====
            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            20,
                            com.itextpdf.text.Font.BOLD
                    );

            Paragraph title = new Paragraph("Digital Farm - Task Report", titleFont);
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // ===== Table =====
            com.itextpdf.text.pdf.PdfPTable table =
                    new com.itextpdf.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);

            // Header font (white text)
            com.itextpdf.text.Font headerFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            12,
                            com.itextpdf.text.Font.BOLD,
                            com.itextpdf.text.BaseColor.WHITE
                    );

            // Green color
            com.itextpdf.text.BaseColor green =
                    new com.itextpdf.text.BaseColor(46, 125, 50);

            // Create header cells with green background
            String[] headers = {"Name", "Type", "Assigned", "Cost", "Status"};

            for (String h : headers) {
                com.itextpdf.text.pdf.PdfPCell cell =
                        new com.itextpdf.text.pdf.PdfPCell(new Paragraph(h, headerFont));
                cell.setBackgroundColor(green);
                cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                cell.setPadding(8);
                table.addCell(cell);
            }

            // Normal font
            com.itextpdf.text.Font normalFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            11
                    );

            boolean alternate = false;

            for (Task task : tasks) {

                com.itextpdf.text.BaseColor rowColor =
                        alternate ? new com.itextpdf.text.BaseColor(240, 240, 240)
                                : com.itextpdf.text.BaseColor.WHITE;

                String[] values = {
                        task.getTaskName(),
                        task.getTaskType(),
                        task.getAssignedTo(),
                        String.valueOf(task.getCost()),
                        task.getStatus()
                };

                for (String value : values) {
                    com.itextpdf.text.pdf.PdfPCell cell =
                            new com.itextpdf.text.pdf.PdfPCell(new Paragraph(value, normalFont));
                    cell.setBackgroundColor(rowColor);
                    cell.setPadding(6);
                    table.addCell(cell);
                }

                alternate = !alternate;
            }

            document.add(table);
            document.close();

            System.out.println("PDF Exported Successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean validateTask(Task task, Crop crop) {

        if(task.getTaskName().toLowerCase().contains("harvest")
                && !crop.getGrowthStage().equalsIgnoreCase("Mature")) {
            return false;
        }

        if(task.getDescription() == null || task.getDescription().length() < 10) {
            return false;
        }

        return true;
    }

}