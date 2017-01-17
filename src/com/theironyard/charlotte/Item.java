package com.theironyard.charlotte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by kelseynewman on 1/12/17.
 */
public class Item {
    Integer id;
    String name;
    int quantity;
    double price;
    Integer orderId;

    public Item() {
    }

    public Item(String name, int quantity, double price, Integer orderId) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.orderId = orderId;
    }

    public Item(Integer id, String name, int quantity, double price, Integer orderId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.orderId = orderId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public static void createTable(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS items (id IDENTITY, name VARCHAR, quantity INT, price DOUBLE, order_id INT)");
    }

    public static void addItemToOrder(Connection conn, Item item) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("insert into items values (null, ?, ?, ?, ?)");
        stmt.setString(1, item.getName());
        stmt.setInt(2, item.getQuantity());
        stmt.setDouble(3, item.getPrice());
        stmt.setInt(4, item.getOrderId());
        stmt.execute();

    }
}
