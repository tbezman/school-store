package com.bezman.servlet;

import com.bezman.reference.Reference;
import com.bezman.json.JSON;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.sql.*;

/**
 * Created by Terence on 11/8/2014.
 */
@Controller
@RequestMapping
public class IndexServlet {

    public static Statement statement;
    public static Connection connection;

    @RequestMapping(value = "/",method = RequestMethod.GET)
    public String processWelcome(Model model, HttpServletRequest request){

        IndexServlet.servletLoginCheck(model, request);

        model.addAttribute("motd", StringEscapeUtils.escapeHtml(Reference.motd).replace("\n", "<br/>"));

        return "main";
    }

    public static ResultSet execQuery(String query) throws SQLException {
        return connection.createStatement().executeQuery(query);
    }

    public static int execUpdate(String update) throws SQLException {
        return connection.createStatement().executeUpdate(update);
    }

    public boolean queryDoesReturn(String query) throws SQLException {
        ResultSet resultSet = execQuery(query);

        while(resultSet.next()){
            return true;
        }

        return false;
    }

    public static boolean isSessionAdmin(String sessionID){
        try {
            PreparedStatement sessionStatement = IndexServlet.connection.prepareStatement("SELECT * from sessions WHERE sessionID=?");
            sessionStatement.setString(1, sessionID);

            ResultSet resultSet = sessionStatement.executeQuery();

            String username = null;

            while(resultSet.next()){
                username = resultSet.getString("username");
            }

            if(username == null)
                return false;

            PreparedStatement roleStatement = IndexServlet.connection.prepareStatement("SELECT  * from accounts where username=?");
            roleStatement.setString(1, username);

            ResultSet accountsSet = roleStatement.executeQuery();

            while(accountsSet.next()){
                if (accountsSet.getString("role").equals("admin"))
                    return true;
            }

            return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    @PostConstruct
    public void startup(){
        System.out.println("THIS IS A STARTUP MESSAGE");

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connection = DriverManager.getConnection(Reference.dbLink, Reference.dbUsername, Reference.dbPass);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSONObject motdJSON = JSON.pullJSONObjectFromFile("motd.json");

        if(motdJSON == null){
            Reference.motd = "No messages today";
        }else{
            Reference.motd = (String) motdJSON.get("message");
        }
    }

    @PreDestroy
    public void preDestroy(){

        try {
            this.connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("message", Reference.motd);

        JSON.putJSONObjectToFile(jsonObject, "motd.json");
    }

    public static Cookie getCookie(Cookie[] cookies, String name){
        if(cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }

        return new Cookie("12", "12");
    }

    public static void servletLoginCheck(Model model, HttpServletRequest request){
        Cookie cookie = IndexServlet.getCookie(request.getCookies(), "sessionID");
        if (cookie != null){
            try {
                PreparedStatement statement = IndexServlet.connection.prepareStatement("SELECT  * from sessions where sessionID=?");
                statement.setString(1, cookie.getValue());

                ResultSet resultSet = statement.executeQuery();
                String username = null;

                while(resultSet.next()){
                    model.addAttribute("username", resultSet.getString("username"));
                    username = resultSet.getString("username");
                }

                PreparedStatement roleStatement = IndexServlet.connection.prepareStatement("SELECT * from accounts where username=?");
                roleStatement.setString(1, username);

                ResultSet accountSet = roleStatement.executeQuery();

                while(accountSet.next()){
                    model.addAttribute("role", accountSet.getString("role"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
