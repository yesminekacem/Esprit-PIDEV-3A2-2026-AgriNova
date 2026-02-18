package tn.esprit.marketplace.service;


import tn.esprit.marketplace.entity.Cart;
import tn.esprit.marketplace.entity.Order;
import tn.esprit.marketplace.entity.OrderItem;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private Connection connection;

    public OrderService() {
        connection = MyDatabase.getInstance().getConx();
    }

    // Create order from cart
    public int createOrder(Order order, List<Cart> cartItems) throws SQLException {
        connection.setAutoCommit(false);

        try {
            // 1. Insert order
            String orderQuery = "INSERT INTO orders (user_id, total_price, delivery_address, payment_method) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement orderStmt = connection.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setString(1, order.getUserId());
            orderStmt.setDouble(2, order.getTotalPrice());
            orderStmt.setString(3, order.getDeliveryAddress());
            orderStmt.setString(4, order.getPaymentMethod());
            orderStmt.executeUpdate();

            // Get generated order ID
            ResultSet rs = orderStmt.getGeneratedKeys();
            int orderId = 0;
            if (rs.next()) {
                orderId = rs.getInt(1);
            }

            // 2. Insert order items and update product quantities
            String itemQuery = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price_per_unit, subtotal) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            String updateStockQuery = "UPDATE product_listing SET quantity = quantity - ?, " +
                    "status = CASE WHEN (quantity - ?) <= 0 THEN 'sold-out' ELSE 'available' END " +
                    "WHERE listing_id = ?";



            PreparedStatement itemStmt = connection.prepareStatement(itemQuery);
            PreparedStatement stockStmt = connection.prepareStatement(updateStockQuery);

            for (Cart cartItem : cartItems) {
                // Insert order item
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, cartItem.getProductId());
                itemStmt.setString(3, cartItem.getProductName());
                itemStmt.setInt(4, cartItem.getQuantity());
                itemStmt.setDouble(5, cartItem.getPricePerUnit());
                itemStmt.setDouble(6, cartItem.getSubtotal());
                itemStmt.executeUpdate();

                // Update product stock
                stockStmt.setInt(1, cartItem.getQuantity());
                stockStmt.setInt(2, cartItem.getQuantity());
                stockStmt.setInt(3, cartItem.getProductId());
                stockStmt.executeUpdate();
            }

            // 3. Clear cart
            String clearCartQuery = "DELETE FROM cart WHERE user_id = ?";
            PreparedStatement clearStmt = connection.prepareStatement(clearCartQuery);
            clearStmt.setString(1, order.getUserId());
            clearStmt.executeUpdate();

            connection.commit();
            return orderId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // Get orders by user
    public List<Order> getOrdersByUser(String userId) throws SQLException {
        List<Order> orders = new ArrayList<>();

        String query = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setUserId(rs.getString("user_id"));
                order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
                order.setTotalPrice(rs.getDouble("total_price"));
                order.setStatus(rs.getString("status"));
                order.setDeliveryAddress(rs.getString("delivery_address"));
                order.setPaymentMethod(rs.getString("payment_method"));
                order.setCreatedAt(rs.getTimestamp("created_at"));

                orders.add(order);
            }
        }

        return orders;
    }

    // Get all orders (for admin)
    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();

        String query = "SELECT * FROM orders ORDER BY order_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setUserId(rs.getString("user_id"));
                order.setOrderDate(rs.getTimestamp("order_date").toLocalDateTime());
                order.setTotalPrice(rs.getDouble("total_price"));
                order.setStatus(rs.getString("status"));
                order.setDeliveryAddress(rs.getString("delivery_address"));
                order.setPaymentMethod(rs.getString("payment_method"));
                order.setCreatedAt(rs.getTimestamp("created_at"));

                orders.add(order);
            }
        }

        return orders;
    }

    // Get order items for an order
    public List<OrderItem> getOrderItems(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();

        String query = "SELECT * FROM order_items WHERE order_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getInt("id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setProductName(rs.getString("product_name"));
                item.setQuantity(rs.getInt("quantity"));
                item.setPricePerUnit(rs.getDouble("price_per_unit"));
                item.setSubtotal(rs.getDouble("subtotal"));

                items.add(item);
            }
        }

        return items;
    }
    // Update order status
    public void updateOrderStatus(int orderId, String status) throws SQLException {
        String query = "UPDATE orders SET status = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, orderId);
            pstmt.executeUpdate();
        }
    }

    // Delete order and restore stock
    public void cancelOrder(int orderId) throws SQLException {
        connection.setAutoCommit(false);

        try {
            // Get order items to restore stock
            List<OrderItem> items = getOrderItems(orderId);

            // Restore product stock
            String restoreQuery = "UPDATE product_listing SET quantity = quantity + ?, " +
                    "status = 'available' WHERE listing_id = ?";

            PreparedStatement restoreStmt = connection.prepareStatement(restoreQuery);

            for (OrderItem item : items) {
                restoreStmt.setInt(1, item.getQuantity());
                restoreStmt.setInt(2, item.getProductId());
                restoreStmt.executeUpdate();
            }

            // Delete order (cascade will delete order_items)
            String deleteQuery = "DELETE FROM orders WHERE id = ?";
            PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery);
            deleteStmt.setInt(1, orderId);
            deleteStmt.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    public void updateOrderDetails(int orderId, String deliveryAddress, String paymentMethod) throws SQLException {
        String sql = "UPDATE orders SET delivery_address = ?, payment_method = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, deliveryAddress);
        ps.setString(2, paymentMethod);
        ps.setInt(3, orderId);
        ps.executeUpdate();
    }
    public void deleteOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, orderId);
        ps.executeUpdate();
    }



}

