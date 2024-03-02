package com.networksecurity.controller;

import com.networksecurity.Constants;
import com.networksecurity.bean.User;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MyController {

    public String loginName = ""; // must be complited with username

    public static long getDateDiff(Timestamp oldTs, Timestamp newTs, TimeUnit timeUnit) {
        long diffInMS = newTs.getTime() - oldTs.getTime();
        return timeUnit.convert(diffInMS, TimeUnit.MILLISECONDS);
    }

    /**
     * Endpoint.
     *
     * @return response entity
     */
    @RequestMapping("/")
    public String index() {
        //create table for login attempts --> we need only one
        //db info
        String host = "localhost";
        String port = "5432";
        String dbName = "GDPR";
        String username = "GDPR";
        String password = "";
        Connection connection = null;
        Statement statement = null;
        boolean tableExist = false;
        try {

            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://"
                    + host + ":" + port + "/"
                    + dbName + "", "" + username + "", "" + password);
            if (connection != null) {
                System.out.println("connection ok!");
                //check if table exists
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                ResultSet resultSet = databaseMetaData.getTables(null, "logging", null, new String[]{"TABLE"});
                if (resultSet.next()) {
                    tableExist = true;
                    System.out.println("table exists: " + tableExist);
                } else {
                    //create table
                    String query = "CREATE TABLE IF NOT EXISTS logging(id SERIAL PRIMARY KEY, username varchar (50),"
                          +  " timestamp TIMESTAMP)";
                    statement = connection.createStatement();
                    statement.executeUpdate(query);
                    System.out.println("finished");
                }
            } else {
                System.out.println("connection failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "addUser";
    }

    /**
     * Show message endpoint.
     *
     * @return model and view
     */
    @PostMapping(value = "/showMessage")
    public ModelAndView doStuffMethod() {
        System.out.println("Button clicked!");
        //page pwd changed
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("pwdChanged");

        //time when pwd changed

        String queryModPwd = "UPDATE users SET last_modified=? WHERE username=?";
        String host = "localhost";
        String port = "5432";
        String dbName = "GDPR";
        String username = "GDPR";
        String password = "";
        Connection connection = null;
        PreparedStatement stmt = null;
        //alter column
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://"
                    + host + ":" + port + "/"
                    + dbName + "", "" + username + "", "" + password);
            if (connection != null) {
                System.out.println("you are connected");
                long now = System.currentTimeMillis();
                Timestamp sqlTimestamp = new Timestamp(now);
                System.out.println("current time: " + sqlTimestamp);
                stmt = connection.prepareStatement(queryModPwd);
                stmt.setTimestamp(1, sqlTimestamp);

                System.out.println("login name: " + loginName);
                stmt.setString(2, loginName);
                int affectedrows = stmt.executeUpdate();
            } else {
                System.out.println("connection failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return modelAndView;
    }

    /**
     * Add user endpoint.
     *
     * @param user the user
     * @return model and view
     */
    @PostMapping(value = "/addUser")
    public ModelAndView save(@ModelAttribute User user) {
        System.out.println("User from UI = " + user);
        boolean success = false;

        //page for succes login
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("showMessage");
        modelAndView.addObject("user", user);

        //page for fail login
        ModelAndView modelAndView2 = new ModelAndView();
        modelAndView2.setViewName("addUser");
        modelAndView2.addObject("user", user);

        //page to block login
        ModelAndView modelAndView3 = new ModelAndView();
        modelAndView3.setViewName("block");

        //page to change pwd
        ModelAndView modelAndView4 = new ModelAndView();
        modelAndView4.setViewName("msgChangePwd");

        //db code
        String query = "SELECT * FROM users WHERE username=? AND password = crypt(?, password)";
        String host = "localhost";
        String port = "5432";
        String dbName = "GDPR";
        String username = "GDPR";
        String password = "";

        Connection connection = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtCountQuery = null;
        PreparedStatement stmtSQL = null;
        PreparedStatement stmtTimestamp = null;

        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://"
                    + host + ":" + port + "/"
                    + dbName + "", "" + username + "", "" + password);

            System.out.println("before connection");
            if (connection != null) {
                System.out.println("connection ok!");
                stmt = connection.prepareStatement(query);


                //credentials
                String name = user.getName();
                String pwd = user.getPassword();
                loginName = name;

                System.out.println("name is : " + name + " pwd is : " + pwd);
                stmt.setString(1, name);
                stmt.setString(2, pwd);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    // Login Successful if match is found
                    success = true;
                    System.out.println("Success!");

                    //check if you need to change pwd
                    //compare timespamps
                    String getTimestampQuery = "SELECT last_modified FROM users WHERE username=?";
                    stmtTimestamp = connection.prepareStatement(getTimestampQuery);
                    stmtTimestamp.setString(1, name);
                    ResultSet rsTimestamp = stmtTimestamp.executeQuery();
                    while (rsTimestamp.next()) {
                        Timestamp lastModified = rsTimestamp.getTimestamp("last_modified");
                        System.out.println("last modified at: " + lastModified);
                        long now = System.currentTimeMillis();
                        Timestamp currentTimestamp = new Timestamp(now);

                        long diff = getDateDiff(lastModified, currentTimestamp, TimeUnit.DAYS);
                        System.out.println("difference in days is: " + diff);
                        System.out.println("difference in hours is: " + diff);
                        if (diff >= Constants.DAYS_TO_CHANGE_PWD) {
                            System.out.println("pwd must be changed");
                            //msgChangePwd
                            return modelAndView4;
                        }

                    }

                } else {
                    // Login failed --> here handle failed attempt!!!
                    //get current time
                    //String current_time = getCurrentTimeStamp();
                    long now = System.currentTimeMillis();
                    Timestamp sqlTimestamp = new Timestamp(now);

                    //add fail atempts in logging
                    String sql = "INSERT INTO logging(username ,timestamp) " + "VALUES(?,?)";
                    stmtSQL = connection.prepareStatement(sql);
                    stmtSQL.setString(1, name);
                    stmtSQL.setTimestamp(2, sqlTimestamp);
                    int rsSQL = stmtSQL.executeUpdate();
                    if (rsSQL > 0) {
                        System.out.println("insert row");
                    }

                    //Query to get the number of rows in a table
                    String queryCount = "SELECT COUNT(*) FROM logging WHERE username=?";
                    stmtCountQuery = connection.prepareStatement(queryCount);
                    stmtCountQuery.setString(1, name);
                    //Executing the query
                    ResultSet rsCount = stmtCountQuery.executeQuery();
                    //Retrieving the result
                    rsCount.next();
                    int count = rsCount.getInt(1);
                    System.out.println("Number of " + name + " records in the table: " + count);
                    if (count >= Constants.MAXIMUM_LOGIN_ATTEMPTS) {
                        //return block index
                        return modelAndView3;
                    }

                    //if count > 2 -->html page to block this user
                }
                System.out.println("boolean is: " + success);
                rs.close();
            } else {
                System.out.println("connection failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (success) {
            return modelAndView;
        } else {
            return modelAndView2;
        }
    }

}
