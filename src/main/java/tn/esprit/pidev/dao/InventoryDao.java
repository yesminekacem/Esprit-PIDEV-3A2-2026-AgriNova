package tn.esprit.pidev.dao;

import tn.esprit.pidev.entities.ConditionStatus;
import tn.esprit.pidev.entities.Inventory;
import tn.esprit.pidev.entities.InventoryRentalStatus;
import tn.esprit.pidev.entities.ItemType;
import tn.esprit.pidev.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for Inventory entity. CRUD + search, available rentable, by type, maintenance, total value.
 * Uses PreparedStatement and DatabaseConnection singleton. PIDEV - AgriRent.
 */
public class InventoryDao implements IDao<Inventory> {

    @Override
    public void create(Inventory entity) {
        String sql = "INSERT INTO inventory (item_name, item_type, description, quantity, unit_price, purchase_date, " +
                "condition_status, is_rentable, rental_price_per_day, rental_status, last_maintenance_date, next_maintenance_date, " +
                "total_usage_hours, owner_name, owner_contact, image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return;
            setInsertParams(ps, entity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) entity.setInventoryId(rs.getInt(1));
            }
            System.out.println("[InventoryDao] Item created successfully. ID: " + entity.getInventoryId());
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR create: " + e.getMessage());
        }
    }

    @Override
    public Inventory getById(int id) {
        String sql = "SELECT * FROM inventory WHERE inventory_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return null;
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Inventory> getAll() {
        List<Inventory> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY inventory_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            System.out.println("[InventoryDao] getAll: " + list.size() + " items.");
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Inventory entity) {
        String sql = "UPDATE inventory SET item_name=?, item_type=?, description=?, quantity=?, unit_price=?, purchase_date=?, " +
                "condition_status=?, is_rentable=?, rental_price_per_day=?, rental_status=?, last_maintenance_date=?, next_maintenance_date=?, " +
                "total_usage_hours=?, owner_name=?, owner_contact=?, image_path=? WHERE inventory_id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            setInsertParams(ps, entity);
            ps.setInt(17, entity.getInventoryId());
            int n = ps.executeUpdate();
            if (n > 0) System.out.println("[InventoryDao] Item updated successfully.");
            else System.out.println("[InventoryDao] No row updated for id=" + entity.getInventoryId());
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR update: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM inventory WHERE inventory_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return;
            ps.setInt(1, id);
            int n = ps.executeUpdate();
            if (n > 0) System.out.println("[InventoryDao] Item deleted successfully.");
            else System.out.println("[InventoryDao] No row deleted for id=" + id);
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR delete: " + e.getMessage());
        }
    }


    /**
     * LIKE search by item name.
     */
    public List<Inventory> searchByName(String name) {
        List<Inventory> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE item_name LIKE ? ORDER BY inventory_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR searchByName: " + e.getMessage());
        }
        return list;
    }

    /**
     * Items with is_rentable=true AND rental_status='AVAILABLE'.
     */
    public List<Inventory> getAvailableRentableItems() {
        List<Inventory> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE is_rentable = TRUE AND rental_status = 'AVAILABLE' ORDER BY item_name";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getAvailableRentableItems: " + e.getMessage());
        }
        return list;
    }

    /**
     * Filter by item type.
     */
    public List<Inventory> getByType(ItemType type) {
        List<Inventory> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE item_type = ? ORDER BY inventory_id";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getByType: " + e.getMessage());
        }
        return list;
    }

    /**
     * Items whose next_maintenance_date is within 7 days from today.
     */
    public List<Inventory> getItemsNeedingMaintenance() {
        List<Inventory> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE next_maintenance_date IS NOT NULL AND next_maintenance_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) ORDER BY next_maintenance_date";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return list;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getItemsNeedingMaintenance: " + e.getMessage());
        }
        return list;
    }

    /**
     * SUM(quantity * unit_price) over all inventory.
     */
    public double getTotalInventoryValue() {
        String sql = "SELECT COALESCE(SUM(quantity * unit_price), 0) AS total FROM inventory";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) return 0;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDao] ERROR getTotalInventoryValue: " + e.getMessage());
        }
        return 0;
    }

    private void setInsertParams(PreparedStatement ps, Inventory e) throws SQLException {
        ps.setString(1, e.getItemName());
        ps.setString(2, e.getItemType() != null ? e.getItemType().name() : null);
        ps.setString(3, e.getDescription());
        ps.setInt(4, e.getQuantity());
        ps.setDouble(5, e.getUnitPrice());
        ps.setObject(6, e.getPurchaseDate());
        ps.setString(7, e.getConditionStatus() != null ? e.getConditionStatus().name() : "GOOD");
        ps.setBoolean(8, e.isRentable());
        ps.setDouble(9, e.getRentalPricePerDay());
        ps.setString(10, e.getRentalStatus() != null ? e.getRentalStatus().name() : "AVAILABLE");
        ps.setObject(11, e.getLastMaintenanceDate());
        ps.setObject(12, e.getNextMaintenanceDate());
        ps.setInt(13, e.getTotalUsageHours());
        ps.setString(14, e.getOwnerName());
        ps.setString(15, e.getOwnerContact());
        ps.setString(16, e.getImagePath());
    }

    private Inventory mapRow(ResultSet rs) throws SQLException {
        Inventory i = new Inventory();
        i.setInventoryId(rs.getInt("inventory_id"));
        i.setItemName(rs.getString("item_name"));
        String it = rs.getString("item_type");
        if (it != null) i.setItemType(ItemType.valueOf(it));
        i.setDescription(rs.getString("description"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        Date pd = rs.getDate("purchase_date");
        i.setPurchaseDate(pd != null ? pd.toLocalDate() : null);
        String cs = rs.getString("condition_status");
        if (cs != null) i.setConditionStatus(ConditionStatus.valueOf(cs));
        i.setRentable(rs.getBoolean("is_rentable"));
        i.setRentalPricePerDay(rs.getDouble("rental_price_per_day"));
        String rsVal = rs.getString("rental_status");
        if (rsVal != null) i.setRentalStatus(InventoryRentalStatus.valueOf(rsVal));
        Date lm = rs.getDate("last_maintenance_date");
        i.setLastMaintenanceDate(lm != null ? lm.toLocalDate() : null);
        Date nm = rs.getDate("next_maintenance_date");
        i.setNextMaintenanceDate(nm != null ? nm.toLocalDate() : null);
        i.setTotalUsageHours(rs.getInt("total_usage_hours"));
        i.setOwnerName(rs.getString("owner_name"));
        i.setOwnerContact(rs.getString("owner_contact"));
        Timestamp ca = rs.getTimestamp("created_at");
        i.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        i.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        i.setImagePath(rs.getString("image_path"));
        return i;
    }
}

