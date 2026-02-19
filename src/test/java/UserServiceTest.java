import org.junit.jupiter.api.*;
import tn.esprit.user.entity.User;
import tn.esprit.user.entity.Role;
import tn.esprit.user.service.UserCrud;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    static UserCrud service;
    static int testUserId = -1;

    @BeforeAll
    static void setup() {
        service = new UserCrud();
    }

    @Test
    @Order(1)
    void testAddUser() throws SQLException {
        cleanupEmail("test-user@esprit.tn");

        User u = new User();
        u.setEmail("test-user@esprit.tn");
        u.setFullName("Test User");
        u.setPasswordHash("hashed_test_123");
        u.setRole(Role.USER);

        service.add(u);
        User added = service.findByEmail("test-user@esprit.tn");
        assertNotNull(added);
        testUserId = added.getId();

        System.out.println("✅ Test1: Added user ID=" + testUserId);
    }

    @Test
    @Order(2)
    void testUpdateUser() throws SQLException {
        assertTrue(testUserId > 0, "Test1 must run first");

        cleanupEmail("modified@esprit.tn");

        User u = service.findById(testUserId);
        assertNotNull(u);

        u.setEmail("modified@esprit.tn");
        u.setFullName("Modified User");
        u.setPasswordHash("new_hash");
        u.setRole(Role.ADMIN);

        boolean result = service.update(u);
        assertTrue(result);

        User updated = service.findByEmail("modified@esprit.tn");
        assertNotNull(updated);
        assertEquals(testUserId, updated.getId());
        assertEquals("Modified User", updated.getFullName());
        assertEquals(Role.ADMIN, updated.getRole());

        System.out.println("✅ Test2: Updated user ID=" + testUserId);
    }

    @Test
    @Order(3)
    void testDeleteUser() throws SQLException {
        assertTrue(testUserId > 0, "Test1 must run first");

        User u = service.findByEmail("modified@esprit.tn");
        assertNotNull(u);
        assertEquals(testUserId, u.getId());

        boolean result = service.delete(testUserId);
        assertTrue(result);

        User deleted = service.findById(testUserId);
        assertNull(deleted);

        System.out.println("✅ Test3: Deleted user ID=" + testUserId);
    }

    @AfterAll  // ✅ Only after ALL tests
    static void cleanup() throws SQLException {
        if (testUserId > 0) {
            try {
                service.delete(testUserId);
                System.out.println("🧹 Final cleanup ID=" + testUserId);
            } catch (Exception e) {
                System.err.println("Cleanup failed: " + e.getMessage());
            }
        }
        cleanupEmail("test-user@esprit.tn");
        cleanupEmail("modified@esprit.tn");
    }

    private static void cleanupEmail(String email) throws SQLException {
        User existing = service.findByEmail(email);
        if (existing != null) {
            service.delete(existing.getId());
            System.out.println("🧹 Cleaned: " + email);
        }
    }
}
