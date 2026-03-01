package tn.esprit.forum.entity;

import java.time.LocalDateTime;

public class Post {
    private int idPost;
    private String title;
    private String content;
    private String author;
    private String category;
    private String status; // ACTIVE / ARCHIVED
    private LocalDateTime createdAt;
    private int authorId;
    private String imagePath;

    public Post() {}

    public Post(int idPost, String title, String content, String author, String category,
                String status, LocalDateTime createdAt, int authorId) {
        this.idPost = idPost;
        this.title = title;
        this.content = content;
        this.author = author;
        this.category = category;
        this.status = status;
        this.createdAt = createdAt;
        this.authorId = authorId;
    }

    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
