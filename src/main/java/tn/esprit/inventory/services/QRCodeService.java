package tn.esprit.inventory.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import tn.esprit.inventory.entities.Inventory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeService - Generates QR codes for inventory items using ZXing library.
 *
 * Each QR code encodes equipment details: ID, name, type, owner, daily rate.
 * Can be saved as PNG or displayed directly in JavaFX.
 *
 * Dependency in pom.xml:
 *   <dependency>
 *     <groupId>com.google.zxing</groupId>
 *     <artifactId>core</artifactId>
 *     <version>3.5.2</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.google.zxing</groupId>
 *     <artifactId>javase</artifactId>
 *     <version>3.5.2</version>
 *   </dependency>
 *
 * MODULE: Inventory - API #2
 */
public class QRCodeService {

    private static final int QR_SIZE = 300;
    private static final String QR_OUTPUT_DIR = System.getProperty("user.home") + "/AgriRent/QRCodes/";

    public QRCodeService() {
        // Ensure output directory exists
        new File(QR_OUTPUT_DIR).mkdirs();
    }

    /**
     * Generates QR code data string for an inventory item.
     * Format is designed to be human-readable when scanned.
     */
    public String buildQRContent(Inventory item) {
        return "=== AGRIRENT EQUIPMENT ===\n" +
               "ID: " + item.getInventoryId() + "\n" +
               "Name: " + item.getItemName() + "\n" +
               "Type: " + (item.getItemType() != null ? item.getItemType().name() : "N/A") + "\n" +
               "Owner: " + item.getOwnerName() + "\n" +
               "Contact: " + item.getOwnerContact() + "\n" +
               "Daily Rate: " + item.getRentalPricePerDay() + " TND/day\n" +
               "Status: " + (item.getRentalStatus() != null ? item.getRentalStatus().name() : "N/A") + "\n" +
               "Condition: " + (item.getConditionStatus() != null ? item.getConditionStatus().name() : "N/A") + "\n" +
               "==========================";
    }

    /**
     * Generates a QR code as a JavaFX Image for direct display in an ImageView.
     *
     * @param item the inventory item
     * @return JavaFX Image of the QR code, or null on failure
     */
    public Image generateQRImage(Inventory item) {
        try {
            BufferedImage bufferedImage = generateBufferedImage(buildQRContent(item));
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            System.err.println("❌ QR generation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves QR code as PNG file to ~/AgriRent/QRCodes/
     *
     * @param item the inventory item
     * @return path to the saved file, or null on failure
     */
    public String saveQRCode(Inventory item) {
        try {
            String fileName = QR_OUTPUT_DIR + "QR_" + item.getInventoryId() + "_" + item.getItemName().replaceAll("\\s+", "_") + ".png";
            BufferedImage image = generateBufferedImage(buildQRContent(item));
            ImageIO.write(image, "PNG", new File(fileName));
            System.out.println("✅ QR Code saved: " + fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("❌ Failed to save QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Core method: generates a BufferedImage from a QR code string.
     */
    private BufferedImage generateBufferedImage(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < QR_SIZE; x++) {
            for (int y = 0; y < QR_SIZE; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    /**
     * Gets the output directory path where QR codes are saved.
     */
    public String getOutputDirectory() {
        return QR_OUTPUT_DIR;
    }
}
