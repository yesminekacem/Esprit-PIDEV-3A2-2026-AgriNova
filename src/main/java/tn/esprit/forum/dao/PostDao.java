package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Post;
import tn.esprit.utils.DbConnect;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.EnumMap;
import java.util.Map;
import tn.esprit.forum.entity.ReactionType;



public class PostDao {
    private final Connection cnx;

    public PostDao() throws SQLException {
        cnx = DbConnect.getInstance().getConnection();
    }

    public int add(Post p) throws SQLException {
        String sql = "INSERT INTO post (title, content, image_path, author, category, status, author_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, p.getTitle());
            st.setString(2, p.getContent());
            st.setString(3, p.getImagePath()); // ✅ NEW
            st.setString(4, p.getAuthor());
            st.setString(5, p.getCategory());
            st.setString(6, (p.getStatus() == null || p.getStatus().isBlank()) ? "ACTIVE" : p.getStatus());
            st.setInt(7, p.getAuthorId());
            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Insert failed, no generated ID.");
    }





    public void update(Post p) throws SQLException {
        String sql = "UPDATE post SET title=?, content=?, image_path=?, author=?, category=?, status=?, author_id=? WHERE id_post=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setString(1, p.getTitle());
            st.setString(2, p.getContent());
            st.setString(3, p.getImagePath()); // ✅ NEW
            st.setString(4, p.getAuthor());
            st.setString(5, p.getCategory());
            st.setString(6, p.getStatus());
            st.setInt(7, p.getAuthorId());
            st.setInt(8, p.getIdPost());
            st.executeUpdate();
        }
    }

    public void delete(int idPost) throws SQLException {
        String sql = "DELETE FROM post WHERE id_post=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idPost);
            st.executeUpdate();
        }
    }

    public Post getById(int idPost) throws SQLException {
        String sql = "SELECT * FROM post WHERE id_post=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idPost);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return mapPost(rs);
            }
        }
    }

    public List<Post> getAll() throws SQLException {
        String sql = "SELECT * FROM post ORDER BY created_at DESC";
        List<Post> posts = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) posts.add(mapPost(rs));
        }
        return posts;
    }

    private Post mapPost(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setIdPost(rs.getInt("id_post"));
        p.setTitle(rs.getString("title"));
        p.setContent(rs.getString("content"));
        p.setAuthor(rs.getString("author"));
        p.setCategory(rs.getString("category"));
        p.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        p.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        p.setAuthorId(rs.getInt("author_id"));
        p.setImagePath(rs.getString("image_path")); // ✅ NEW
        return p;
    }
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM post";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int getLastInsertedId() throws SQLException {
        String sql = "SELECT MAX(id_post) FROM post";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public void deleteOwned(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM post WHERE id_post = ? AND author_id = ?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            st.setInt(2, userId);
            int affected = st.executeUpdate();
            if (affected == 0) {
                throw new SQLException("You are not allowed to delete this post.");
            }
        }
    }

    public ReactionType getUserReaction(int postId, int userId) throws SQLException {
        String sql = "SELECT reaction FROM post_reaction WHERE id_post=? AND user_id=? LIMIT 1";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            st.setInt(2, userId);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                String r = rs.getString("reaction");
                if (r == null) return null;
                return ReactionType.valueOf(r.trim().toUpperCase());
            }
        }
    }

    public void setReaction(int postId, int userId, ReactionType reaction) throws SQLException {
        // MariaDB safe UPSERT:
        String sql = """
        INSERT INTO post_reaction (id_post, user_id, reaction)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE reaction = VALUES(reaction)
    """;
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            st.setInt(2, userId);
            st.setString(3, reaction.name());
            st.executeUpdate();
        }
    }

    public void removeReaction(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM post_reaction WHERE id_post=? AND user_id=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            st.setInt(2, userId);
            st.executeUpdate();
        }
    }

    public int countAllReactions(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM post_reaction WHERE id_post=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            try (ResultSet rs = st.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Map<ReactionType, Integer> countReactionsByType(int postId) throws SQLException {
        Map<ReactionType, Integer> map = new EnumMap<>(ReactionType.class);
        for (ReactionType t : ReactionType.values()) map.put(t, 0);

        String sql = "SELECT reaction, COUNT(*) cnt FROM post_reaction WHERE id_post=? GROUP BY reaction";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, postId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String r = rs.getString("reaction");
                    int cnt = rs.getInt("cnt");
                    if (r == null) continue;
                    ReactionType t = ReactionType.valueOf(r.trim().toUpperCase());
                    map.put(t, cnt);
                }
            }
        }
        return map;
    }
}
