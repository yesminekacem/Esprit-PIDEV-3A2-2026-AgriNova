package tn.esprit.utils;

import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;

//SessionManager.getInstance().getCurrentUser().
public class SessionManager {
    private static SessionManager instance;
    private User currentUser;
    private long loginTime;

    private SessionManager() {
        // Private constructor for singleton
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Login a user and start session
     */
    public void login(User user) {
        this.currentUser = user;
        this.loginTime = System.currentTimeMillis();
        System.out.println("Session started for: " + user.getEmail() + " at " + new java.util.Date());
    }

    /**
     * Logout current user and clear session
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("Session ended for: " + currentUser.getEmail());
        }
        this.currentUser = null;
        this.loginTime = 0;
    }

    /**
     * Get currently logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Get session duration in minutes
     */
    public long getSessionDuration() {
        if (!isLoggedIn()) return 0;
        return (System.currentTimeMillis() - loginTime) / 1000 / 60;
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return isLoggedIn() && currentUser.getRole() == Role.ADMIN;
    }

    /**
     * Get current user's full name
     */
    public String getCurrentUserName() {
        return isLoggedIn() ? currentUser.getFullName() : "Guest";
    }

    /**
     * Get current user's email
     */
    public String getCurrentUserEmail() {
        return isLoggedIn() ? currentUser.getEmail() : "";
    }

    /**
     * Update current user in session (after profile edit)
     */
    public void updateCurrentUser(User updatedUser) {
        if (isLoggedIn() && currentUser.getId() == updatedUser.getId()) {
            this.currentUser = updatedUser;
            System.out.println("Session user updated: " + updatedUser.getEmail());
        }
    }
}
