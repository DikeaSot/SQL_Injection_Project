package com.zetcode.controller;

import com.zetcode.bean.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit ;


@Controller
public class MyController {
   
   final int MAXIMUM_LOGIN_ATTEMPTS = 5;
   final int DAYS_TO_CHANGE_PWD = 90;
   
   public String login_name=""; // must be complited with username

   /*public String getCurrentTimeStamp() {
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
   }*/
 
   public static long getDateDiff(Timestamp oldTs, Timestamp newTs, TimeUnit timeUnit) {
    long diffInMS = newTs.getTime() - oldTs.getTime();
    return timeUnit.convert(diffInMS, TimeUnit.MILLISECONDS);
}
  
   @RequestMapping("/")
    public String index(){
        //create table for login attempts --> we need only one
        //db info
        String host = "localhost";
        String port = "5432";
        String db_name = "GDPR";
        String username = "GDPR";
        String password = "";
	Connection connection = null;
        Statement statement = null;
        boolean table_exist = false;
        try{

          Class.forName("org.postgresql.Driver");
          connection = DriverManager.getConnection("jdbc:postgresql://"+host+":"+port+"/"+db_name+"", ""+username+"", ""+password);
          if(connection != null){
             System.out.println("connection ok!");
            //check if table exists
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            ResultSet resultSet = databaseMetaData.getTables(null, "logging", null, new String[] {"TABLE"});
            if(resultSet.next()){
              table_exist = true;
              System.out.println("table exists: " + table_exist);
            } else{
              //create table
            String query = "CREATE TABLE IF NOT EXISTS logging(id SERIAL PRIMARY KEY, username varchar (50), timestamp TIMESTAMP)";
            statement = connection.createStatement();
            statement.executeUpdate(query);
            System.out.println("finished");
            }            
          }else{
            System.out.println("connection failed");
          }
        }catch(Exception e){
          e.printStackTrace();
        }finally{
           try{
             statement.close();
             connection.close();
           }catch(Exception e){
             e.printStackTrace();
           }   
        }
        return "addUser";
    }
    
     @RequestMapping(value="/showMessage", method = RequestMethod.POST)
      public ModelAndView doStuffMethod() {
      System.out.println("Button cliked!");
      //page pwd changed
      ModelAndView modelAndView = new ModelAndView();
      modelAndView.setViewName("pwdChanged");

      //time when pwd changed
      

      String query_mod_pwd = "UPDATE users SET last_modified=? WHERE username=?";
      String host = "localhost";
      String port = "5432";
      String db_name = "GDPR";
      String username = "GDPR";
      String password = "";
      Connection connection = null;
      PreparedStatement stmt = null;
      //alter column
      try{
        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection("jdbc:postgresql://"+host+":"+port+"/"+db_name+"", ""+username+"", ""+password);
        if(connection != null){
          System.out.println("you are connected");
          long now = System.currentTimeMillis();
          Timestamp sqlTimestamp = new Timestamp(now);
          System.out.println("current time: "+sqlTimestamp);
          stmt = connection.prepareStatement(query_mod_pwd);
          stmt.setTimestamp(1, sqlTimestamp);

          System.out.println("login name: "+login_name);
          stmt.setString(2, login_name);
          int affectedrows = stmt.executeUpdate();
        }else{
          System.out.println("connection failed");
        }
      }catch(Exception e){
           e.printStackTrace();
      } finally{
           try{
             stmt.close();
             connection.close();
           }catch(Exception e){
             e.printStackTrace();
           }   
        } 
      return modelAndView;
   }

    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    public ModelAndView save(@ModelAttribute User user){
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
        String db_name = "GDPR";
        String username = "GDPR";
        String password = "";

	Connection connection = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt_count_query = null;
        PreparedStatement stmt_SQL = null;
        PreparedStatement stmt_timestamp = null;

        try{
          Class.forName("org.postgresql.Driver");
          connection = DriverManager.getConnection("jdbc:postgresql://"+host+":"+port+"/"+db_name+"", ""+username+"", ""+password);
          
          System.out.println("before connection");
          if(connection != null){
            System.out.println("connection ok!");
            stmt = connection.prepareStatement(query);
            

            //credentials            
            String name = user.getName();
            String pwd = user.getPassword();
            login_name = name;

            System.out.println("name is : "+name + " pwd is : "+pwd);
            stmt.setString(1, name);
            stmt.setString(2, pwd);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
            // Login Successful if match is found
              success = true;
              System.out.println("Success!");

              //check if you need to change pwd
              //compare timespamps
              String get_timestamp_query = "SELECT last_modified FROM users WHERE username=?";
              stmt_timestamp = connection.prepareStatement(get_timestamp_query);
              stmt_timestamp.setString(1, name);
              ResultSet rs_timestamp = stmt_timestamp.executeQuery();
              while(rs_timestamp.next()){
                Timestamp prev_timestamp = rs_timestamp.getTimestamp("last_modified");
                System.out.println("last modified at: "+prev_timestamp);
                long now = System.currentTimeMillis();
                Timestamp currentTimestamp = new Timestamp(now);

                long diff = getDateDiff(prev_timestamp, currentTimestamp, TimeUnit.DAYS);
                System.out.println("difference in days is: "+diff);
                System.out.println("difference in hours is: "+diff);
                if(diff>=DAYS_TO_CHANGE_PWD){                
                  System.out.println("pwd must be changed");
                  //msgChangePwd
                  return modelAndView4;
                }

              }

            }else{
            // Login failed --> here handle failed attempt!!!
            //get current time
             //String current_time = getCurrentTimeStamp();
             long now = System.currentTimeMillis();
             Timestamp sqlTimestamp = new Timestamp(now);

             //add fail atempts in logging
             String SQL = "INSERT INTO logging(username ,timestamp) " + "VALUES(?,?)";
             stmt_SQL = connection.prepareStatement(SQL);
             stmt_SQL.setString(1, name);
             stmt_SQL.setTimestamp(2, sqlTimestamp);
             int rs_SQL = stmt_SQL.executeUpdate();
             if (rs_SQL> 0) System.out.println("insert row");

             //Query to get the number of rows in a table
             String query_count = "SELECT COUNT(*) FROM logging WHERE username=?";
             stmt_count_query = connection.prepareStatement(query_count);
             stmt_count_query.setString(1, name);
             //Executing the query
             ResultSet rs_count = stmt_count_query.executeQuery();
             //Retrieving the result
             rs_count.next();
             int count = rs_count.getInt(1);
             System.out.println("Number of "+ name+ " records in the table: "+count);
             if(count>=MAXIMUM_LOGIN_ATTEMPTS){
               //pop up window
              /* ScriptEngineManager manager = new ScriptEngineManager();
               ScriptEngine engine = manager.getEngineByName("JavaScript");
               // read script file
              engine.eval(Files.newBufferedReader(Paths.get("C:/Scripts/Jsfunctions.js"), StandardCharsets.UTF_8));

              Invocable inv = (Invocable) engine;
              // call function from script file
              inv.invokeFunction("yourFunction", "param"); */
               //return block index
               return modelAndView3;
             }
            
             //if count > 2 -->html page to block this user            
            }
            System.out.println("boolean is: "+ success);
            rs.close();
          }else{
             System.out.println("connection failed");
          }
        }catch(Exception e){
          e.printStackTrace();
        }finally{
           try{
             stmt.close();
             connection.close();
           }catch(Exception e){
             e.printStackTrace();
           }
   
        }
        if(success) return modelAndView;
        else{
          return modelAndView2;
        }
    }
    
   }
