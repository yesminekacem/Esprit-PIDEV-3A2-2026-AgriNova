package tn.esprit.forum.dao;

import tn.esprit.forum.entity.Post;

public class TestPostCrud {
    public static void main(String[] args) throws Exception {

        PostDao postDao = new PostDao();

        // 1) CREATE
        Post p = new Post();
        p.setTitle("CRUD Test Post");
        p.setContent("This post will be updated then deleted.");
        p.setAuthor("twitie");
        p.setCategory("Testing");
        p.setStatus("ACTIVE");
        p.setAuthorId(1);

        int newId = postDao.add(p);
        System.out.println("✅ Created new post id = " + newId);

        // 2) READ
        Post created = postDao.getById(newId);
        System.out.println("READ created = " + created);

        if (created == null) throw new RuntimeException("❌ Created post not found. ID=" + newId);

        // 3) UPDATE
        created.setTitle("UPDATED Title");
        created.setContent("UPDATED Content");
        created.setCategory("UpdatedCategory");
        postDao.update(created);

        // 4) READ after update
        Post updated = postDao.getById(newId);
        System.out.println("✅ After update = " + updated);

        if (updated == null || !"UPDATED Title".equals(updated.getTitle())) {
            throw new RuntimeException("❌ UPDATE FAILED");
        }
        System.out.println("✅ UPDATE confirmed");

        // 5) DELETE
        postDao.delete(newId);

        // 6) READ after delete
        Post deleted = postDao.getById(newId);
        System.out.println("After delete getById = " + deleted);

        if (deleted != null) throw new RuntimeException("❌ DELETE FAILED");
        System.out.println("✅ DELETE confirmed");

        System.out.println("✅ POST CRUD fully validated.");
    }
}
