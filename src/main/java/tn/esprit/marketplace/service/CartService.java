package tn.esprit.marketplace.service;

import tn.esprit.marketplace.entity.Cart;
import tn.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartService {
    private final Connection connection;

    public CartService() {
        connection = MyDatabase.getInstance().getConx();
    }

    // Add product to cart
    public void addToCart(Cart cartItem) throws SQLException {
        int realStock = getAvailableStockForCart(cartItem.getProductId(), cartItem.getUserId());
        int qtyToAdd = Math.min(cartItem.getQuantity(), realStock);

        if (qtyToAdd <= 0) {
            System.out.println("⚠️ Cannot add product " + cartItem.getProductId() + " - no stock available");
            return;
        }

        String query = "INSERT INTO cart (user_id, product_id, quantity) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE quantity = LEAST(quantity + ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, cartItem.getUserId());
            pstmt.setInt(2, cartItem.getProductId());
            pstmt.setInt(3, qtyToAdd);
            pstmt.setInt(4, qtyToAdd);
            pstmt.setInt(5, realStock); // cap to available stock
            int rows = pstmt.executeUpdate();
            System.out.println("🛒 Added to cart: " + qtyToAdd + " of product " + cartItem.getProductId() + " (rows: " + rows + ")");
        }
    }

    // Get cart items for a user
    public List<Cart> getCartByUser(String userId) throws SQLException {
        List<Cart> cartItems = new ArrayList<>();

        String query = "SELECT c.id, c.user_id, c.product_id, c.quantity as quantityInCart, c.added_at, " +
                "p.product_name, p.price_per_unit, p.picture, p.quantity as availableStock " +
                "FROM cart c " +
                "JOIN product_listing p ON c.product_id = p.listing_id " +
                "WHERE c.user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Cart cart = new Cart();
                cart.setId(rs.getInt("id"));
                cart.setUserId(rs.getString("user_id"));
                cart.setProductId(rs.getInt("product_id"));
                cart.setQuantity(rs.getInt("quantityInCart"));          // quantity already in cart
                cart.setAddedAt(rs.getTimestamp("added_at"));
                cart.setProductName(rs.getString("product_name"));
                cart.setPricePerUnit(rs.getDouble("price_per_unit"));
                cart.setPicture(rs.getString("picture"));
                cart.setAvailableStock(rs.getInt("availableStock"));   // real stock left

                cartItems.add(cart);
            }
        }

        return cartItems;
    }


    // Update cart quantity
    public void updateCartQuantity(int cartId, int productId, String userId, int newQuantity) throws SQLException {
        int realStock = getAvailableStockForCart(productId, userId);
        int qtyToUpdate = Math.min(newQuantity, realStock);

        String query = "UPDATE cart SET quantity = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, qtyToUpdate);
            pstmt.setInt(2, cartId);
            int rows = pstmt.executeUpdate();
            System.out.println("🔄 Updated cart ID " + cartId + " to " + qtyToUpdate + "kg (rows: " + rows + ")");
        }
    }

    // Remove item from cart
    public void removeFromCart(int cartId) throws SQLException {
        String query = "DELETE FROM cart WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, cartId);
            int rows = pstmt.executeUpdate();
            System.out.println("🗑️ Removed cart ID " + cartId + " (rows: " + rows + ")");
        }
    }

    // Clear user's cart
    public void clearCart(String userId) throws SQLException {
        String query = "DELETE FROM cart WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userId);
            int rows = pstmt.executeUpdate();
            System.out.println("🧹 Cleared cart for " + userId + " (rows: " + rows + ")");
        }
    }

    // Get cart total
    public double getCartTotal(String userId) throws SQLException {
        String query = "SELECT SUM(c.quantity * p.price_per_unit) as total " +
                "FROM cart c " +
                "JOIN product_listing p ON c.product_id = p.listing_id " +
                "WHERE c.user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Math.abs(rs.getDouble("total"));
            }
        }
        return 0.0;
    }

    // Get available stock considering other carts
    public int getAvailableStockForCart(int productId, String currentUserId) throws SQLException {
        // Total product stock
        String queryStock = "SELECT quantity FROM product_listing WHERE listing_id = ?";
        int totalStock = 0;
        try (PreparedStatement pst = connection.prepareStatement(queryStock)) {
            pst.setInt(1, productId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) totalStock = rs.getInt("quantity");
        }

        // Quantity already in carts of other users
        String queryCart = "SELECT SUM(quantity) as qty_in_cart FROM cart WHERE product_id = ? AND user_id != ?";
        int qtyInOtherCarts = 0;
        try (PreparedStatement pst = connection.prepareStatement(queryCart)) {
            pst.setInt(1, productId);
            pst.setString(2, currentUserId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) qtyInOtherCarts = rs.getInt("qty_in_cart");
        }

        return Math.max(totalStock - qtyInOtherCarts, 0);
    }

    // Fix negatives (run once)
    public void fixNegativeQuantities() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            int cartFixed = stmt.executeUpdate("UPDATE cart SET quantity = ABS(quantity) WHERE quantity < 0");
            int prodFixed = stmt.executeUpdate("UPDATE product_listing SET quantity = ABS(quantity) WHERE quantity < 0");
            System.out.println("🛠️ Fixed negatives: Cart=" + cartFixed + ", Products=" + prodFixed);
        }
    }
}
