package tn.esprit.user.entity;

public class User {
    private int id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role;
    private String profileImage;
    private boolean emailVerified;
    private String faceData;
    private boolean banned;

    public User() {}

    public User(int id, String fullName, String email, String passwordHash, Role role, String profileImage) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.profileImage = profileImage;
        this.emailVerified = false; // Default to false for new users
    }

    public User(int id, String fullName, String email, String passwordHash, Role role) {
        this(id, fullName, email, passwordHash, role, null);

    }
    public User(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.profileImage = null;
        this.emailVerified = false; // Default to false for new users
    }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getFaceData() { return faceData; }
    public void setFaceData(String faceData) { this.faceData = faceData; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public String getStatus() { return banned ? "Banned" : "Active"; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash.substring(0, 10) + "..." + '\'' +
                ", role=" + role +
                ", profileImage='" + (profileImage != null ? profileImage.substring(0, 30) + "..." : "null") + '\'' +
                '}';
    }
}
