package tn.esprit.pidev.services;

import tn.esprit.pidev.entities.Inventory;
import tn.esprit.pidev.entities.Rental;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDFService - Generates PDF documents for AgriRent.
 *
 * Generates:
 *  1. Rental Contract/Invoice PDF
 *  2. Inventory Report PDF
 *
 * Uses pure Java with basic HTML-to-text approach for PDF generation
 * without needing iText (uses plain Java output to .html then converts).
 *
 * To use iText, add to pom.xml:
 *   <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itextpdf</artifactId>
 *     <version>5.5.13.3</version>
 *   </dependency>
 *
 * MODULE: Rental - API #2 (Advanced Feature: PDF Export)
 */
public class PDFService {

    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/AgriRent/PDFs/";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PDFService() {
        new File(OUTPUT_DIR).mkdirs();
    }

    // ============================================================
    //  PUBLIC API
    // ============================================================

    /**
     * Generates an HTML rental contract (open in browser/print as PDF).
     * Returns the file path.
     *
     * @param rental the rental to generate contract for
     * @return path to generated HTML file
     */
    public String generateRentalContract(Rental rental) {
        String fileName = OUTPUT_DIR + "Contract_Rental_" + rental.getRentalId() + ".html";
        String content = buildRentalContractHTML(rental);
        writeFile(fileName, content);
        System.out.println("✅ Rental contract generated: " + fileName);
        return fileName;
    }

    /**
     * Generates an HTML inventory report for all items.
     * Returns the file path.
     *
     * @param inventoryList all inventory items
     * @return path to generated HTML file
     */
    public String generateInventoryReport(List<Inventory> inventoryList) {
        String fileName = OUTPUT_DIR + "Inventory_Report_" + System.currentTimeMillis() + ".html";
        String content = buildInventoryReportHTML(inventoryList);
        writeFile(fileName, content);
        System.out.println("✅ Inventory report generated: " + fileName);
        return fileName;
    }

    /**
     * Opens the generated file in the system's default browser.
     */
    public void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                java.awt.Desktop.getDesktop().browse(file.toURI());
            }
        } catch (Exception e) {
            System.err.println("❌ Could not open file: " + e.getMessage());
        }
    }

    public String getOutputDirectory() {
        return OUTPUT_DIR;
    }

    // ============================================================
    //  HTML BUILDERS
    // ============================================================

    private String buildRentalContractHTML(Rental rental) {
        String equipName = rental.getInventory() != null
                ? rental.getInventory().getItemName()
                : "Equipment #" + rental.getInventoryId();

        String startDate = rental.getStartDate() != null ? rental.getStartDate().format(DATE_FMT) : "N/A";
        String endDate = rental.getEndDate() != null ? rental.getEndDate().format(DATE_FMT) : "N/A";
        String generatedAt = LocalDateTime.now().format(DATETIME_FMT);

        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
               "<title>Rental Contract #" + rental.getRentalId() + "</title>" +
               "<style>" +
               "  body { font-family: 'Arial', sans-serif; max-width: 800px; margin: 40px auto; color: #333; }" +
               "  .header { background: linear-gradient(135deg, #2E7D32, #66BB6A); color: white; padding: 30px; border-radius: 12px; text-align: center; margin-bottom: 30px; }" +
               "  .header h1 { margin: 0; font-size: 28px; } .header p { margin: 6px 0 0; opacity: 0.85; }" +
               "  .section { margin-bottom: 24px; }" +
               "  .section h2 { color: #2E7D32; border-bottom: 2px solid #E8F5E9; padding-bottom: 8px; }" +
               "  table { width: 100%; border-collapse: collapse; }" +
               "  th { background: #2E7D32; color: white; padding: 10px; text-align: left; }" +
               "  td { padding: 10px; border-bottom: 1px solid #f0f0f0; }" +
               "  tr:nth-child(even) td { background: #F9FBF9; }" +
               "  .total-box { background: #E8F5E9; border: 2px solid #2E7D32; border-radius: 8px; padding: 20px; text-align: right; margin-top: 20px; }" +
               "  .total-box h2 { color: #2E7D32; margin: 0; font-size: 24px; }" +
               "  .signatures { display: flex; justify-content: space-between; margin-top: 60px; }" +
               "  .sig-box { width: 40%; text-align: center; border-top: 1px solid #333; padding-top: 8px; }" +
               "  .badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }" +
               "  .badge-green { background: #E8F5E9; color: #2E7D32; }" +
               "  .badge-orange { background: #FFF3E0; color: #E65100; }" +
               "  .footer { text-align: center; color: #9E9E9E; font-size: 12px; margin-top: 40px; border-top: 1px solid #eee; padding-top: 16px; }" +
               "  @media print { body { margin: 20px; } }" +
               "</style></head><body>" +

               "<div class='header'>" +
               "  <h1>🌿 AgriRent</h1>" +
               "  <p>Agricultural Equipment Rental System</p>" +
               "  <p style='font-size:14px;margin-top:10px;'>RENTAL CONTRACT — Document #" + rental.getRentalId() + "</p>" +
               "</div>" +

               "<div style='text-align:right;color:#9E9E9E;font-size:13px;margin-bottom:20px;'>Generated: " + generatedAt + "</div>" +

               "<div class='section'><h2>📋 Contract Information</h2>" +
               "<table><tr><td><strong>Contract ID</strong></td><td>#" + rental.getRentalId() + "</td>" +
               "<td><strong>Status</strong></td><td><span class='badge badge-green'>" + (rental.getRentalStatus() != null ? rental.getRentalStatus().name() : "N/A") + "</span></td></tr>" +
               "<tr><td><strong>Equipment</strong></td><td>" + equipName + "</td>" +
               "<td><strong>Payment Status</strong></td><td>" + (rental.getPaymentStatus() != null ? rental.getPaymentStatus().name() : "N/A") + "</td></tr></table></div>" +

               "<div class='section'><h2>👤 Parties Involved</h2>" +
               "<table><tr><th>Role</th><th>Name</th><th>Contact</th><th>Address</th></tr>" +
               "<tr><td>Owner</td><td>" + rental.getOwnerName() + "</td><td>—</td><td>—</td></tr>" +
               "<tr><td>Renter</td><td>" + rental.getRenterName() + "</td><td>" + rental.getRenterContact() + "</td><td>" + nvl(rental.getRenterAddress()) + "</td></tr>" +
               "</table></div>" +

               "<div class='section'><h2>📅 Rental Period</h2>" +
               "<table><tr><th>Start Date</th><th>End Date</th><th>Duration</th><th>Daily Rate</th></tr>" +
               "<tr><td>" + startDate + "</td><td>" + endDate + "</td>" +
               "<td><strong>" + rental.getTotalDays() + " days</strong></td>" +
               "<td>" + String.format("%.2f TND", rental.getDailyRate()) + "</td></tr></table></div>" +

               "<div class='section'><h2>💰 Financial Summary</h2>" +
               "<table>" +
               "<tr><td>Base Rental Cost</td><td style='text-align:right;'>" + String.format("%.2f TND", rental.getDailyRate() * rental.getTotalDays()) + "</td></tr>" +
               "<tr><td>Delivery Fee</td><td style='text-align:right;'>" + (rental.isRequiresDelivery() ? String.format("%.2f TND", rental.getDeliveryFee()) : "0.00 TND (Self-pickup)") + "</td></tr>" +
               "<tr><td>Security Deposit</td><td style='text-align:right;'>" + String.format("%.2f TND", rental.getSecurityDeposit()) + "</td></tr>" +
               "<tr><td>Late Fee</td><td style='text-align:right;'>" + String.format("%.2f TND", rental.getLateFee()) + "</td></tr>" +
               "</table>" +
               "<div class='total-box'><p style='margin:0 0 6px;color:#555;'>Total Amount Due</p>" +
               "<h2>" + String.format("%.2f TND", rental.getTotalCost()) + "</h2></div></div>" +

               (rental.isRequiresDelivery() ? "<div class='section'><h2>🚚 Delivery Information</h2>" +
               "<p><strong>Delivery Address:</strong> " + nvl(rental.getDeliveryAddress()) + "</p></div>" : "") +

               "<div class='section'><h2>📝 Terms & Conditions</h2>" +
               "<ol style='color:#555;line-height:1.8;'>" +
               "<li>The renter is responsible for any damage to the equipment during the rental period.</li>" +
               "<li>Equipment must be returned in the same condition as received.</li>" +
               "<li>Late returns will incur a fee of 150% of the daily rate per additional day.</li>" +
               "<li>The security deposit will be refunded within 3 business days after inspection.</li>" +
               "<li>Cancellation must be made at least 48 hours before the start date.</li>" +
               "</ol></div>" +

               "<div class='signatures'>" +
               "  <div class='sig-box'><p>Owner Signature</p><p style='color:#9E9E9E;font-size:13px;'>" + rental.getOwnerName() + "</p></div>" +
               "  <div class='sig-box'><p>Renter Signature</p><p style='color:#9E9E9E;font-size:13px;'>" + rental.getRenterName() + "</p></div>" +
               "</div>" +

               "<div class='footer'>" +
               "  <p>🌿 AgriRent — Connecting Farmers Across Tunisia</p>" +
               "  <p>This document was automatically generated by the AgriRent System. For support, contact your administrator.</p>" +
               "</div>" +
               "</body></html>";
    }

    private String buildInventoryReportHTML(List<Inventory> items) {
        double totalValue = items.stream().mapToDouble(Inventory::calculateTotalValue).sum();
        long rentableCount = items.stream().filter(Inventory::isRentable).count();
        long availableCount = items.stream().filter(Inventory::isAvailableForRent).count();
        long maintenanceDue = items.stream().filter(Inventory::isMaintenanceDueSoon).count();
        String generatedAt = LocalDateTime.now().format(DATETIME_FMT);

        StringBuilder rows = new StringBuilder();
        for (Inventory item : items) {
            rows.append("<tr>")
                .append("<td>").append(item.getInventoryId()).append("</td>")
                .append("<td><strong>").append(item.getItemName()).append("</strong></td>")
                .append("<td>").append(item.getItemType() != null ? item.getItemType().name() : "N/A").append("</td>")
                .append("<td>").append(item.getQuantity()).append("</td>")
                .append("<td>").append(String.format("%.2f TND", item.getUnitPrice())).append("</td>")
                .append("<td>").append(String.format("%.2f TND", item.calculateTotalValue())).append("</td>")
                .append("<td>").append(item.getConditionStatus() != null ? item.getConditionStatus().name() : "N/A").append("</td>")
                .append("<td>").append(item.getRentalStatus() != null ? item.getRentalStatus().name() : "N/A").append("</td>")
                .append("<td>").append(String.format("%.2f", item.getRentalPricePerDay())).append(" TND/day</td>")
                .append("<td>").append(item.isMaintenanceDueSoon() ? "⚠️ Soon" : "✅ OK").append("</td>")
                .append("</tr>");
        }

        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>" +
               "<title>AgriRent Inventory Report</title>" +
               "<style>" +
               "  body { font-family: Arial, sans-serif; max-width: 1100px; margin: 30px auto; color: #333; }" +
               "  .header { background: linear-gradient(135deg, #2E7D32, #66BB6A); color: white; padding: 30px; border-radius: 12px; margin-bottom: 30px; }" +
               "  .stat-grid { display: flex; gap: 16px; margin-bottom: 30px; }" +
               "  .stat-card { flex: 1; background: white; border: 1px solid #e0e0e0; border-radius: 12px; padding: 20px; text-align: center; }" +
               "  .stat-card h3 { font-size: 28px; margin: 0; color: #2E7D32; }" +
               "  .stat-card p { margin: 4px 0 0; color: #757575; font-size: 13px; }" +
               "  table { width: 100%; border-collapse: collapse; font-size: 13px; }" +
               "  th { background: #2E7D32; color: white; padding: 10px; text-align: left; white-space: nowrap; }" +
               "  td { padding: 9px 10px; border-bottom: 1px solid #f0f0f0; }" +
               "  tr:hover td { background: #F1F8E9; }" +
               "  .footer { text-align: center; color: #9E9E9E; font-size: 12px; margin-top: 30px; }" +
               "  @media print { .stat-grid { display: block; } .stat-card { display: inline-block; width: 22%; } }" +
               "</style></head><body>" +

               "<div class='header'>" +
               "  <h1 style='margin:0;'>🌿 AgriRent — Inventory Report</h1>" +
               "  <p style='margin:8px 0 0;opacity:.85;'>Generated: " + generatedAt + " | Total Items: " + items.size() + "</p>" +
               "</div>" +

               "<div class='stat-grid'>" +
               "<div class='stat-card'><h3>" + items.size() + "</h3><p>Total Items</p></div>" +
               "<div class='stat-card'><h3>" + availableCount + "</h3><p>Available for Rent</p></div>" +
               "<div class='stat-card'><h3>" + rentableCount + "</h3><p>Rentable Items</p></div>" +
               "<div class='stat-card'><h3>" + maintenanceDue + "</h3><p>Maintenance Due</p></div>" +
               "<div class='stat-card'><h3>" + String.format("%.0f", totalValue) + " TND</h3><p>Total Portfolio Value</p></div>" +
               "</div>" +

               "<h2 style='color:#2E7D32;'>Equipment List</h2>" +
               "<table><tr>" +
               "<th>ID</th><th>Name</th><th>Type</th><th>Qty</th><th>Unit Price</th><th>Total Value</th>" +
               "<th>Condition</th><th>Rental Status</th><th>Daily Rate</th><th>Maintenance</th></tr>" +
               rows +
               "</table>" +

               "<div class='footer'><p>🌿 AgriRent Inventory Report — Confidential | AgriRent System</p></div>" +
               "</body></html>";
    }

    // ============================================================
    //  UTILITIES
    // ============================================================

    private void writeFile(String path, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("❌ Failed to write file: " + e.getMessage());
        }
    }

    private String nvl(String value) {
        return value != null && !value.isBlank() ? value : "—";
    }
}
