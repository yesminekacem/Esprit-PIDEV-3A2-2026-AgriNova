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
            "INSERT INTO `user`(full_name,email,password,role) VALUES (?,?,?,?)";

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, full_name, email, password, role FROM `user` WHERE id=?";

    private static final String SELECT_BY_EMAIL_SQL =
            "SELECT id, full_name, email, password, role FROM `user` WHERE email=?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, full_name, email, password, role FROM `user`";

    private static final String UPDATE_SQL =
            "UPDATE `user` SET full_name=?, email=?, password=?, role=? WHERE id=?";

    private static final String DELETE_SQL =
            "DELETE FROM `user` WHERE id=?";

    // CREATE
    public void add(User u) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(INSERT_SQL)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash()); // maps to DB column: password
            ps.setString(4, u.getRole().name());  // enum -> String
            ps.executeUpdate();
        }
    }

    // READ by id
    public User findById(int id) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRowToUser(rs);
            }
        }
    }

    // READ by email
    public User findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(SELECT_BY_EMAIL_SQL)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapRowToUser(rs);
            }
        }
    }

    // READ all
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

    // UPDATE
    public boolean update(User u) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(UPDATE_SQL)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash()); // maps to DB column: password
            ps.setString(4, u.getRole().name());
            ps.setInt(5, u.getId());

            return ps.executeUpdate() > 0;
        }
    }

    // DELETE
    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = DbConnect.getInstance().getConnection().prepareStatement(DELETE_SQL)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Helper mapper
    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password")); // DB column is named password
        u.setRole(Role.valueOf(rs.getString("role"))); // String -> enum
        return u;
    }
}
