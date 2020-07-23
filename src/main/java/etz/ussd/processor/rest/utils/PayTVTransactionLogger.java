package com.etz.ussd.processor.rest.utils;

import com.etz.ussd.processor.lib.dao.USSDDBBean;
import com.etz.ussd.processor.lib.model.UssdBillers;
import com.etz.ussd.processor.lib.model.UssdSubscriber;
import com.etz.ussd.processor.lib.model.UssdTransactionLog;
import com.etz.ussd.processor.lib.util.DBUtilities;
import com.etz.ussd.processor.lib.util.UssdUtilities;
import java.util.Date;
import org.apache.log4j.Logger;

public class PayTVTransactionLogger implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(PayTVTransactionLogger.class);
  
  private static final String SENDER_ID = "20220";
  
  private DBUtilities dBUtilities;
  
  private USSDDBBean bean;
  
  private UssdUtilities ussdUtilities;
  
  private String bankCode;
  
  private String accountNum;
  
  private String mobile;
  
  private String pin;
  
  private String reference;
  
  private String alias;
  
  private String serviceid;
  
  private String payTV;
  
  private String provider;
  
  private String message;
  
  private double amount;
  
  private int responseCode;
  
  public PayTVTransactionLogger() {}
  
  public PayTVTransactionLogger(DBUtilities dBUtilities, USSDDBBean bean, UssdUtilities ussdUtilities, String bankCode, String accountNum, String mobile, String pin, String reference, String alias, String serviceid, String payTV, double amount, String provider, int responseCode, String message) {
    this.dBUtilities = dBUtilities;
    this.bean = bean;
    this.ussdUtilities = ussdUtilities;
    this.bankCode = bankCode;
    this.accountNum = accountNum;
    this.mobile = mobile;
    this.pin = pin;
    this.reference = reference;
    this.alias = alias;
    this.serviceid = serviceid;
    this.payTV = payTV;
    this.amount = amount;
    this.provider = provider;
    this.responseCode = responseCode;
    this.message = message;
  }
  
  public void run() {
    logTransactions(this.bankCode, this.accountNum, this.mobile, this.pin, this.reference, this.alias, this.serviceid, this.payTV, this.amount, this.provider, this.responseCode, this.message);
  }
  
  public void logTransactions(String bankCode, String accountNum, String mobile, String pin, String reference, String alias, String serviceid, String payTV, double amount, String provider, int responseCode, String message) {
    UssdSubscriber subscriber = this.dBUtilities.getUssdSubscriber(bankCode, accountNum, "222", mobile);
    if (subscriber == null) {
      UssdSubscriber ussdSubscr = new UssdSubscriber();
      ussdSubscr.setAccountNo(accountNum);
      ussdSubscr.setBankCode(bankCode);
      ussdSubscr.setPin(pin);
      ussdSubscr.setActive(true);
      ussdSubscr.setMobileNo(mobile);
      ussdSubscr.setCreated(new Date());
      ussdSubscr.setLastTranTime(new Date());
      ussdSubscr.setAppcode("222");
      this.bean.create(ussdSubscr);
      LOGGER.info("Logging Transactions......." + ussdSubscr);
      this.ussdUtilities.sendSMS(reference, mobile, "20220", "You have successfully registered your account " + accountNum.substring(0, 3) + "*****" + accountNum.substring(8) + " at " + UssdUtilities.getBankName(bankCode) + " bank for MultiChoice USSD payments. Please keep your PIN safe");
    } 
    UssdBillers ussdBiller = new UssdBillers();
    ussdBiller.setBillerName(alias);
    ussdBiller.setBillerNo(serviceid);
    ussdBiller.setMobileNo(mobile);
    ussdBiller.setActive(true);
    ussdBiller.setBillerType(payTV);
    ussdBiller.setAppcode("222");
    ussdBiller.setCreated(new Date());
    ussdBiller.setAmount(amount);
    UssdTransactionLog tLog = new UssdTransactionLog();
    tLog.setActionType(payTV.toUpperCase());
    tLog.setAmount(amount);
    tLog.setBankCode("222");
    tLog.setUserBankCode(bankCode);
    tLog.setUniqueTransId(reference);
    tLog.setTrans_date(new Date());
    tLog.setMobileNo(mobile);
    tLog.setAppid("222");
    tLog.setProvider(provider.toUpperCase());
    tLog.setShortCode("*389*9*" + serviceid);
    tLog.setResponseCode("" + responseCode);
    tLog.setResponseMessage(message);
    LOGGER.info("Logging Transactions......." + mobile);
    this.dBUtilities.logTransaction(tLog);
  }
}
