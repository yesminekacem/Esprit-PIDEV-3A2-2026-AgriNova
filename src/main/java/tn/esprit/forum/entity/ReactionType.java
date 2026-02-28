package tn.esprit.forum.entity;

public enum ReactionType {
    LIKE("Like"),
    LOVE("Love"),
    HAHA("Haha"),
    WOW("Wow"),
    SAD("Sad"),
    ANGRY("Angry");

    public final String displayName;

    ReactionType(String displayName) {
        this.displayName = displayName;
    }
}