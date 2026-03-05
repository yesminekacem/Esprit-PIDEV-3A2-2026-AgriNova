package tn.esprit.inventory.services;

import tn.esprit.inventory.dao.InventoryDao;
import tn.esprit.inventory.entities.Inventory;
import tn.esprit.inventory.entities.InventoryRentalStatus;
import tn.esprit.inventory.entities.ItemType;

import java.util.List;

/**
 * Service layer for Inventory: validation and business rules. PIDEV - AgriRent.
 */
public class InventoryService {

    private final InventoryDao inventoryDao = new InventoryDao();

    /**
     * Adds inventory after validation: name not empty, quantity > 0, unitPrice >= 0, if rentable then rentalPricePerDay > 0.
     *
     * @param inventory entity to add
     * @return true if added successfully
     */
    public boolean addInventory(Inventory inventory) {
        if (inventory == null) {
            System.out.println("[InventoryService] Cannot add null inventory.");
            return false;
        }
        if (inventory.getItemName() == null || inventory.getItemName().trim().isEmpty()) {
            System.out.println("[InventoryService] Item name is required.");
            return false;
        }
        if (inventory.getQuantity() <= 0) {
            System.out.println("[InventoryService] Quantity must be greater than 0.");
            return false;
        }
        if (inventory.getUnitPrice() < 0) {
            System.out.println("[InventoryService] Unit price must be >= 0.");
            return false;
        }
        if (inventory.isRentable() && inventory.getRentalPricePerDay() <= 0) {
            System.out.println("[InventoryService] Rental price per day must be > 0 when item is rentable.");
            return false;
        }
        if (inventory.getRentalStatus() == null) {
            inventory.setRentalStatus(InventoryRentalStatus.AVAILABLE);
        }
        inventoryDao.create(inventory);
        return inventory.getInventoryId() > 0;
    }

    /**
     * Updates inventory after existence check.
     *
     * @param inventory entity to update
     * @return true if updated
     */
    public boolean updateInventory(Inventory inventory) {
        if (inventory == null || inventory.getInventoryId() <= 0) {
            System.out.println("[InventoryService] Valid inventory with ID required.");
            return false;
        }
        if (inventoryDao.getById(inventory.getInventoryId()) == null) {
            System.out.println("[InventoryService] Inventory not found for id=" + inventory.getInventoryId());
            return false;
        }
        if (inventory.getItemName() == null || inventory.getItemName().trim().isEmpty()) {
            System.out.println("[InventoryService] Item name is required.");
            return false;
        }
        if (inventory.getQuantity() <= 0) {
            System.out.println("[InventoryService] Quantity must be greater than 0.");
            return false;
        }
        if (inventory.getUnitPrice() < 0) {
            System.out.println("[InventoryService] Unit price must be >= 0.");
            return false;
        }
        if (inventory.isRentable() && inventory.getRentalPricePerDay() <= 0) {
            System.out.println("[InventoryService] Rental price per day must be > 0 when rentable.");
            return false;
        }
        inventoryDao.update(inventory);
        return true;
    }

    /**
     * Deletes inventory by id. Prevents delete if status is RENTED_OUT.
     *
     * @param id inventory id
     * @return true if deleted
     */
    public boolean deleteInventory(int id) {
        Inventory inv = inventoryDao.getById(id);
        if (inv == null) {
            System.out.println("[InventoryService] Inventory not found for id=" + id);
            return false;
        }
        if (inv.getRentalStatus() == InventoryRentalStatus.RENTED_OUT) {
            System.out.println("[InventoryService] Cannot delete: item is currently rented out.");
            return false;
        }
        inventoryDao.delete(id);
        return true;
    }

    public List<Inventory> getAllInventory() {
        return inventoryDao.getAll();
    }

    public List<Inventory> searchInventory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return inventoryDao.getAll();
        return inventoryDao.searchByName(keyword.trim());
    }

    public List<Inventory> getAvailableForRent() {
        return inventoryDao.getAvailableRentableItems();
    }

    public List<Inventory> getItemsByType(ItemType type) {
        if (type == null) return inventoryDao.getAll();
        return inventoryDao.getByType(type);
    }

    public List<Inventory> getMaintenanceAlerts() {
        return inventoryDao.getItemsNeedingMaintenance();
    }

    public double getTotalInventoryValue() {
        return inventoryDao.getTotalInventoryValue();
    }

    /**
     * Statistics inner class and method.
     */
    public static class InventoryStatistics {
        public int totalItems;
        public double totalValue;
        public int rentableItems;
        public int currentlyRented;
        public int availableForRent;
        public int needMaintenance;

        @Override
        public String toString() {
            return String.format("Total items: %d | Total value: %.2f | Rentable: %d | Rented: %d | Available for rent: %d | Need maintenance: %d",
                    totalItems, totalValue, rentableItems, currentlyRented, availableForRent, needMaintenance);
        }
    }

    public InventoryStatistics getStatistics() {
        List<Inventory> all = inventoryDao.getAll();
        InventoryStatistics s = new InventoryStatistics();
        s.totalItems = all.size();
        s.totalValue = inventoryDao.getTotalInventoryValue();
        s.rentableItems = (int) all.stream().filter(Inventory::isRentable).count();
        s.currentlyRented = (int) all.stream().filter(i -> i.getRentalStatus() == InventoryRentalStatus.RENTED_OUT).count();
        s.availableForRent = inventoryDao.getAvailableRentableItems().size();
        s.needMaintenance = inventoryDao.getItemsNeedingMaintenance().size();
        return s;
    }
}
