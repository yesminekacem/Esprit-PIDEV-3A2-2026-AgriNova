package tn.esprit.marketplace.entity;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class Order {
    private int orderId;
    private String userId;
    private LocalDateTime orderDate;
    private double totalPrice;
    private String status;
    private String deliveryAddress;
    private String paymentMethod;
    private Timestamp createdAt;

    // Constructors
    public Order() {}

    // Constructor for creating new order
    public Order(String userId, double totalPrice, String deliveryAddress, String paymentMethod) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.status = "pending";
    }

    // Constructor with all fields
    public Order(int id, String userId, LocalDateTime orderDate, double totalPrice,
                 String status, String deliveryAddress, String paymentMethod, Timestamp createdAt) {
        this.orderId = id;
        this.userId = userId;
        this.orderDate = orderDate;
        this.totalPrice = totalPrice;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return orderId;
    }

    public void setId(int id) {
        this.orderId = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

