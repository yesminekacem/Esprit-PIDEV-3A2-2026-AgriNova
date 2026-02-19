package tn.esprit.marketplace.service;

import tn.esprit.marketplace.entity.ProductListing;
import tn.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductListingService implements ICRUD<ProductListing> {
    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public ProductListingService() {
        conx = MyDatabase.getInstance().getConx();
    }



    @Override
    public void addMeth2(ProductListing product) throws SQLException {
        String req = "INSERT INTO `product_listing`(`user_id`, `product_name`, `price_per_unit`, `quantity`, `status`, `description`, `picture`, `category`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, product.getUser_id());
        pstm.setString(2, product.getProduct_name());
        pstm.setDouble(3, product.getPrice_per_unit());
        pstm.setInt(4, product.getQuantity());
        pstm.setString(5, product.getStatus());
        pstm.setString(6, product.getDescription());
        pstm.setString(7, product.getPicture());
        pstm.setString(8, product.getCategory());
        pstm.executeUpdate();
        pstm.close();
    }

    @Override
    public void modifier(ProductListing product) throws SQLException {
        String req = "UPDATE `product_listing` SET `product_name`=?, `price_per_unit`=?, `quantity`=?, " +
                "`status`=?, `description`=?, `picture`=?, `category`=? WHERE `listing_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, product.getProduct_name());
        pstm.setDouble(2, product.getPrice_per_unit());
        pstm.setInt(3, product.getQuantity());
        pstm.setString(4, product.getStatus());
        pstm.setString(5, product.getDescription());
        pstm.setString(6, product.getPicture());
        pstm.setString(7, product.getCategory());
        pstm.setInt(8, product.getListing_id());
        pstm.executeUpdate();
        pstm.close();
    }

    @Override
    public void delete(ProductListing product) throws SQLException {
        String req = "DELETE FROM `product_listing` WHERE `listing_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, product.getListing_id());
        pstm.executeUpdate();
        pstm.close();
    }

    /**
     * 🔥 THIS IS THE METHOD YOUR CONTROLLER NEEDS
     * Returns ALL products (ALL users, available + out of stock)
     */
    public List<ProductListing> getAllProducts() throws SQLException {
        String req = "SELECT * FROM `product_listing` ORDER BY `listing_id` DESC";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);
        List<ProductListing> products = new ArrayList<>();

        while (res.next()) {
            products.add(new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            ));
        }

        stm.close();
        return products;
    }

    @Override
    public List<ProductListing> afficherList() throws SQLException {
        String req = "SELECT * FROM `product_listing`";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);
        List<ProductListing> products = new ArrayList<>();

        while (res.next()) {
            products.add(new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            ));
        }
        stm.close();
        return products;
    }
    public List<ProductListing> getAllOtherUsersProducts(String currentUserId) throws SQLException {
        String req = "SELECT * FROM `product_listing` WHERE `user_id` != ?";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, currentUserId);
        ResultSet res = pstm.executeQuery();
        List<ProductListing> products = new ArrayList<>();

        while (res.next()) {
            products.add(new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            ));
        }
        pstm.close();
        return products;
    }
    public ProductListing getProductById(int id) throws SQLException {
        String req = "SELECT * FROM `product_listing` WHERE `listing_id` = ?";
        PreparedStatement pstm = conx.prepareStatement(req);
        pstm.setInt(1, id);
        ResultSet res = pstm.executeQuery();

        if (res.next()) {
            ProductListing product = new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            );
            pstm.close();
            return product;
        } else {
            pstm.close();
            throw new SQLException("Product not found with id: " + id);
        }
    }



    /** Get user's products only (Manage Products tab) */
    public List<ProductListing> getMyProducts(String userId) throws SQLException {
        String req = "SELECT * FROM `product_listing` WHERE `user_id` = ?";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, userId);
        ResultSet res = pstm.executeQuery();
        List<ProductListing> myProducts = new ArrayList<>();

        while (res.next()) {
            myProducts.add(new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            ));
        }
        pstm.close();
        return myProducts;
    }

    /**
     * Marketplace (OLD METHOD - now only available products)
     * You can still keep it for filters if needed
     */
    public List<ProductListing> getAvailableProducts(String userId) throws SQLException {
        String req = "SELECT * FROM `product_listing` WHERE `user_id` != ? AND `status` = 'available'";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, userId);
        ResultSet res = pstm.executeQuery();
        List<ProductListing> products = new ArrayList<>();

        while (res.next()) {
            products.add(new ProductListing(
                    res.getInt("listing_id"),
                    res.getString("user_id"),
                    res.getString("product_name"),
                    res.getDouble("price_per_unit"),
                    res.getInt("quantity"),
                    res.getString("status"),
                    res.getString("description"),
                    res.getString("picture"),
                    res.getString("category")
            ));
        }
        pstm.close();
        return products;
    }
}
