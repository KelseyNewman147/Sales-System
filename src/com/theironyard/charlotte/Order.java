package com.theironyard.charlotte;

import java.sql.*;
import java.util.List;

/**
 * Created by kelseynewman on 1/12/17.
 */
public class Order {
    Integer id;
    Integer userId;
    Boolean complete;
    List<Item> items;

    public Order() {
    }

    public Order(Integer userId, boolean complete) {
        this.userId = userId;
        this.complete = complete;
    }

    public Order(Integer id, Integer userId, boolean complete) {
        this.id = id;
        this.userId = userId;
        this.complete = complete;
    }

    public Boolean isComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public static void createTable(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS orders (id IDENTITY, user_id INT, complete boolean)");
    }

    public static int createOrder(Connection conn, int userID) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("insert into orders values (null, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, userID);
        stmt.setBoolean(2, false);
        stmt.executeUpdate();

        ResultSet keys = stmt.getGeneratedKeys();

        keys.next();

        return keys.getInt(1);
    }

    public static Order getLatestCurrentOrder(Connection conn, Integer userId) throws SQLException {
        Order order = null;

        if (userId != null) {
            PreparedStatement stmt = conn.prepareStatement("select top 1 * from orders where user_id = ? and complete = false");
            stmt.setInt(1, userId);
            ResultSet results  = stmt.executeQuery();

            if (results.next()) {
                order = new Order(results.getInt("id"), results.getInt("user_id"), false);
            }
        }
        return order;
    }
}
