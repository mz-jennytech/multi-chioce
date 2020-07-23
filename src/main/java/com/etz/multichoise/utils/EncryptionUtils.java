package com.etz.multichoise.utils;

import java.math.BigInteger;
import java.security.MessageDigest;

public class EncryptionUtils {
  public static String md5(String value) {
    String macValue = "";
    try {
      MessageDigest mdEnc = MessageDigest.getInstance("MD5");
      mdEnc.update(value.getBytes(), 0, value.length());
      macValue = (new BigInteger(1, mdEnc.digest())).toString(16);
      int len = 32 - macValue.length();
      for (int i = 0; i < len; i++)
        macValue = "0" + macValue; 
    } catch (Exception e) {
      System.out.println("Error generating Check Value :: " + e.getMessage());
      macValue = "";
    } 
    return macValue;
  }
}
