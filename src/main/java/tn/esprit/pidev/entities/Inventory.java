package tn.esprit.pidev.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity: Farm equipment/item that can be tracked and rented out.
 * PIDEV - AgriRent System
 */
public class Inventory {
    private int inventoryId;
    private String itemName;
    private ItemType itemType;
    private String description;
    private int quantity;
    private double unitPrice;
    private LocalDate purchaseDate;
    private ConditionStatus conditionStatus;
    private boolean isRentable;
    private double rentalPricePerDay;
    private InventoryRentalStatus rentalStatus;
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    private int totalUsageHours;
    private String ownerName;
    private String ownerContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imagePath;

    /**
     * Calculates total value as quantity * unitPrice.
     *
     * @return total value
     */
    public double calculateTotalValue() {
        return quantity * unitPrice;
    }

    /**
     * Checks if maintenance is due within 7 days.
     *
     * @return true if next maintenance date is within 7 days
     */
    public boolean isMaintenanceDueSoon() {
        if (nextMaintenanceDate == null) return false;
        return !LocalDate.now().plusDays(7).isBefore(nextMaintenanceDate);
    }

    /**
     * Returns true if item is rentable and status is AVAILABLE.
     *
     * @return true if available for rent
     */
    public boolean isAvailableForRent() {
        return isRentable && rentalStatus == InventoryRentalStatus.AVAILABLE;
    }

    /**
     * Potential monthly income if rented every day (rentalPricePerDay * 30).
     *
     * @return potential monthly income, 0 if not rentable
     */
    public double calculatePotentialMonthlyIncome() {
        return isRentable ? rentalPricePerDay * 30 : 0;
    }

    // --- Getters and Setters ---
    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public ConditionStatus getConditionStatus() { return conditionStatus; }
    public void setConditionStatus(ConditionStatus conditionStatus) { this.conditionStatus = conditionStatus; }
    public boolean isRentable() { return isRentable; }
    public void setRentable(boolean rentable) { isRentable = rentable; }
    public double getRentalPricePerDay() { return rentalPricePerDay; }
    public void setRentalPricePerDay(double rentalPricePerDay) { this.rentalPricePerDay = rentalPricePerDay; }
    public InventoryRentalStatus getRentalStatus() { return rentalStatus; }
    public void setRentalStatus(InventoryRentalStatus rentalStatus) { this.rentalStatus = rentalStatus; }
    public LocalDate getLastMaintenanceDate() { return lastMaintenanceDate; }
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) { this.lastMaintenanceDate = lastMaintenanceDate; }
    public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }
    public int getTotalUsageHours() { return totalUsageHours; }
    public void setTotalUsageHours(int totalUsageHours) { this.totalUsageHours = totalUsageHours; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerContact() { return ownerContact; }
    public void setOwnerContact(String ownerContact) { this.ownerContact = ownerContact; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return "Inventory{id=" + inventoryId + ", name='" + itemName + "', type=" + itemType + ", qty=" + quantity + ", status=" + rentalStatus + "}";
    }
}
