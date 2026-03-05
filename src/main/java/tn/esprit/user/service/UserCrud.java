package tn.esprit.user.service;

import tn.esprit.user.entity.Role;
import tn.esprit.user.entity.User;
import tn.esprit.utils.DbConnect;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserCrud {

    private static final String INSERT_SQL =
            "INSERT INTO `user`(full_name,email,password,role,email_verified) VALUES (?,?,?,?,?)";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, full_name, email, password, role, profile_image, email_verified, face_data, banned FROM `user` WHERE id=?";

    private static final String SELECT_BY_EMAIL_SQL =
            "SELECT id, full_name, email, password, role, profile_image, email_verified, face_data, banned FROM `user` WHERE email=?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, full_name, email, password, role, profile_image, email_verified, face_data, banned FROM `user`";

    private static final String UPDATE_SQL =
            "UPDATE `user` SET full_name=?, email=?, password=?, role=?, profile_image=?, email_verified=?, face_data=?, banned=? WHERE id=?";

    private static final String UPDATE_BAN_SQL =
            "UPDATE `user` SET banned=? WHERE id=?";

    private static final String UPDATE_EMAIL_VERIFICATION_SQL =
            "UPDATE `user` SET email_verified=? WHERE email=?";

    private static final String DELETE_SQL =
            "DELETE FROM `user` WHERE id=?";

    // CREATE - includes email_verified field
    public void add(User u) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(INSERT_SQL)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isEmailVerified());
            ps.executeUpdate();
        }
    }

    // READ by id ✅ Updated
    public User findById(int id) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRowToUser(rs);
            }
        }
    }

    // READ by email ✅ Updated
    public User findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(SELECT_BY_EMAIL_SQL)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRowToUser(rs);
            }
        }
    }

    // READ all ✅ Updated
    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();

        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        }
        return list;
    }

    // UPDATE - Updated to include profile_image, email_verified, face_data, banned
    public boolean update(User u) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(UPDATE_SQL)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole().name());
            ps.setString(5, u.getProfileImage());
            ps.setBoolean(6, u.isEmailVerified());
            ps.setString(7, u.getFaceData());
            ps.setBoolean(8, u.isBanned());
            ps.setInt(9, u.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // TOGGLE BAN
    public boolean setBanned(int id, boolean banned) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(UPDATE_BAN_SQL)) {
            ps.setBoolean(1, banned);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // UPDATE EMAIL VERIFICATION STATUS
    public boolean updateEmailVerification(String email, boolean verified) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(UPDATE_EMAIL_VERIFICATION_SQL)) {
            ps.setBoolean(1, verified);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        }
    }

    // DELETE ✅ No change
    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Helper mapper - Updated to include email_verified, face_data, banned
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setProfileImage(rs.getString("profile_image"));
        u.setEmailVerified(rs.getBoolean("email_verified"));
        u.setFaceData(rs.getString("face_data"));
        try { u.setBanned(rs.getBoolean("banned")); } catch (SQLException ignored) {}
        return u;
    }
}
