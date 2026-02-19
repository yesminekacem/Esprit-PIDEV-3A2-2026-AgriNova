package tn.esprit.crop.entity;

import java.time.LocalDate;

public class Task {

    private int taskId;
    private int cropId;
    private String taskName;
    private String description;
    private String taskType;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private String status;
    private String assignedTo;
    private double cost;

    // Constructor without id (INSERT)
    public Task(int cropId, String taskName, String description,
                String taskType, LocalDate scheduledDate,
                LocalDate completedDate, String status,
                String assignedTo, double cost) {

        this.cropId = cropId;
        this.taskName = taskName;
        this.description = description;
        this.taskType = taskType;
        this.scheduledDate = scheduledDate;
        this.completedDate = completedDate;
        this.status = status;
        this.assignedTo = assignedTo;
        this.cost = cost;
    }

    // Constructor with id (SELECT)
    public Task(int taskId, int cropId, String taskName, String description,
                String taskType, LocalDate scheduledDate,
                LocalDate completedDate, String status,
                String assignedTo, double cost) {

        this.taskId = taskId;
        this.cropId = cropId;
        this.taskName = taskName;
        this.description = description;
        this.taskType = taskType;
        this.scheduledDate = scheduledDate;
        this.completedDate = completedDate;
        this.status = status;
        this.assignedTo = assignedTo;
        this.cost = cost;
    }

    // Getters & Setters

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getCropId() { return cropId; }
    public void setCropId(int cropId) { this.cropId = cropId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalDate getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
}
