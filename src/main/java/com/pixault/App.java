package com.pixault;

import com.pixault.dao.UserDAO;
import com.pixault.model.User;

public class App {

    public static void main(String[] args) {

        User user = new User("Shiwani23@email.com", "123456");

        UserDAO dao = new UserDAO();

        if (dao.registerUser(user)) {
            System.out.println("User registered successfully!");
        } else {
            System.out.println("Registration failed.");
        }
    }
}
