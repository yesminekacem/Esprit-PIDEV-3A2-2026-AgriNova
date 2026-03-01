package tn.esprit.forum.entity;

import java.time.LocalDateTime;

public class Notification {

    private int id;
    private int recipientId;   // who receives it (post owner)
    private int actorId;       // who did action (liker/commenter)
    private int postId;        // related post
    private String type;       // LIKE / COMMENT
    private String message;    // display text
    private boolean isRead;    // read status
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(int recipientId, int actorId, int postId, String type, String message) {
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.postId = postId;
        this.type = type;
        this.message = message;
        this.isRead = false;
    }

    // ----- Getters & Setters -----

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRecipientId() { return recipientId; }
    public void setRecipientId(int recipientId) { this.recipientId = recipientId; }

    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipientId=" + recipientId +
                ", actorId=" + actorId +
                ", postId=" + postId +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
}