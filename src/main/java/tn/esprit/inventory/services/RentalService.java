package tn.esprit.inventory.services;

import tn.esprit.inventory.dao.InventoryDao;
import tn.esprit.inventory.dao.RentalDao;
import tn.esprit.inventory.entities.Inventory;
import tn.esprit.inventory.entities.PaymentStatus;
import tn.esprit.inventory.entities.Rental;
import tn.esprit.inventory.entities.RentalStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service layer for Rental: validation and business rules. PIDEV - AgriRent.
 */
public class RentalService {

    private final RentalDao rentalDao = new RentalDao();
    private final InventoryDao inventoryDao = new InventoryDao();

    /**
     * Validates and creates a rental request. Auto-calculates totalDays, totalCost, securityDeposit (50% of total).
     * Checks: renter name, dates not null, start not in past, end after start, inventory exists and available.
     *
     * @param rental rental to create
     * @return true if created
     */
    public boolean requestRental(Rental rental) {
        if (rental == null) {
            System.out.println("[RentalService] Cannot create null rental.");
            return false;
        }
        if (rental.getRenterName() == null || rental.getRenterName().trim().isEmpty()) {
            System.out.println("[RentalService] Renter name is required.");
            return false;
        }
        if (rental.getStartDate() == null || rental.getEndDate() == null) {
            System.out.println("[RentalService] Start and end dates are required.");
            return false;
        }
        if (rental.getStartDate().isBefore(LocalDate.now())) {
            System.out.println("[RentalService] Start date cannot be in the past.");
            return false;
        }
        if (!rental.getEndDate().isAfter(rental.getStartDate())) {
            System.out.println("[RentalService] End date must be after start date.");
            return false;
        }
        Inventory inv = inventoryDao.getById(rental.getInventoryId());
        if (inv == null) {
            System.out.println("[RentalService] Inventory not found for id=" + rental.getInventoryId());
            return false;
        }
        if (!inv.isAvailableForRent()) {
            System.out.println("[RentalService] Inventory is not available for rent.");
            return false;
        }
        int totalDays = (int) Rental.calculateDaysBetween(rental.getStartDate(), rental.getEndDate());
        if (totalDays <= 0) totalDays = 1;
        rental.setTotalDays(totalDays);
        double totalCost = rental.getDailyRate() * totalDays + (rental.isRequiresDelivery() ? rental.getDeliveryFee() : 0);
        rental.setTotalCost(totalCost);
        rental.setSecurityDeposit(totalCost * 0.5);
        rental.setRentalStatus(RentalStatus.PENDING);
        rental.setPaymentStatus(PaymentStatus.PENDING);
        rentalDao.create(rental);
        return rental.getRentalId() > 0;
    }

    /**
     * Approves rental only if status is PENDING.
     *
     * @param rentalId rental id
     * @return true if approved
     */
    public boolean approveRental(int rentalId) {
        Rental r = rentalDao.getById(rentalId);
        if (r == null) {
            System.out.println("[RentalService] Rental not found: " + rentalId);
            return false;
        }
        if (r.getRentalStatus() != RentalStatus.PENDING) {
            System.out.println("[RentalService] Only PENDING rentals can be approved.");
            return false;
        }
        rentalDao.approveRental(rentalId);
        return true;
    }

    /**
     * Cancels rental: status CANCELLED and frees inventory (AVAILABLE).
     *
     * @param rentalId rental id
     * @param reason   optional reason (for audit)
     * @return true if cancelled
     */
    public boolean cancelRental(int rentalId, String reason) {
        Rental r = rentalDao.getById(rentalId);
        if (r == null) {
            System.out.println("[RentalService] Rental not found: " + rentalId);
            return false;
        }
        r.setRentalStatus(RentalStatus.CANCELLED);
        rentalDao.update(r);
        // Free inventory if it was reserved/rented
        Inventory inv = inventoryDao.getById(r.getInventoryId());
        if (inv != null && inv.getRentalStatus() == tn.esprit.inventory.entities.InventoryRentalStatus.RENTED_OUT) {
            inv.setRentalStatus(tn.esprit.inventory.entities.InventoryRentalStatus.AVAILABLE);
            inventoryDao.update(inv);
        }
        System.out.println("[RentalService] Rental cancelled." + (reason != null ? " Reason: " + reason : ""));
        return true;
    }

    /**
     * Completes rental with return condition and owner rating. Calculates late fee if overdue.
     *
     * @param rentalId       rental id
     * @param returnCondition condition of item on return
     * @param rating         owner rating 1-5
     * @return true if completed
     */
    public boolean completeRental(int rentalId, String returnCondition, int rating) {
        Rental r = rentalDao.getById(rentalId);
        if (r == null) {
            System.out.println("[RentalService] Rental not found: " + rentalId);
            return false;
        }
        int safeRating = Math.max(1, Math.min(5, rating));
        rentalDao.completeRental(rentalId, returnCondition != null ? returnCondition : "OK", safeRating);
        return true;
    }

    /**
     * Updates editable rental fields (renter info, dates, totals) after validation.
     *
     * @param rental rental to update
     * @return true if updated
     */
    public boolean updateRental(Rental rental) {
        if (rental == null || rental.getRentalId() <= 0) {
            System.out.println("[RentalService] Valid rental with ID required.");
            return false;
        }
        Rental existing = rentalDao.getById(rental.getRentalId());
        if (existing == null) {
            System.out.println("[RentalService] Rental not found for id=" + rental.getRentalId());
            return false;
        }
        if (existing.getRentalStatus() != RentalStatus.PENDING &&
                existing.getRentalStatus() != RentalStatus.APPROVED) {
            System.out.println("[RentalService] Only PENDING or APPROVED rentals can be updated.");
            return false;
        }
        if (rental.getRenterName() == null || rental.getRenterName().trim().isEmpty()) {
            System.out.println("[RentalService] Renter name is required.");
            return false;
        }
        if (rental.getStartDate() == null || rental.getEndDate() == null) {
            System.out.println("[RentalService] Start and end dates are required.");
            return false;
        }
        if (rental.getStartDate().isBefore(LocalDate.now())) {
            System.out.println("[RentalService] Start date cannot be in the past.");
            return false;
        }
        if (!rental.getEndDate().isAfter(rental.getStartDate())) {
            System.out.println("[RentalService] End date must be after start date.");
            return false;
        }

        int totalDays = (int) Rental.calculateDaysBetween(rental.getStartDate(), rental.getEndDate());
        if (totalDays <= 0) totalDays = 1;
        rental.setTotalDays(totalDays);
        double totalCost = rental.getDailyRate() * totalDays + (rental.isRequiresDelivery() ? rental.getDeliveryFee() : 0);
        rental.setTotalCost(totalCost);
        rental.setSecurityDeposit(totalCost * 0.5);

        rentalDao.update(rental);
        return true;
    }

    /**
     * Deletes rental by id.
     *
     * @param rentalId id to delete
     * @return true if deleted
     */
    public boolean deleteRental(int rentalId) {
        Rental r = rentalDao.getById(rentalId);
        if (r == null) {
            System.out.println("[RentalService] Rental not found: " + rentalId);
            return false;
        }
        rentalDao.delete(rentalId);
        return true;
    }

    /**
     * Activates rental: APPROVED -> ACTIVE.
     *
     * @param rentalId rental id
     * @return true if activated
     */
    public boolean activateRental(int rentalId) {
        Rental r = rentalDao.getById(rentalId);
        if (r == null) {
            System.out.println("[RentalService] Rental not found: " + rentalId);
            return false;
        }
        if (r.getRentalStatus() != RentalStatus.APPROVED) {
            System.out.println("[RentalService] Only APPROVED rentals can be activated.");
            return false;
        }
        r.setRentalStatus(RentalStatus.ACTIVE);
        rentalDao.update(r);
        return true;
    }

    public List<Rental> getAllRentals() {
        return rentalDao.getAll();
    }

    public List<Rental> getRenterHistory(String renterName) {
        if (renterName == null || renterName.trim().isEmpty()) return rentalDao.getAll();
        return rentalDao.getRentalsByRenter(renterName.trim());
    }

    public List<Rental> getActiveRentals() {
        return rentalDao.getActiveRentals();
    }

    public List<Rental> getOverdueRentals() {
        return rentalDao.getOverdueRentals();
    }

    public double getTotalRevenue() {
        return rentalDao.getTotalRevenue();
    }

    /**
     * Statistics inner class and method.
     */
    public static class RentalStatistics {
        public int totalRentals;
        public int activeRentals;
        public int completedRentals;
        public int overdueRentals;
        public double totalRevenue;

        @Override
        public String toString() {
            return String.format("Total rentals: %d | Active: %d | Completed: %d | Overdue: %d | Total revenue: %.2f",
                    totalRentals, activeRentals, completedRentals, overdueRentals, totalRevenue);
        }
    }

    public RentalStatistics getStatistics() {
        List<Rental> all = rentalDao.getAll();
        RentalStatistics s = new RentalStatistics();
        s.totalRentals = all.size();
        s.activeRentals = rentalDao.getActiveRentals().size();
        s.completedRentals = (int) all.stream().filter(r -> r.getRentalStatus() == RentalStatus.COMPLETED).count();
        s.overdueRentals = rentalDao.getOverdueRentals().size();
        s.totalRevenue = rentalDao.getTotalRevenue();
        return s;
    }
}
