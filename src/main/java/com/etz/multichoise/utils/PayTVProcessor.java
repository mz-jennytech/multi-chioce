package com.etz.multichoise.utils;

import com.etz.ussd.processor.lib.dao.USSDDBBean;
import com.etz.ussd.processor.lib.model.UssdRequest;
import com.etz.ussd.processor.lib.model.UssdTransactionLog;
import com.etz.ussd.processor.lib.util.DBUtilities;
import com.etz.ussd.processor.lib.util.SwitchUtilities;
import com.etz.ussd.processor.lib.util.UssdUtilities;
import com.etz.ussd.processor.lib.util.VasgateUtilities;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import org.apache.log4j.Logger;

public class PayTVProcessor implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(PayTVProcessor.class);
  
  private static final String SENDER_ID = "20220";
  
  private final DBUtilities dBUtilities;
  
  private final USSDDBBean bean;
  
  private final SwitchUtilities switchUtilities;
  
  private final UssdUtilities ussdUtilities;
  
  private final VasgateUtilities vasgateUtilities;
  
  private final String reference;
  
  private final String cardNumber;
  
  private final String Apin;
  
  private final String cardExpDate;
  
  private final String nar;
  
  private final String serviceid;
  
  private final String payTV;
  
  private final String mobile;
  
  private final String acctNo;
  
  private final String bankCode;
  
  private final String alias;
  
  private final String network;
  
  private final double amount;
  
  private final String feeCat;
  
  private final String merchant;
  
  public PayTVProcessor(DBUtilities dBUtilities, USSDDBBean bean, SwitchUtilities switchUtilities, UssdUtilities ussdUtilities, VasgateUtilities vasgateUtilities, String network, String reference, String cardNumber, String Apin, String cardExpDate, double amount, String nar, String serviceid, String payTV, String mobile, String acctNo, String bankCode, String alias, String merchant, String feeCat) {
    this.dBUtilities = dBUtilities;
    this.bean = bean;
    this.ussdUtilities = ussdUtilities;
    this.switchUtilities = switchUtilities;
    this.vasgateUtilities = vasgateUtilities;
    this.network = network;
    this.reference = reference;
    this.cardNumber = cardNumber;
    this.Apin = Apin;
    this.cardExpDate = cardExpDate;
    this.amount = amount;
    this.nar = nar;
    this.serviceid = serviceid;
    this.payTV = payTV;
    this.mobile = mobile;
    this.acctNo = acctNo;
    this.bankCode = bankCode;
    this.alias = alias;
    this.merchant = merchant;
    this.feeCat = feeCat;
  }
  
  public void doPayTVTransactions(String network, String reference, String cardNumber, String Apin, String cardExpDate, double amount, String nar, String serviceid, String payTV, String mobile, String acctNo, String bankCode, String alias, String merchant, String feeCat) {
    String checkvalue, mac;
    JsonObject jo, result;
    UssdRequest ussdRequestx;
    String message = "";
    LOGGER.info("Doing AutoSwitch Call");
    LOGGER.info("Merchant " + merchant);
    LOGGER.info("Fee Category " + feeCat);
    JsonObject ob = this.switchUtilities.doPaymentByCard("P", reference, cardNumber, Apin, cardExpDate, amount, 0.0D, merchant, "", nar, feeCat);
    int responseCode = ob.get("responseCode").getAsInt();
    LOGGER.info("AutoSwitch Response >>>>>> " + responseCode);
    LOGGER.info("AutoSwitch Request Obj " + ob);
    switch (responseCode) {
      case 0:
        checkvalue = reference + amount + serviceid + "JUSTTOPUP";
        LOGGER.info("checkvalue " + checkvalue);
        mac = EncryptionUtils.md5(checkvalue);
        jo = new JsonObject();
        jo.addProperty("reference", reference);
        jo.addProperty("amount", Double.valueOf(amount));
        jo.addProperty("alias", payTV);
        jo.addProperty("action", "process");
        jo.addProperty("mobile", mobile);
        jo.addProperty("account", serviceid);
        jo.addProperty("merchant", merchant);
        jo.addProperty("name", "JUSTOPUP");
        jo.addProperty("type", "4");
        jo.addProperty("type2", "0");
        jo.addProperty("mac", mac);
        jo.addProperty("bank", bankCode);
        jo.addProperty("mode", "0");
        jo.addProperty("client", "JUSTTOPUP");
        jo.addProperty("otherinfo", "1");
        result = this.vasgateUtilities.postToVasgateJson(jo.toString());
        LOGGER.info("VASGATE PAYTV RESULT " + result);
        if (result.get("httpCode").getAsInt() == 200 || result.get("httpCode").getAsInt() == 503) {
          LOGGER.info("I got a response of 200 or 503 proceed >>>>>>>");
          JsonElement el = result.get("error");
          if (el != null) {
            LOGGER.info("I got a response  >>>>>>> result.get(error) != null");
            if (el.getAsInt() == 0) {
              LOGGER.info("Transaction Successful if we get error field as 0. Please investigate if 503 is gotten >>>>>>>");
              if (alias.equalsIgnoreCase("DSTV")) {
                message = "You have successfully made a payment of N" + amount + " to " + alias + ". for Service ID :" + serviceid + " Ref:" + reference + ". Bouquet change? Check www.eazy.dstv.com";
              } else if (alias.equalsIgnoreCase("GOTV")) {
                message = "You have successfully made a payment of N" + amount + " to " + alias + ". for Service ID :" + serviceid + " Ref:" + reference + ". Bouquet change? Check eazy.gotvafrica.com/en/ng";
              } 
              this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
              LOGGER.info(message);
              LOGGER.info("Logging Transactions......." + mobile);
              UssdTransactionLog tLog = new UssdTransactionLog();
              tLog.setActionType(payTV.toUpperCase());
              tLog.setAmount(amount);
              tLog.setBankCode("222");
              tLog.setUserBankCode(bankCode);
              tLog.setUniqueTransId(reference);
              tLog.setTrans_date(new Date());
              tLog.setMobileNo(mobile);
              tLog.setAppid("222");
              tLog.setProvider(network.toUpperCase());
              tLog.setShortCode("*389*9*" + serviceid);
              tLog.setResponseCode("" + responseCode);
              tLog.setResponseMessage(message);
              LOGGER.info("Logging Transactions......." + mobile);
              this.dBUtilities.logTransaction(tLog);
            } else {
              message = "Your transaction cannot be processed at this time, please try again later. ~Ref:" + reference;
              this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
              LOGGER.info(message);
              UssdRequest ussdRequest = new UssdRequest();
              ussdRequest.setAppid("Biller");
              ussdRequest.setCreated(new Date());
              ussdRequest.setMobileNo(mobile);
              ussdRequest.setProvider(network);
              ussdRequest.setMessage(message);
              ussdRequest.setReference(reference);
              this.dBUtilities.logUssdRequest(ussdRequest);
            } 
          } else if (result.get("httpCode").getAsInt() == 503) {
            LOGGER.info("Please investigate, 503 is gotten >>>>>>> " + mobile);
            if (alias.equalsIgnoreCase("DSTV")) {
              message = "You have successfully made a payment of N" + amount + " to " + alias + ". for Service ID :" + serviceid + " Ref:" + reference + ". Bouquet change? Check www.eazy.dstv.com";
            } else if (alias.equalsIgnoreCase("GOTV")) {
              message = "You have successfully made a payment of N" + amount + " to " + alias + ". for Service ID :" + serviceid + " Ref:" + reference + ". Bouquet change? Check eazy.gotvafrica.com/en/ng";
            } 
            this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
            LOGGER.info(message);
            LOGGER.info("Logging Transactions......." + mobile);
            UssdTransactionLog tLog = new UssdTransactionLog();
            tLog.setActionType(payTV.toUpperCase());
            tLog.setAmount(amount);
            tLog.setBankCode("222");
            tLog.setUserBankCode(bankCode);
            tLog.setUniqueTransId(reference);
            tLog.setTrans_date(new Date());
            tLog.setMobileNo(mobile);
            tLog.setAppid("222");
            tLog.setProvider(network.toUpperCase());
            tLog.setShortCode("*389*9*" + serviceid);
            tLog.setResponseCode("" + responseCode);
            tLog.setResponseMessage(message);
            LOGGER.info("Logging Transactions......." + mobile);
            this.dBUtilities.logTransaction(tLog);
          } 
        } else {
          message = "Your transaction cannot be processed at this time, please try again later. ~Ref:" + reference;
          this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
          this.switchUtilities.doPaymentByCard("R", reference, cardNumber, Apin, cardExpDate, amount, 0.0D, "7006021166", "", nar);
          LOGGER.info(message);
          UssdRequest ussdRequest = new UssdRequest();
          ussdRequest.setAppid("Biller");
          ussdRequest.setCreated(new Date());
          ussdRequest.setMobileNo(mobile);
          ussdRequest.setProvider(network);
          ussdRequest.setMessage(message);
          ussdRequest.setReference(reference);
          this.dBUtilities.logUssdRequest(ussdRequest);
        } 
        return;
      case 5:
      case 25:
        message = "You have insufficient funds for this transaction. Please confirm your balance and try again. ~Ref:" + reference;
        this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
        LOGGER.info(message);
        ussdRequestx = new UssdRequest();
        ussdRequestx.setAppid("Biller");
        ussdRequestx.setCreated(new Date());
        ussdRequestx.setMobileNo(mobile);
        ussdRequestx.setProvider(network);
        ussdRequestx.setMessage(message);
        ussdRequestx.setReference(reference);
        this.dBUtilities.logUssdRequest(ussdRequestx);
        return;
    } 
    message = "Transaction failed, your bank did not approve this transaction. ~Ref:" + reference;
    this.ussdUtilities.sendSMS(reference, mobile, "20220", message);
    LOGGER.info(message);
    LOGGER.info("Inserting Transaction");
    UssdRequest ussdRequest2 = new UssdRequest();
    ussdRequest2.setAppid("Biller");
    ussdRequest2.setCreated(new Date());
    ussdRequest2.setMobileNo(mobile);
    ussdRequest2.setProvider(network);
    ussdRequest2.setMessage(message);
    ussdRequest2.setReference(reference);
    LOGGER.info("About logging transactions " + ussdRequest2);
    this.dBUtilities.logUssdRequest(ussdRequest2);
    LOGGER.info("Done Inserting Transaction    >>>>>>>> " + ussdRequest2.getReference());
  }
  
  public void run() {
    doPayTVTransactions(this.network, this.reference, this.cardNumber, this.Apin, this.cardExpDate, this.amount, this.nar, this.serviceid, this.payTV, this.mobile, this.acctNo, this.bankCode, this.alias, this.merchant, this.feeCat);
  }
}
