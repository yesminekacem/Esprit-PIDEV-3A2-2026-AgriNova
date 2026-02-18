package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Comment;
import tn.esprit.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDao {
    private final Connection cnx;

    public CommentDao() {
        cnx = MyConnection.getInstance().getCnx();
    }

    public int add(Comment c) throws SQLException {
        // created_at is auto (DEFAULT current_timestamp()), so we don't include it.
        String sql = "INSERT INTO comment (id_post, content, author, author_id, likes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement st = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setInt(1, c.getIdPost());
            st.setString(2, c.getContent());
            st.setString(3, c.getAuthor());
            st.setInt(4, c.getAuthorId()); // ✅ REQUIRED by DB
            st.setInt(5, c.getLikes());

            st.executeUpdate();

            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Insert failed, no generated ID.");
    }

    public void update(Comment c) throws SQLException {
        String sql = "UPDATE comment SET id_post=?, content=?, author=?, author_id=?, likes=? WHERE id_comment=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, c.getIdPost());
            st.setString(2, c.getContent());
            st.setString(3, c.getAuthor());
            st.setInt(4, c.getAuthorId()); // ✅
            st.setInt(5, c.getLikes());
            st.setInt(6, c.getIdComment());
            st.executeUpdate();
        }
    }

    public void delete(int idComment) throws SQLException {
        String sql = "DELETE FROM comment WHERE id_comment=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idComment);
            st.executeUpdate();
        }
    }

    public Comment getById(int idComment) throws SQLException {
        String sql = "SELECT * FROM comment WHERE id_comment=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idComment);
            try (ResultSet rs = st.executeQuery()) {
                if (!rs.next()) return null;
                return mapComment(rs);
            }
        }
    }

    public List<Comment> getByPost(int idPost) throws SQLException {
        String sql = "SELECT * FROM comment WHERE id_post=? ORDER BY created_at ASC";
        List<Comment> comments = new ArrayList<>();
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idPost);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) comments.add(mapComment(rs));
            }
        }
        return comments;
    }

    public void like(int idComment) throws SQLException {
        String sql = "UPDATE comment SET likes = likes + 1 WHERE id_comment=?";
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idComment);
            st.executeUpdate();
        }
    }

    private Comment mapComment(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setIdComment(rs.getInt("id_comment"));
        c.setIdPost(rs.getInt("id_post"));
        c.setContent(rs.getString("content"));
        c.setAuthor(rs.getString("author"));
        c.setAuthorId(rs.getInt("author_id")); // ✅
        c.setLikes(rs.getInt("likes"));

        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());

        return c;
    }
}
