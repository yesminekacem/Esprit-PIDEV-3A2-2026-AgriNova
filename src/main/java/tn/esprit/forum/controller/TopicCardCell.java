package tn.esprit.forum.controller;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import tn.esprit.forum.entity.Post;

import java.time.Duration;
import java.time.LocalDateTime;

public class TopicCardCell extends ListCell<Post> {

    private final HBox root = new HBox(16);

    private final StackPane avatarWrap = new StackPane();
    private final Label avatarLetter = new Label();

    private final VBox content = new VBox(8);

    private final HBox headerRow = new HBox(10);
    private final VBox titleMeta = new VBox(4);
    private final HBox metaRow = new HBox(8);

    private final Label lblTitle = new Label();
    private final Label lblAuthor = new Label();
    private final Label dot1 = new Label("•");
    private final Label lblRole = new Label("Farm Member"); // static like figma (you can map later)
    private final Label dot2 = new Label("•");
    private final Label lblTimeAgo = new Label();

    private final Region spacer = new Region();
    private final Label categoryChip = new Label();

    private final Label excerpt = new Label();

    public TopicCardCell() {
        // root card
        root.getStyleClass().add("topic-card");
        root.setPadding(new Insets(18));
        root.setFillHeight(true);

        // avatar
        avatarWrap.getStyleClass().add("topic-avatar");
        avatarWrap.setMinSize(48, 48);
        avatarWrap.setPrefSize(48, 48);
        avatarWrap.setMaxSize(48, 48);

        avatarLetter.getStyleClass().add("topic-avatar-text");
        avatarWrap.getChildren().add(avatarLetter);

        // title
        lblTitle.getStyleClass().add("topic-title");
        lblTitle.setWrapText(true);

        // meta
        lblAuthor.getStyleClass().add("topic-meta-strong");
        lblRole.getStyleClass().add("topic-meta");
        lblTimeAgo.getStyleClass().add("topic-meta");
        dot1.getStyleClass().add("topic-meta-dot");
        dot2.getStyleClass().add("topic-meta-dot");

        metaRow.getChildren().addAll(lblAuthor, dot1, lblRole, dot2, lblTimeAgo);
        metaRow.getStyleClass().add("topic-meta-row");

        titleMeta.getChildren().addAll(lblTitle, metaRow);

        // category chip
        categoryChip.getStyleClass().add("topic-chip");
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(titleMeta, spacer, categoryChip);

        // excerpt
        excerpt.getStyleClass().add("topic-excerpt");
        excerpt.setWrapText(true);

        content.getChildren().addAll(headerRow, excerpt);
        HBox.setHgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(avatarWrap, content);
    }

    @Override
    protected void updateItem(Post post, boolean empty) {
        super.updateItem(post, empty);

        if (empty || post == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        // Avatar letter
        String a = post.getAuthor() == null ? "?" : post.getAuthor().trim();
        avatarLetter.setText(a.isEmpty() ? "?" : ("" + Character.toUpperCase(a.charAt(0))));

        // Content
        lblTitle.setText(post.getTitle() == null ? "" : post.getTitle());

        lblAuthor.setText(post.getAuthor() == null ? "Unknown" : post.getAuthor());

        // time ago
        lblTimeAgo.setText(formatTimeAgo(post.getCreatedAt()));

        // excerpt (first ~140 chars like figma preview)
        String c = post.getContent() == null ? "" : post.getContent().trim();
        if (c.length() > 140) c = c.substring(0, 140) + "...";
        excerpt.setText(c);

        // category chip
        String cat = post.getCategory() == null ? "General" : post.getCategory();
        categoryChip.setText(cat);
        applyCategoryChipClass(cat);

        setText(null);
        setGraphic(root);
    }

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

    private void applyCategoryChipClass(String category) {
        categoryChip.getStyleClass().removeIf(s -> s.startsWith("chip-"));

        String cat = category.toLowerCase();
        if (cat.contains("organic")) categoryChip.getStyleClass().add("chip-green");
        else if (cat.contains("soil")) categoryChip.getStyleClass().add("chip-amber");
        else if (cat.contains("water")) categoryChip.getStyleClass().add("chip-blue");
        else if (cat.contains("harvest")) categoryChip.getStyleClass().add("chip-orange");
        else if (cat.contains("equip")) categoryChip.getStyleClass().add("chip-gray");
        else if (cat.contains("crop")) categoryChip.getStyleClass().add("chip-emerald");
        else categoryChip.getStyleClass().add("chip-gray");
    }
}
