package com.auctionuet.server.util;

public class PasswordUtils {
    public static String generateSalt() { return "mock_salt"; }
    
    public static String hash(String password, String salt) { 
        return password + "_" + salt; 
    }
    
    public static boolean verify(String password, String salt, String hashedPassword) { 
        return hashedPassword.equals(hash(password, salt)); 
    }
}
