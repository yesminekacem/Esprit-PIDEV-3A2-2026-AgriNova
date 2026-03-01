package tn.esprit.forum.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tn.esprit.forum.entity.Comment;

import java.time.Duration;
import java.time.LocalDateTime;

public class CommentCardCell extends ListCell<Comment> {

    private final HBox row = new HBox(12);

    private final StackPane avatar = new StackPane();
    private final Label avatarLetter = new Label();

    private final VBox bubble = new VBox(8);

    private final HBox header = new HBox(10);
    private final HBox nameTime = new HBox(8);

    private final Label lblAuthor = new Label();
    private final Label dot = new Label("•");
    private final Label lblTime = new Label();

    private final Region spacer = new Region();
    private final MenuButton menu = new MenuButton("⋯");

    private final Label lblText = new Label();

    private final HBox footer = new HBox(12);
    private final Label lblLikes = new Label();

    // callbacks injected from controller (no logic here)
    private final java.util.function.Consumer<Comment> onEdit;
    private final java.util.function.Consumer<Comment> onDelete;

    public CommentCardCell(java.util.function.Consumer<Comment> onEdit,
                           java.util.function.Consumer<Comment> onDelete) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        // Row wrapper
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 6, 10, 6));

        // Avatar
        avatar.setMinSize(40, 40);
        avatar.setPrefSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setStyle("""
            -fx-background-color: #dcfce7;
            -fx-background-radius: 999;
            -fx-border-color: rgba(0,0,0,0.08);
            -fx-border-radius: 999;
        """);
        avatarLetter.setStyle("-fx-font-weight: 900; -fx-text-fill: #166534;");
        avatar.getChildren().add(avatarLetter);

        // Bubble card
        bubble.setPadding(new Insets(12, 12, 10, 12));
        bubble.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 16;
            -fx-border-color: rgba(0,0,0,0.08);
            -fx-border-radius: 16;
        """);
        bubble.setEffect(new DropShadow(14, 0, 4, Color.web("#00000010")));

        // hover feel (simple but nice)
        bubble.setOnMouseEntered(e -> {
            bubble.setTranslateY(-2);
            bubble.setEffect(new DropShadow(18, 0, 7, Color.web("#00000018")));
        });
        bubble.setOnMouseExited(e -> {
            bubble.setTranslateY(0);
            bubble.setEffect(new DropShadow(14, 0, 4, Color.web("#00000010")));
        });

        // Author/time
        lblAuthor.setStyle("-fx-font-weight: 900; -fx-text-fill: #111827;");
        dot.setStyle("-fx-text-fill: #d1d5db;");
        lblTime.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 12px;");
        nameTime.setAlignment(Pos.CENTER_LEFT);
        nameTime.getChildren().addAll(lblAuthor, dot, lblTime);

        // Menu button (modern circle)
        menu.setStyle("""
            -fx-background-color: rgba(17,24,39,0.06);
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-padding: 4 12;
            -fx-font-size: 16px;
            -fx-cursor: hand;
        """);
        menu.setOnMouseEntered(e -> menu.setStyle("""
            -fx-background-color: rgba(37,99,235,0.12);
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-padding: 4 12;
            -fx-font-size: 16px;
            -fx-cursor: hand;
        """));
        menu.setOnMouseExited(e -> menu.setStyle("""
            -fx-background-color: rgba(17,24,39,0.06);
            -fx-background-radius: 999;
            -fx-border-radius: 999;
            -fx-padding: 4 12;
            -fx-font-size: 16px;
            -fx-cursor: hand;
        """));

        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(nameTime, spacer, menu);

        // Comment text
        lblText.setWrapText(true);
        lblText.setStyle("""
            -fx-text-fill: #374151;
            -fx-font-size: 13px;
            -fx-line-spacing: 2;
        """);

        // Footer (likes)
        footer.setAlignment(Pos.CENTER_LEFT);
        lblLikes.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        footer.getChildren().add(lblLikes);

        bubble.getChildren().addAll(header, lblText, footer);

        HBox.setHgrow(bubble, Priority.ALWAYS);
        row.getChildren().addAll(avatar, bubble);
    }

    @Override
    protected void updateItem(Comment c, boolean empty) {
        super.updateItem(c, empty);

        if (empty || c == null) {
            setGraphic(null);
            return;
        }

        // Avatar letter
        String author = safe(c.getAuthor());
        avatarLetter.setText(author.isEmpty() ? "?" : ("" + Character.toUpperCase(author.charAt(0))));

        // Header
        lblAuthor.setText(author.isEmpty() ? "Unknown" : author);
        lblTime.setText(formatTimeAgo(c.getCreatedAt()));

        // Content
        String text = safe(c.getContent()).trim();
        lblText.setText(text.isEmpty() ? "No input" : text);

        // Likes footer
        int likes = c.getLikes();
        lblLikes.setText("👍  " + likes + (likes == 1 ? " like" : " likes"));

        // Menu items (Edit/Delete)
        menu.getItems().clear();

        MenuItem edit = new MenuItem("Edit");
        MenuItem del = new MenuItem("Delete");

        edit.setOnAction(e -> { if (onEdit != null) onEdit.accept(c); });
        del.setOnAction(e -> { if (onDelete != null) onDelete.accept(c); });

        menu.getItems().addAll(edit, del);

        setGraphic(row);
    }

    private String safe(String s) { return s == null ? "" : s; }

    private String formatTimeAgo(LocalDateTime createdAt) {
        if (createdAt == null) return "just now";

        Duration d = Duration.between(createdAt, LocalDateTime.now());
        long seconds = Math.max(0, d.getSeconds());

        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hours ago";
        long days = hours / 24;
        if (days < 7) return days + " days ago";
        long weeks = days / 7;
        if (weeks < 4) return weeks + " weeks ago";
        long months = days / 30;
        if (months < 12) return months + " months ago";
        long years = days / 365;
        return years + " years ago";
    }
}