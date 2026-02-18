package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Comment;
import tn.esprit.forum.entity.Post;

public class TestCommentCrud {
    public static void main(String[] args) throws Exception {

        PostDao postDao = new PostDao();
        CommentDao commentDao = new CommentDao();

        // We need a valid post id for FK (comment.id_post)
        Post p = new Post();
        p.setTitle("Temp Post for Comment Test");
        p.setContent("Will be deleted at the end.");
        p.setAuthor("twitie");
        p.setCategory("Testing");
        p.setStatus("ACTIVE");
        p.setAuthorId(1);

        int postId = postDao.add(p);
        System.out.println("✅ Created temp post id = " + postId);

        // 1) CREATE comment
        Comment c = new Comment();
        c.setIdPost(postId);
        c.setContent("CRUD Test Comment");
        c.setAuthor("twitie");
        c.setLikes(0);

        int commentId = commentDao.add(c);
        System.out.println("✅ Created comment id = " + commentId);

        // 2) READ
        Comment created = commentDao.getById(commentId);
        System.out.println("READ created = " + created);
        if (created == null) throw new RuntimeException("❌ Comment not found. ID=" + commentId);

        // 3) UPDATE
        created.setContent("UPDATED comment content");
        created.setAuthor("updated-author");
        created.setLikes(7);
        commentDao.update(created);

        // 4) READ after update
        Comment updated = commentDao.getById(commentId);
        System.out.println("✅ After update = " + updated);

        if (updated == null || !"UPDATED comment content".equals(updated.getContent())) {
            throw new RuntimeException("❌ UPDATE FAILED");
        }
        System.out.println("✅ UPDATE confirmed");

        // 5) LIKE (increment)
        commentDao.like(commentId);
        Comment liked = commentDao.getById(commentId);
        System.out.println("✅ After like = " + liked);

        if (liked.getLikes() != updated.getLikes() + 1) {
            throw new RuntimeException("❌ LIKE FAILED");
        }
        System.out.println("✅ LIKE confirmed");

        // 6) DELETE comment
        commentDao.delete(commentId);
        Comment deleted = commentDao.getById(commentId);
        System.out.println("After delete getById = " + deleted);

        if (deleted != null) throw new RuntimeException("❌ DELETE FAILED");
        System.out.println("✅ DELETE confirmed");

        // Cleanup: delete temp post (should be safe)
        postDao.delete(postId);
        System.out.println("✅ Temp post deleted");

        System.out.println("✅ COMMENT CRUD fully validated.");
    }
}
