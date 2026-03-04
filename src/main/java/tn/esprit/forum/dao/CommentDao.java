package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Comment;
import tn.esprit.utils.DbConnect;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDao {

    private final Connection cnx;

    public CommentDao() throws SQLException {
        cnx = DbConnect.getInstance().getConnection();
    }

    public int add(Comment c) throws SQLException {
        String sql = "INSERT INTO comment (id_post, content, author, author_id, likes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement st = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setInt(1, c.getIdPost());
            st.setString(2, c.getContent());
            st.setString(3, c.getAuthor());
            st.setInt(4, c.getAuthorId());
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
            st.setInt(4, c.getAuthorId());
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

    public List<Comment> getByPost(int idPost) throws SQLException {
        String sql = "SELECT * FROM comment WHERE id_post=? ORDER BY created_at ASC";
        List<Comment> comments = new ArrayList<>();
        try (PreparedStatement st = cnx.prepareStatement(sql)) {
            st.setInt(1, idPost);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapComment(rs));
                }
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
        c.setAuthorId(rs.getInt("author_id"));
        c.setLikes(rs.getInt("likes"));

        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return c;
    }
    public boolean hasFlaggedComments(int postId) throws SQLException {
        String sql = "SELECT 1 FROM comment WHERE id_post = ? AND content LIKE ? LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setString(2, "%***%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
