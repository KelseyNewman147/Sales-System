package com.theironyard.charlotte;

import java.sql.*;
import java.util.List;


/**
 * Created by kelseynewman on 1/12/17.
 */
public class User {
    Integer id;
    String name;
    String email;
    String address;
    String paymentMethod;
    List<Order> orders;


    public User() {
    }

    public User(String name, String email, String address, String paymentMethod) {
        this.name = name;
        this.email = email;
        this.address = address;
        this.paymentMethod = paymentMethod;
    }

    public User(Integer id, String name, String email, String address, String paymentMethod) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.paymentMethod = paymentMethod;
    }

    public List<Order> getOrders() {
        return orders;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static void createTable(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, email VARCHAR, address VARCHAR, paymentMethod VARCHAR)");
    }

    public static void createUser(Connection conn, User user) throws SQLException {
        //add a new user to list of users
        PreparedStatement stmt = conn.prepareStatement("insert into users values (null, ?, ?, ?, ?)");
        stmt.setString(1, user.getName());
        stmt.setString(2, user.getEmail());
        stmt.setString(3, user.getAddress());
        stmt.setString(4, user.getPaymentMethod());
        stmt.execute();
    }
 }
