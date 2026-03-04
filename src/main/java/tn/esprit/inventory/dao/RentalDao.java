package tn.esprit.inventory.dao;

import tn.esprit.inventory.entities.PaymentStatus;
import tn.esprit.inventory.entities.Rental;
import tn.esprit.inventory.entities.RentalStatus;
import tn.esprit.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Rental entity.
 *
 * ⚠ KEY ARCHITECTURAL NOTE — WHY approveRental / completeRental DO NOT CALL getById():
 *
 * DatabaseConnection is a singleton that returns the SAME Connection object every time.
 * Any method that wraps its connection in try-with-resources will CLOSE that shared object
 * when it exits. So if approveRental() held conn = getConnection(), then called getById()
 * (which also does try(Connection c = getConnection()...)), getById would close the very
 * same conn object. The subsequent conn.prepareStatement() would throw "connection closed"
 * and silently fail (caught by the catch block), giving the impression the dialog succeeded
 * but nothing was saved to the database.
 *
 * Fix: approveRental and completeRental do their own inline SELECT + UPDATE on a single
 * connection without delegating to any other DAO method.
 */
public class RentalDao implements IDao<Rental> {

    private final InventoryDao inventoryDao = new InventoryDao();

    // ─────────────────────────────────────────────────────────────
    //  CRUD
    // ─────────────────────────────────────────────────────────────

    @Override
    public void create(Rental entity) {
        String sql = "INSERT INTO rental (inventory_id, owner_name, renter_name, renter_contact, renter_address, " +
                "start_date, end_date, actual_return_date, daily_rate, total_days, total_cost, security_deposit, late_fee, " +
                "requires_delivery, delivery_fee, delivery_address, rental_status, pickup_condition, return_condition, " +
                "pickup_photos, return_photos, damage_notes, owner_rating, renter_rating, owner_review, renter_review, payment_status, payment_method) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return;
            setInsertParams(ps, entity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) entity.setRentalId(rs.getInt(1));
            }
            System.out.println("[RentalDao] Rental created. ID=" + entity.getRentalId());
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR create: " + e.getMessage());
        }
    }

    @Override
    public Rental getById(int id) {
        String sql = "SELECT * FROM rental WHERE rental_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return null;
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Rental r = mapRow(rs);
                    r.setInventory(inventoryDao.getById(r.getInventoryId()));
                    return r;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Rental> getAll() {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rental ORDER BY rental_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn != null ? conn.prepareStatement(sql) : null) {
            if (conn == null || ps == null) return list;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getAll: " + e.getMessage());
        }
        // Attach inventory AFTER closing ResultSet (avoids nested ResultSet issues)
        for (Rental r : list) r.setInventory(inventoryDao.getById(r.getInventoryId()));
        return list;
    }

    @Override
    public void update(Rental entity) {
        String sql = "UPDATE rental SET inventory_id=?, owner_name=?, renter_name=?, renter_contact=?, renter_address=?, " +
                "start_date=?, end_date=?, actual_return_date=?, daily_rate=?, total_days=?, total_cost=?, security_deposit=?, late_fee=?, " +
                "requires_delivery=?, delivery_fee=?, delivery_address=?, rental_status=?, pickup_condition=?, return_condition=?, " +
                "pickup_photos=?, return_photos=?, damage_notes=?, owner_rating=?, renter_rating=?, owner_review=?, renter_review=?, payment_status=?, payment_method=? " +
                "WHERE rental_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            setInsertParams(ps, entity);
            ps.setInt(29, entity.getRentalId());
            int n = ps.executeUpdate();
            System.out.println("[RentalDao] Rental update rows affected: " + n);
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR update: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM rental WHERE rental_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setInt(1, id);
            int n = ps.executeUpdate();
            System.out.println("[RentalDao] Rental delete rows affected: " + n);
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR delete: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  APPROVE  ← BUG FIX: no longer calls getById() internally
    // ─────────────────────────────────────────────────────────────

    /**
     * Approves a rental: rental_status → APPROVED, inventory rental_status → RENTED_OUT.
     *
     * IMPORTANT: Does NOT call getById() because that would close the shared singleton
     * connection before the UPDATE statements can execute. Everything is done inline.
     */
    public void approveRental(int rentalId) {
        // Use try-with-resources so the connection is properly closed at the end
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (conn == null) { System.err.println("[RentalDao] No DB connection."); return; }

            // Step 1: fetch current status + inventoryId inline (no getById call)
            String currentStatus = null;
            int inventoryId = -1;
            try (PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT rental_status, inventory_id FROM rental WHERE rental_id = ?")) {
                psCheck.setInt(1, rentalId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        currentStatus = rs.getString("rental_status");
                        inventoryId   = rs.getInt("inventory_id");
                    }
                }
            }

            if (currentStatus == null) {
                System.out.println("[RentalDao] approveRental: rental not found id=" + rentalId);
                return;
            }
            if (!RentalStatus.PENDING.name().equals(currentStatus)) {
                System.out.println("[RentalDao] approveRental: status is " + currentStatus + ", must be PENDING.");
                return;
            }

            // Step 2: run both UPDATEs in a transaction
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psRental = conn.prepareStatement(
                        "UPDATE rental SET rental_status = 'APPROVED' WHERE rental_id = ?")) {
                    psRental.setInt(1, rentalId);
                    int rows = psRental.executeUpdate();
                    System.out.println("[RentalDao] rental UPDATE rows=" + rows);
                }
                try (PreparedStatement psInv = conn.prepareStatement(
                        "UPDATE inventory SET rental_status = 'RENTED_OUT' WHERE inventory_id = ?")) {
                    psInv.setInt(1, inventoryId);
                    int rows = psInv.executeUpdate();
                    System.out.println("[RentalDao] inventory UPDATE rows=" + rows);
                }
                conn.commit();
                System.out.println("[RentalDao] ✅ Rental #" + rentalId + " approved.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[RentalDao] ERROR approveRental transaction: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR approveRental: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  COMPLETE  ← BUG FIX: no longer calls getById() internally
    // ─────────────────────────────────────────────────────────────

    /**
     * Completes a rental: sets actual_return_date, return_condition, owner_rating,
     * rental_status → COMPLETED, payment_status → FULLY_PAID, calculates late fee,
     * and frees inventory → AVAILABLE.
     *
     * IMPORTANT: Does NOT call getById() — same reason as approveRental above.
     */
    public void completeRental(int rentalId, String returnCondition, int ownerRating) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            if (conn == null) { System.err.println("[RentalDao] No DB connection."); return; }

            // Step 1: fetch end_date + inventoryId inline (no getById call)
            LocalDate endDate = null;
            double dailyRate = 0;
            int inventoryId  = -1;
            try (PreparedStatement psCheck = conn.prepareStatement(
                    "SELECT end_date, daily_rate, inventory_id FROM rental WHERE rental_id = ?")) {
                psCheck.setInt(1, rentalId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        Date ed = rs.getDate("end_date");
                        endDate     = ed != null ? ed.toLocalDate() : null;
                        dailyRate   = rs.getDouble("daily_rate");
                        inventoryId = rs.getInt("inventory_id");
                    }
                }
            }

            if (inventoryId == -1) {
                System.out.println("[RentalDao] completeRental: rental not found id=" + rentalId);
                return;
            }

            // Step 2: calculate late fee
            LocalDate today = LocalDate.now();
            double lateFee = 0;
            if (endDate != null && today.isAfter(endDate)) {
                long lateDays = java.time.temporal.ChronoUnit.DAYS.between(endDate, today);
                lateFee = lateDays * (dailyRate * 1.5);
                System.out.println("[RentalDao] Late fee calculated: " + lateFee + " TND (" + lateDays + " days)");
            }

            // Step 3: run both UPDATEs in a transaction
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psRental = conn.prepareStatement(
                        "UPDATE rental SET actual_return_date=?, return_condition=?, owner_rating=?, " +
                                "late_fee=?, rental_status='COMPLETED', payment_status='FULLY_PAID' WHERE rental_id=?")) {
                    psRental.setObject(1, today);
                    psRental.setString(2, returnCondition != null ? returnCondition : "OK");
                    psRental.setInt(3, Math.max(1, Math.min(5, ownerRating)));
                    psRental.setDouble(4, lateFee);
                    psRental.setInt(5, rentalId);
                    int rows = psRental.executeUpdate();
                    System.out.println("[RentalDao] rental UPDATE rows=" + rows);
                }
                try (PreparedStatement psInv = conn.prepareStatement(
                        "UPDATE inventory SET rental_status = 'AVAILABLE' WHERE inventory_id = ?")) {
                    psInv.setInt(1, inventoryId);
                    int rows = psInv.executeUpdate();
                    System.out.println("[RentalDao] inventory UPDATE rows=" + rows);
                }
                conn.commit();
                System.out.println("[RentalDao] ✅ Rental #" + rentalId + " completed. Late fee=" + lateFee);
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[RentalDao] ERROR completeRental transaction: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR completeRental: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  QUERIES
    // ─────────────────────────────────────────────────────────────

    public List<Rental> getRentalsByRenter(String renterName) {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rental WHERE renter_name LIKE ? ORDER BY rental_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            ps.setString(1, "%" + renterName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getRentalsByRenter: " + e.getMessage());
        }
        for (Rental r : list) r.setInventory(inventoryDao.getById(r.getInventoryId()));
        return list;
    }

    public List<Rental> getActiveRentals() {
        return getByStatus(RentalStatus.ACTIVE);
    }

    public List<Rental> getOverdueRentals() {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rental WHERE rental_status = 'ACTIVE' AND end_date < CURDATE() AND actual_return_date IS NULL ORDER BY end_date";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getOverdueRentals: " + e.getMessage());
        }
        for (Rental r : list) r.setInventory(inventoryDao.getById(r.getInventoryId()));
        return list;
    }

    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_cost), 0) AS total FROM rental WHERE rental_status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return 0;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getTotalRevenue: " + e.getMessage());
        }
        return 0;
    }

    private List<Rental> getByStatus(RentalStatus status) {
        List<Rental> list = new ArrayList<>();
        String sql = "SELECT * FROM rental WHERE rental_status = ? ORDER BY rental_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RentalDao] ERROR getByStatus: " + e.getMessage());
        }
        for (Rental r : list) r.setInventory(inventoryDao.getById(r.getInventoryId()));
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    //  PARAM MAPPING
    // ─────────────────────────────────────────────────────────────

    private void setInsertParams(PreparedStatement ps, Rental e) throws SQLException {
        ps.setInt(1, e.getInventoryId());
        ps.setString(2, e.getOwnerName());
        ps.setString(3, e.getRenterName());
        ps.setString(4, e.getRenterContact());
        ps.setString(5, e.getRenterAddress());
        ps.setObject(6, e.getStartDate());
        ps.setObject(7, e.getEndDate());
        ps.setObject(8, e.getActualReturnDate());
        ps.setDouble(9, e.getDailyRate());
        ps.setInt(10, e.getTotalDays());
        ps.setDouble(11, e.getTotalCost());
        ps.setDouble(12, e.getSecurityDeposit());
        ps.setDouble(13, e.getLateFee());
        ps.setBoolean(14, e.isRequiresDelivery());
        ps.setDouble(15, e.getDeliveryFee());
        ps.setString(16, e.getDeliveryAddress());
        ps.setString(17, e.getRentalStatus() != null ? e.getRentalStatus().name() : "PENDING");
        ps.setString(18, e.getPickupCondition());
        ps.setString(19, e.getReturnCondition());
        ps.setString(20, e.getPickupPhotos());
        ps.setString(21, e.getReturnPhotos());
        ps.setString(22, e.getDamageNotes());
        ps.setObject(23, e.getOwnerRating());
        ps.setObject(24, e.getRenterRating());
        ps.setString(25, e.getOwnerReview());
        ps.setString(26, e.getRenterReview());
        ps.setString(27, e.getPaymentStatus() != null ? e.getPaymentStatus().name() : "PENDING");
        ps.setString(28, e.getPaymentMethod());
    }

    private Rental mapRow(ResultSet rs) throws SQLException {
        Rental r = new Rental();
        r.setRentalId(rs.getInt("rental_id"));
        r.setInventoryId(rs.getInt("inventory_id"));
        r.setOwnerName(rs.getString("owner_name"));
        r.setRenterName(rs.getString("renter_name"));
        r.setRenterContact(rs.getString("renter_contact"));
        r.setRenterAddress(rs.getString("renter_address"));
        Date sd = rs.getDate("start_date");
        r.setStartDate(sd != null ? sd.toLocalDate() : null);
        Date ed = rs.getDate("end_date");
        r.setEndDate(ed != null ? ed.toLocalDate() : null);
        Date ard = rs.getDate("actual_return_date");
        r.setActualReturnDate(ard != null ? ard.toLocalDate() : null);
        r.setDailyRate(rs.getDouble("daily_rate"));
        r.setTotalDays(rs.getInt("total_days"));
        r.setTotalCost(rs.getDouble("total_cost"));
        r.setSecurityDeposit(rs.getDouble("security_deposit"));
        r.setLateFee(rs.getDouble("late_fee"));
        r.setRequiresDelivery(rs.getBoolean("requires_delivery"));
        r.setDeliveryFee(rs.getDouble("delivery_fee"));
        r.setDeliveryAddress(rs.getString("delivery_address"));
        String rsVal = rs.getString("rental_status");
        if (rsVal != null) r.setRentalStatus(RentalStatus.valueOf(rsVal));
        r.setPickupCondition(rs.getString("pickup_condition"));
        r.setReturnCondition(rs.getString("return_condition"));
        r.setPickupPhotos(rs.getString("pickup_photos"));
        r.setReturnPhotos(rs.getString("return_photos"));
        r.setDamageNotes(rs.getString("damage_notes"));
        int or2 = rs.getInt("owner_rating");
        r.setOwnerRating(rs.wasNull() ? null : or2);
        int rr = rs.getInt("renter_rating");
        r.setRenterRating(rs.wasNull() ? null : rr);
        r.setOwnerReview(rs.getString("owner_review"));
        r.setRenterReview(rs.getString("renter_review"));
        String psVal = rs.getString("payment_status");
        if (psVal != null) r.setPaymentStatus(PaymentStatus.valueOf(psVal));
        r.setPaymentMethod(rs.getString("payment_method"));
        Timestamp ca = rs.getTimestamp("created_at");
        r.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        r.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        return r;
    }
}