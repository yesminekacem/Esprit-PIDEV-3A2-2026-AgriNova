package tn.esprit.forum.entity;

import java.time.LocalDateTime;

public class Comment {
    private int idComment;
    private int idPost;
    private String content;
    private String author;
    private int likes;
    private LocalDateTime createdAt;
    private int authorId;


    public Comment() {}

    public Comment(int idComment, int idPost, String content, String author, int likes, LocalDateTime createdAt) {
        this.idComment = idComment;
        this.idPost = idPost;
        this.content = content;
        this.author = author;
        this.likes = likes;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int getIdComment() { return idComment; }
    public void setIdComment(int idComment) { this.idComment = idComment; }

    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }


    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    @Override
    public String toString() {
        return "Comment{" +
                "idComment=" + idComment +
                ", idPost=" + idPost +
                ", author='" + author + '\'' +
                ", likes=" + likes +
                ", createdAt=" + createdAt +
                '}';
    }
}
