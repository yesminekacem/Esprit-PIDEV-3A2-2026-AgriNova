package tn.esprit.forum.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.forum.entity.Post;

public class TopicCardController {

    @FXML private Label lblAvatar;
    @FXML private Label lblTitle;
    @FXML private Label lblAuthor;
    @FXML private Label lblRole;
    @FXML private Label lblTimeAgo;
    @FXML private Label lblCategoryChip;
    @FXML private Label lblExcerpt;
    @FXML private Label lblReplies;
    @FXML private Label lblLikes;
    @FXML private Label badgePopular;

    private Post post;
    private Runnable openAction;

    public void setData(Post p, Runnable openAction) {
        this.post = p;
        this.openAction = openAction;

        lblTitle.setText(safe(p.getTitle()));
        lblAuthor.setText(safe(p.getAuthor()));
        lblRole.setText("Farm Member"); // (optional) map from your user table if you have it
        lblTimeAgo.setText("recent");   // (optional) compute from createdAt if you want

        lblExcerpt.setText(safe(p.getContent()).length() > 120
                ? safe(p.getContent()).substring(0, 120) + "..."
                : safe(p.getContent()));

        lblReplies.setText("0 replies"); // if you have comment count, set it here
        lblLikes.setText("0 likes");     // if you store likes, set it here

        String avatar = safe(p.getAuthor());
        lblAvatar.setText(avatar.isEmpty() ? "?" : ("" + Character.toUpperCase(avatar.charAt(0))));

        // Category chip color classes
        lblCategoryChip.setText(safe(p.getCategory()));
        lblCategoryChip.getStyleClass().removeIf(s -> s.startsWith("chip-"));
        lblCategoryChip.getStyleClass().add(mapCategoryClass(p.getCategory()));

        // Popular badge (optional logic)
        boolean popular = false; // decide your rule (likes > X, replies > Y...)
        badgePopular.setVisible(popular);
        badgePopular.setManaged(popular);
    }

    private String mapCategoryClass(String category) {
        if (category == null) return "chip-gray";
        return switch (category.trim().toLowerCase()) {
            case "organic farming" -> "chip-green";
            case "soil management" -> "chip-amber";
            case "water management" -> "chip-blue";
            case "harvesting" -> "chip-orange";
            case "crop management" -> "chip-emerald";
            case "equipment" -> "chip-gray";
            default -> "chip-gray";
        };
    }

    private String safe(String s) { return s == null ? "" : s; }

    @FXML
    private void onOpen() {
        if (openAction != null) openAction.run();
    }

    public Post getPost() { return post; }
}
