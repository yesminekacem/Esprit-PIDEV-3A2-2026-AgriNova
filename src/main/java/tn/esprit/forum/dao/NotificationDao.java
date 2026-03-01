package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Notification;
import tn.esprit.utils.DbConnect;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {

    private final Connection cnx;

    public NotificationDao() throws SQLException {
        cnx = DbConnect.getInstance().getConnection(); // ✅ SAME AS PostDao
    }

    public void add(Notification n) throws SQLException {
        String sql = """
                INSERT INTO notifications(recipient_id, actor_id, post_id, type, message, is_read)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getRecipientId());
            ps.setInt(2, n.getActorId());
            ps.setInt(3, n.getPostId());
            ps.setString(4, n.getType());
            ps.setString(5, n.getMessage());
            ps.setBoolean(6, n.isRead());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) n.setId(rs.getInt(1));
            }
        }
    }

    public int countUnread(int recipientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND is_read = 0";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, recipientId);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
    public List<Notification> findByRecipient(int recipientId) throws SQLException {
        String sql = "SELECT * FROM notifications WHERE recipient_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();

        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, recipientId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void markAsRead(int id) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, id);
            st.executeUpdate();
        }
    }

    public void markAllAsRead(int recipientId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = 1 WHERE recipient_id = ? AND is_read = 0";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, recipientId);
            st.executeUpdate();
        }
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setRecipientId(rs.getInt("recipient_id"));
        n.setActorId(rs.getInt("actor_id"));
        n.setPostId(rs.getInt("post_id"));
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));

        Timestamp ts = rs.getTimestamp("created_at");
        n.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());

        return n;
    }

}