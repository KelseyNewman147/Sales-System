package com.theironyard.charlotte;

import com.theironyard.charlotte.utilities.PasswordStorage;
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
        PreparedStatement stmt = conn.prepareStatement("Select items.name as item_name, items.quantity, items.price, users.name, users.email, orders.id as order_id, orders.complete from users " +
                " Inner join orders on users.id = orders.user_id" +
                " Inner join items on items.order_id = orders.id" +
                " Where user_id = ?");
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
                        results.getString("password"),
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

    private static User getUserByEmail(Connection conn, String email) throws SQLException, PasswordStorage.InvalidHashException, PasswordStorage.CannotPerformOperationException {
        User user = null;

        if (email != null) {
            PreparedStatement stmt = conn.prepareStatement("select * from users where email = ?");
            stmt.setString(1, email);
            ResultSet results = stmt.executeQuery();

            if (results.next()) {
                user = new User(results.getInt("id"),
                        results.getString("name"),
                        email,
                        results.getString("password"),
                        results.getString("address"),
                        results.getString("paymentMethod"));
            }
        }
        return user;
    }

    private static List<Item> getItemsforCurrentOrder(Connection connection, Integer orderId) throws SQLException {
        List<Item> items = new ArrayList<>();

        if (orderId != null) {
            PreparedStatement stmt = connection.prepareStatement("select * from items where order_id = ?");
            stmt.setInt(1, orderId);

            ResultSet results = stmt.executeQuery();

            while (results.next()) {
                String name = results.getString("name");
                Integer quantity = results.getInt("quantity");
                Double price = results.getDouble("price");
                Integer currentOrder = orderId;
                items.add(new Item(name, quantity, price, currentOrder));
            }
        }
        return items;
    }

    private static Double getSubtotalForItems(List<Item> items) throws SQLException {
      Double itemCost;
      Double subTotal = 0.0;
        for (Item item : items) {
            itemCost = item.getPrice() * item.getQuantity();
            subTotal += itemCost;
        }
        return subTotal;
    }

    private static void setCurrentOrderToComplete(Connection conn, Integer orderId) throws SQLException {
            PreparedStatement stmt = conn.prepareStatement("update orders set complete = true where id = ?");
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
    }


    public static void main(String[] args) throws SQLException{
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        User.createTable(conn);
        Order.createTable(conn);
        Item.createTable(conn);

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
            String password = req.queryParams("password");
            User user = getUserByEmail(conn, email);

            PasswordStorage.verifyPassword(password, user.getPassword());
            Integer userID = user.getId();

            if (userID != null) {
                Session session = req.session();
                session.attribute("user_id", userID);
            }

//            innerJoins(conn, userID);
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
                    req.queryParams("password"),
                    req.queryParams("address"),
                    req.queryParams("paymentMethod"));
            User.createUser(conn, currentUser);

            Integer userId = getUserByEmail(conn, currentUser.getEmail()).getId();
            if (userId != null) {
                Session session = req.session();
                session.attribute("user_id", userId);
            }
//            innerJoins(conn, userId);
            res.redirect("/addToCart");
            return "";
        });

        Spark.get("/addToCart", ((req, res) -> {
            HashMap model = new HashMap();
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            Order currentOrder = Order.getLatestCurrentOrder(conn, user.getId());
            if (currentOrder == null) {
                Order.createOrder(conn, user.getId());
            } else {
                List items = getItemsforCurrentOrder(conn, currentOrder.getId());
                model.put("item", items);
            }
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

            res.redirect("/checkout");
            return "";
        });

        Spark.get("/checkout", ((req, res) -> {
            HashMap model = new HashMap();
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            Order currentOrder = Order.getLatestCurrentOrder(conn, user.getId());
            if (currentOrder == null) {
                Order.createOrder(conn, user.getId());
            } else {
                List items = getItemsforCurrentOrder(conn, currentOrder.getId());
                Double subtotal = getSubtotalForItems(items);
                Double total = (subtotal*.07) + subtotal;
                model.put("item", items);
                model.put("subtotal", subtotal);
                model.put("total", total);
            }
            model.put("user", user);

            return new ModelAndView(model, "checkout.html");
        }), new MustacheTemplateEngine());

        Spark.post("/checkout", (req, res) -> {
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            Order currentOrder = Order.getLatestCurrentOrder(conn,user.getId());
            setCurrentOrderToComplete(conn, currentOrder.getId());
            Order.createOrder(conn, user.getId());

            res.redirect("/submitOrder");
            return "";
        });

        Spark.get("/submitOrder", ((req, res) -> {
            HashMap model = new HashMap();
            Session session = req.session();
            User user = getUserById(conn, session.attribute("user_id"));
            int randomNumber = (int)(Math.random()*2000);
            model.put("user", user);
            model.put("randomNumber", randomNumber);

            return new ModelAndView(model, "submitOrder.html");
        }), new MustacheTemplateEngine());

        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );
    }
}
