package com.theironyard.charlotte;


import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

    private static void innerJoins(Connection conn, Integer userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("Select * from users Inner join orders on users.id = orders.user_id Inner join items on items.order_id = orders.id Where user_id = ?");
        stmt.setInt(1, userId);
        stmt.execute();
    }

    private static User getUserById(Connection conn, Integer id) throws SQLException {
        User user = null;

        if (id != null) {
            PreparedStatement stmt = conn.prepareStatement("select * from users where id = ?");

            stmt.setInt(1, id);

            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                user = new User(id, results.getString("name"),
                        results.getString("email"),
                        results.getString("address"),
                        results.getString("paymentMethod"));
                user.setOrders(getOrdersForUserId(conn, id));
            }
        }
        return user;
    }

    private static List<Order> getOrdersForUserId(Connection conn, Integer userId) throws SQLException {
        ArrayList orders = null;
        PreparedStatement stmt = conn.prepareStatement("select * from orders where user_id = ?");
        stmt.setInt(1, userId);

        ResultSet results = stmt.executeQuery();

        if (results.next()) {
            orders = new ArrayList<Order>(results.getInt("user_id"));
        }
        return orders;
    }

    private static Integer getUserIdByEmail(Connection conn, String email) throws SQLException {
        Integer userId = null;

        if (email != null) {
            PreparedStatement stmt = conn.prepareStatement("select * from users where email = ?");
            stmt.setString(1, email);

            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                userId = results.getInt("id");
            }
        }
        return userId;
    }


    public static void main(String[] args) throws SQLException{
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        User.createTable(conn);
        Order.createTable(conn);
        Item.createTable(conn);
   //     innerJoins(conn);

        Spark.staticFileLocation("/Templates");

        Spark.get("/", ((req, res) -> {
            HashMap model = new HashMap();

            Session session = req.session();
            User currentUser = getUserById(conn, session.attribute("user_id"));

            if (currentUser == null) {
                return new ModelAndView(model, "home.html");
            } else {
                model.put("user", currentUser);
            return new ModelAndView(model, "addToCart.html");
            }
        }), new MustacheTemplateEngine());

        Spark.post("/login", (req, res) -> {
            String email = req.queryParams("email");

            Integer userID = getUserIdByEmail(conn, email);
            if (userID != null) {
                Session session = req.session();
                session.attribute("user_id", userID);
            }
            innerJoins(conn, userID);
            res.redirect("/addToCart");
            return "";
        });

        Spark.get("/registration", ((req, res) -> {
            HashMap model = new HashMap();

            Session session = req.session();
            getUserById(conn, session.attribute("user_id"));

            return new ModelAndView(model, "registration.html");
        }), new MustacheTemplateEngine());

        Spark.post("/registration", (req, res) -> {
            User currentUser = new User(
                    req.queryParams("name"),
                    req.queryParams("email"),
                    req.queryParams("address"),
                    req.queryParams("paymentMethod"));
            User.createUser(conn, currentUser);
            Integer userId = getUserIdByEmail(conn, currentUser.getEmail());
            if (userId != null) {
                Session session = req.session();
                session.attribute("user_id", userId);
            }
            innerJoins(conn, userId);
            res.redirect("/addToCart");
            return "";
        });

        Spark.get("/addToCart", ((req, res) -> {
            HashMap model = new HashMap();
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            req.session().attribute("order_id");

            model.put("user", user);

            return new ModelAndView(model, "addToCart.html");
        }), new MustacheTemplateEngine());

        Spark.post("/addToCart", (req, res) -> {
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            if (user != null) {
                Order currentOrder = Order.getLatestCurrentOrder(conn, user.getId());

                if (currentOrder == null) {
                    int orderId = Order.createOrder(conn, user.getId());
                    Item item = new Item(
                            req.queryParams("name"),
                            Integer.valueOf(req.queryParams("quantity")),
                            Double.valueOf(req.queryParams("price")),
                            orderId);
                    Item.addItemToOrder(conn, item);
                }
                else {
                    int orderId = currentOrder.getId();
                    Item item = new Item(
                            req.queryParams("name"),
                            Integer.valueOf(req.queryParams("quantity")),
                            Double.valueOf(req.queryParams("price")),
                            orderId);
                    Item.addItemToOrder(conn, item);
                }
            }

            res.redirect("/addToCart");
            return "";
        });
    }
}
