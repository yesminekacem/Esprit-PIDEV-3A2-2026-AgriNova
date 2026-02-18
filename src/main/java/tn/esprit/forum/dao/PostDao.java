package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Post;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
public class PostDao {
    private final Connection cnx;

    public PostDao() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public int add(Post p) throws SQLException {
        String sql = "INSERT INTO post (title, content, author, category, status, author_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, p.getTitle());
            st.setString(2, p.getContent());
            st.setString(3, p.getAuthor());
            st.setString(4, p.getCategory());
            st.setString(5, (p.getStatus() == null || p.getStatus().isBlank()) ? "ACTIVE" : p.getStatus());
            st.setInt(6, p.getAuthorId());

            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Insert failed, no generated ID.");
    }





    public void update(Post p) throws SQLException {
        String sql = "UPDATE post SET title=?, content=?, author=?, category=?, status=?, author_id=? WHERE id_post=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setString(1, p.getTitle());
            st.setString(2, p.getContent());
            st.setString(3, p.getAuthor());
            st.setString(4, p.getCategory());
            st.setString(5, p.getStatus());
            st.setInt(6, p.getAuthorId());
            st.setInt(7, p.getIdPost());
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

}
