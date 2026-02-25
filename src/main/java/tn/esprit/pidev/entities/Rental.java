package tn.esprit.pidev.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Rental contract between owner and renter. PIDEV - AgriRent.
 */
public class Rental {
    private int rentalId;
    private int inventoryId;
    private String ownerName;
    private String renterName;
    private String renterContact;
    private String renterAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualReturnDate;
    private double dailyRate;
    private int totalDays;
    private double totalCost;
    private double securityDeposit;
    private double lateFee;
    private boolean requiresDelivery;
    private double deliveryFee;
    private String deliveryAddress;
    private RentalStatus rentalStatus;
    private String pickupCondition;
    private String returnCondition;
    private String pickupPhotos;
    private String returnPhotos;
    private String damageNotes;
    private Integer ownerRating;
    private Integer renterRating;
    private String ownerReview;
    private String renterReview;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Inventory inventory;

    /**
     * Calculates rental duration in days between start and end (inclusive logic: end - start in days).
     *
     * @param start start date
     * @param end   end date
     * @return number of days
     */
    public static long calculateDaysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Calculates total cost: dailyRate * totalDays + deliveryFee if required.
     *
     * @return total cost
     */
    public double calculateTotalCost() {
        return dailyRate * totalDays + (requiresDelivery ? deliveryFee : 0);
    }

    /**
     * Calculates late fee: 150% of daily rate per late day (if returned after endDate).
     *
     * @return late fee amount
     */
    public double calculateLateFee() {
        if (actualReturnDate == null || !actualReturnDate.isAfter(endDate)) return 0;
        long lateDays = ChronoUnit.DAYS.between(endDate, actualReturnDate);
        return lateDays * (dailyRate * 1.5);
    }

    /**
     * Checks if rental is overdue (ACTIVE, end date passed, not yet returned).
     *
     * @return true if overdue
     */
    public boolean isOverdue() {
        return rentalStatus == RentalStatus.ACTIVE && LocalDate.now().isAfter(endDate) && actualReturnDate == null;
    }

    /**
     * Days until return date (negative if overdue).
     *
     * @return days until end date from today
     */
    public long getDaysUntilReturn() {
        if (actualReturnDate != null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    /**
     * Human-readable status description.
     *
     * @return status description string
     */
    public String getStatusDescription() {
        switch (rentalStatus) {
            case PENDING: return "Awaiting owner approval";
            case APPROVED: return "Approved, not yet started";
            case ACTIVE: return "Currently rented";
            case RETURNED: return "Item returned, pending completion";
            case COMPLETED: return "Rental completed";
            case CANCELLED: return "Rental cancelled";
            case DISPUTED: return "Under dispute";
            default: return rentalStatus.toString();
        }
    }

    // --- Getters and Setters ---
    public int getRentalId() { return rentalId; }
    public void setRentalId(int rentalId) { this.rentalId = rentalId; }
    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }
    public String getRenterContact() { return renterContact; }
    public void setRenterContact(String renterContact) { this.renterContact = renterContact; }
    public String getRenterAddress() { return renterAddress; }
    public void setRenterAddress(String renterAddress) { this.renterAddress = renterAddress; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDate getActualReturnDate() { return actualReturnDate; }
    public void setActualReturnDate(LocalDate actualReturnDate) { this.actualReturnDate = actualReturnDate; }
    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public double getSecurityDeposit() { return securityDeposit; }
    public void setSecurityDeposit(double securityDeposit) { this.securityDeposit = securityDeposit; }
    public double getLateFee() { return lateFee; }
    public void setLateFee(double lateFee) { this.lateFee = lateFee; }
    public boolean isRequiresDelivery() { return requiresDelivery; }
    public void setRequiresDelivery(boolean requiresDelivery) { this.requiresDelivery = requiresDelivery; }
    public double getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(double deliveryFee) { this.deliveryFee = deliveryFee; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public RentalStatus getRentalStatus() { return rentalStatus; }
    public void setRentalStatus(RentalStatus rentalStatus) { this.rentalStatus = rentalStatus; }
    public String getPickupCondition() { return pickupCondition; }
    public void setPickupCondition(String pickupCondition) { this.pickupCondition = pickupCondition; }
    public String getReturnCondition() { return returnCondition; }
    public void setReturnCondition(String returnCondition) { this.returnCondition = returnCondition; }
    public String getPickupPhotos() { return pickupPhotos; }
    public void setPickupPhotos(String pickupPhotos) { this.pickupPhotos = pickupPhotos; }
    public String getReturnPhotos() { return returnPhotos; }
    public void setReturnPhotos(String returnPhotos) { this.returnPhotos = returnPhotos; }
    public String getDamageNotes() { return damageNotes; }
    public void setDamageNotes(String damageNotes) { this.damageNotes = damageNotes; }
    public Integer getOwnerRating() { return ownerRating; }
    public void setOwnerRating(Integer ownerRating) { this.ownerRating = ownerRating; }
    public Integer getRenterRating() { return renterRating; }
    public void setRenterRating(Integer renterRating) { this.renterRating = renterRating; }
    public String getOwnerReview() { return ownerReview; }
    public void setOwnerReview(String ownerReview) { this.ownerReview = ownerReview; }
    public String getRenterReview() { return renterReview; }
    public void setRenterReview(String renterReview) { this.renterReview = renterReview; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public String toString() {
        return "Rental{id=" + rentalId + ", renter='" + renterName + "', status=" + rentalStatus + ", cost=" + totalCost + "}";
    }
}
