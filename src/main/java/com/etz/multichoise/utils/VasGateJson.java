package com.etz.multichoise.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class VasGateJson {
  static final String PUBLIC_KEY_FILE = "C:\\Keys\\vas-ussd.key";
  
  static String publicKey = "";
  
  static {
    try {
      ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("C:\\Keys\\vas-ussd.key"));
      publicKey = inputStream.readUTF();
      System.out.println("Loading key..");
    } catch (Exception e) {
      e.printStackTrace(System.out);
    } 
  }
  
  public static String doSmartVerifi(String alias, String type, String url, String uniqueId, String account) {
    String json = "";
    String ret = "";
    try {
      json = "{\"reference\":\"" + uniqueId + "\",\"alias\":\"" + alias + "\",\"action\":\"query\",\"type\":\"" + type + "\",\"account\":\"" + account + "\"}";
      System.out.println("json: " + json);
      ret = postSmartVeri(url, json);
    } catch (Exception e) {
      e.printStackTrace(System.out);
    } 
    return ret;
  }
  
  private static String postSmartVeri(String urls, String js) {
    String postResponse = "0";
    String successful = "";
    HttpURLConnection conn = null;
    try {
      InputStream is;
      URL url = new URL(urls);
      conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestProperty("CLIENT_APP", "JUSTTOPUP");
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
      String rsaa = doRSA(js);
      wr.writeBytes(rsaa);
      wr.flush();
      wr.close();
      int responseCode = conn.getResponseCode();
      System.out.println("Response Code : " + responseCode);
      StringBuffer response = new StringBuffer();
      if (conn.getResponseCode() == 200) {
        is = conn.getInputStream();
      } else {
        is = conn.getErrorStream();
      } 
      if (responseCode == 200) {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
          response.append(inputLine); 
        in.close();
        is.close();
        postResponse = response.toString();
        System.out.println("DSTV DETAILS RESPONSE : " + postResponse);
      } else if (responseCode == 503) {
        postResponse = "503";
      } else {
        System.out.println("HTTP Connection Failed to : " + url + "Response :" + postResponse);
      } 
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (conn != null)
        conn.disconnect(); 
    } 
    return postResponse;
  }
  
  public static String doRSA(String data) {
    String encData = "";
    try {
      Cipher cipher = Cipher.getInstance("RSA", "SunJCE");
      BASE64Decoder decoder = new BASE64Decoder();
      byte[] sigBytes2 = decoder.decodeBuffer(publicKey);
      X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(sigBytes2);
      KeyFactory keyFact = KeyFactory.getInstance("RSA");
      PublicKey pubKey2 = keyFact.generatePublic(x509KeySpec);
      cipher.init(1, pubKey2);
      byte[] cipherText = cipher.doFinal(data.getBytes());
      encData = (new BASE64Encoder()).encode(cipherText);
      System.out.println("cipher: " + encData);
    } catch (Exception e) {
      e.printStackTrace(System.out);
    } 
    return encData;
  }
}
