package com.etz.multichoise.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class AddAcctUtils {
  public static String getBankList(String propName) {
    Properties prop = new Properties();
    InputStream input = null;
    try {
      String filename = "bankitlist.properties";
      input = AddAcctUtils.class.getClassLoader().getResourceAsStream(filename);
      if (input == null) {
        System.out.println("Sorry, unable to find " + filename);
        return "";
      } 
      prop.load(input);
      return prop.getProperty(propName);
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      close(input);
    } 
    return "";
  }
  
  private static void close(InputStream input) {
    if (input != null)
      try {
        input.close();
      } catch (IOException e) {
        e.printStackTrace();
      }  
  }
  
  public static boolean verifyUBA_AccountNum(String mobile, String accountNo) {
    boolean status = false;
    String result = "";
    try {
      String url = "http://172.20.232.137/entrust/processor.do?action=check&phone=" + mobile + "&accountno=" + accountNo;
      System.out.println("UBA Account Verification Url :: " + url);
      result = postHttpRequest(url).trim();
      if (!result.isEmpty()) {
        System.out.println("Mobile No :" + mobile + "Result :" + result);
        if (result.startsWith("valid")) {
          status = true;
          System.out.println("Mobile No :" + mobile + "Verification Status :" + status);
        } 
      } 
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    } 
    return status;
  }
  
  public static String postHttpRequest(String urlstr) {
    try {
      URL url = new URL(urlstr);
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setDoInput(true);
      conn.setDoOutput(true);
      InputStream in = conn.getInputStream();
      StringBuilder rcstr = new StringBuilder();
      byte[] b = new byte[4096];
      int len;
      while ((len = in.read(b)) != -1)
        rcstr.append(new String(b, 0, len)); 
      return rcstr.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    } 
  }
  
  public static String getConfig(String name) {
    String value;
    try {
      Context envEntryContext = (Context)(new InitialContext()).lookup("java:comp/env");
      value = (String)envEntryContext.lookup(name);
    } catch (NamingException ex) {
      ex.printStackTrace();
      return "";
    } 
    return value;
  }
}
