package tn.esprit.marketplace.entity;

public class ProductListing {
    private int listing_id;
    private String user_id;  // ← NEW FIELD
    private String product_name;
    private double price_per_unit;
    private int quantity;
    private String status;
    private String description;
    private String picture;
    private String category;

    // Constructor with ID
    public ProductListing(int listing_id, String user_id, String product_name,
                          double price_per_unit, int quantity, String status,
                          String description, String picture, String category) {
        this.listing_id = listing_id;
        this.user_id = user_id;
        this.product_name = product_name;
        this.price_per_unit = price_per_unit;
        this.quantity = quantity;
        this.status = status;
        this.description = description;
        this.picture = picture;
        this.category = category;
    }


    // Constructor without ID (for new products)
    public ProductListing(String user_id, String product_name, double price_per_unit,
                          int quantity, String status, String description,
                          String picture, String category) {
        this.user_id = user_id;
        this.product_name = product_name;
        this.price_per_unit = price_per_unit;
        this.quantity = quantity;
        this.status = status;
        this.description = description;
        this.picture = picture;
        this.category = category;
    }

    // Getters and Setters
    public int getListing_id() { return listing_id; }
    public String getUser_id() { return user_id; }  // ← NEW GETTER
    public String getProduct_name() { return product_name; }
    public double getPrice_per_unit() { return price_per_unit; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public String getPicture() { return picture; }
    public String getCategory() { return category; }

    public void setListing_id(int listing_id) { this.listing_id = listing_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }  // ← NEW SETTER
    public void setProduct_name(String product_name) { this.product_name = product_name; }
    public void setPrice_per_unit(double price_per_unit) { this.price_per_unit = price_per_unit; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setStatus(String status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setPicture(String picture) { this.picture = picture; }
    public void setCategory(String category) { this.category = category; }
}

