package com.zetcode.controller;

import com.zetcode.bean.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
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

@RestController
@CrossOrigin()
//@Controller
public class MyController {
   
   @RequestMapping({ "/hello" })
	public String firstPage() {
		return "Hello World";
	}
       
   }
