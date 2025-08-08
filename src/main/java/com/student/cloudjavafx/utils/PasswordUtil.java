/* 
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license 
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template 
*/
package com.student.cloudjavafx.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** 
* 
* @author asyrn 
*/ 

public class PasswordUtil { 

// A function to encrypt the password 
public static String hashPassword(String password) { 
return BCrypt.withDefaults().hashToString(12, password.toCharArray()); 
}

// Function to verify password against hash
public static boolean verifyPassword(String inputPassword, String storedHash) {
if (storedHash == null || !storedHash.startsWith("$2a$")) {
System.err.println("❌ Invalid hash format");
return false;
}

try {
BCrypt.Result result = BCrypt.verifyer().verify(inputPassword.toCharArray(), storedHash);
return result.verified;
} catch (Exception e) {
System.err.println("❌ Password verification error: " + e.getMessage());
return false;
}
}
}