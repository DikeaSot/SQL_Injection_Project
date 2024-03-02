package com.networksecurity.bean;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        String queryModPwd = "SELECT * FROM users";
        String host = "localhost";
        String port = "5432";
        String dbName = "GDPR";
        String dbUsername = "GDPR";
        String password = "";
        Connection connection = null;
        PreparedStatement stmt = null;
        //alter column
        HashMap<String, String> credHashMap = new HashMap<String, String>(); //credentials HashMap

        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://"
                    + host + ":" + port + "/"
                    + dbName + "", "" + dbUsername + "", "" + password);
            if (connection != null) {
                System.out.println("you are connected");
                stmt = connection.prepareStatement(queryModPwd);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    credHashMap.put(rs.getString(1), rs.getString(2)); //username, password
                    System.out.println("Username: " + rs.getString(1));
                }

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
        //for users in Database
        for (String i : credHashMap.keySet()) {
            if (i.equals(username)) {
                return new User(i, BCrypt.hashpw(credHashMap.get(i), BCrypt.gensalt()), new ArrayList<>());
            }
        }

        //hardcoded user
        if ("MarilenaDikea".equals(username)) {
            //Bcrypt hash stand for the word security
            return new User("MarilenaDikea", BCrypt.hashpw("security", BCrypt.gensalt()),
                    new ArrayList<>());
        } else if ("admin".equals(username)) {
            //Bcrypt hash stand for the word security
            return new User("admin", "$2a$10$gIa5NlLSk/EOBR/L3Xgm2uJmMXzdwni9wKZ00invzW0yZ4v57yF2a",
                    new ArrayList<>());
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }

}