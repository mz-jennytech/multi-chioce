package com.etz.multichoiseussd;

import com.etz.multichoise.utils.AddAcctUtils;
import com.etz.multichoise.utils.PayTVAddAccountProcessor;
import com.etz.multichoise.utils.PayTVProcessor;
import com.etz.multichoise.utils.VasGateJson;
import com.etz.ussd.processor.lib.dao.USSDDBBean;
import com.etz.ussd.processor.lib.model.UssdRequest;
import com.etz.ussd.processor.lib.model.UssdSubscriber;
import com.etz.ussd.processor.lib.model.UssdTransactionLog;
import com.etz.ussd.processor.lib.util.DBUtilities;
import com.etz.ussd.processor.lib.util.SwitchUtilities;
import com.etz.ussd.processor.lib.util.UssdUtilities;
import com.etz.ussd.processor.lib.util.VasgateUtilities;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

@Stateless
@Path("/MultiChoiceUSSD")
public class AddAccountPayTV {
  @Resource(name = "PAYTV_MERCHANT")
  public String PAYTV_MERCHANT;
  
  @Resource(name = "PAYTV_FEE_CAT")
  public String PAYTV_FEE_CAT;
  
  private static final String VASGATE_URL = "http://172.16.10.38:1505/receiver/action/vasgate";
  
  private static final Logger LOGGER = Logger.getLogger(AddAccountPayTV.class);
  
  private static final String SENDER_ID = "20220";
  
  @Inject
  private VasgateUtilities vasgateUtilities;
  
  @Inject
  private DBUtilities dBUtilities;
  
  @Inject
  private USSDDBBean bean;
  
  @Inject
  private SwitchUtilities switchUtilities;
  
  @Inject
  private UssdUtilities ussdUtilities;
  
  private static String invoicePeriod = "1";
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("redirect389Endpoint")
  public String redirectEndpoint(HashMap<String, String> req) {
    String t, userFirstDial = req.get("userFirstDial");
    String mobileNum = req.get("cli");
    String reference = UssdUtilities.generateUniqueId(mobileNum);
    LOGGER.info("389*9 userFirstDial " + userFirstDial);
    String response = "";
    try {
      t = userFirstDial.split("\\*")[3];
    } catch (Exception e) {
      t = "1";
    } 
    if (t.length() > 1) {
      try {
        String result = VasGateJson.doSmartVerifi("GOTVNEW", "3", "http://172.16.10.38:1505/receiver/action/vasgate", reference, t);
        System.out.println("Actual Result :" + result);
        if (result != null && result.length() > 5) {
          JsonObject resultObj = (new JsonParser()).parse(result).getAsJsonObject();
          String code = resultObj.get("error").getAsString();
          System.out.println("RESULT CODE ::" + code);
          String fullName = "";
          String dueAmount = "";
          String dueDate = "";
          if (code.equals("00")) {
            JsonObject resultObj2 = resultObj.get("extra").getAsJsonObject();
            fullName = resultObj2.get("fullname").getAsString();
            dueAmount = resultObj2.get("dueAmount").getAsString();
            dueDate = resultObj2.get("dueDate").getAsString();
            LOGGER.info("NAME ::" + fullName);
            LOGGER.info("DUE AMT ::" + dueAmount);
            LOGGER.info("INVOICE PERIOD :" + invoicePeriod);
            StringBuilder strB1 = new StringBuilder();
            strB1.append("Hi, ").append(fullName.trim()).append("~");
            strB1.append("Your due amount is =N=").append(dueAmount).append("~");
            strB1.append("Due date is ").append(dueDate).append("~");
            strB1.append("Do you want to make payment?").append("~");
            strB1.append("1. Proceed");
            response = fullName.trim() + "-" + dueAmount + "-" + invoicePeriod + "-" + strB1.toString();
          } else if (code.equals("56")) {
            response = "Invalid Smart Card Number Supplied, Please confirm and try again. Thank you";
          } else {
            response = "Hi,~Your due amount is =N=1900~Do you want to make payment?~1. Proceed";
          } 
        } 
      } catch (Exception ex) {
        LOGGER.info("VasGate Error ", ex);
        response = "Hi,~Your due amount is =N=1900~Do you want to make payment?~1. Proceed";
      } 
    } else if (t.equals("1")) {
      response = "Pay DSTV~~Enter Smart Card Number";
    } else if (t.equals("2")) {
      response = "Pay GOTV~~Enter IUC Number";
    } 
    return response;
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("getDueAmount")
  public String getDueAmount(HashMap<String, String> req) {
    String mobileNum = req.get("cli");
    String smartCardNumber = ((String)req.get("smartCardNumber")).split("\\|")[0];
    String reference = UssdUtilities.generateUniqueId(mobileNum);
    String cableTV = req.get("cableTV");
    String response = "";
    String payTV = cableTV.equals("1") ? "DSTVNEW" : "GOTVNEW";
    try {
      String fullName, dueAmount, dueDate;
      JsonObject resultObj2;
      StringBuilder strB1;
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("reference", reference);
      jsonObject.addProperty("alias", payTV);
      jsonObject.addProperty("action", "query");
      jsonObject.addProperty("type", "4");
      jsonObject.addProperty("account", smartCardNumber);
      String payTVreq = jsonObject.toString();
      JsonObject result = this.vasgateUtilities.postToVasgateJson(payTVreq);
      String code = result.get("error").getAsString();
      LOGGER.info("Actual Request : " + payTVreq);
      LOGGER.info("Actual Result :" + result);
      LOGGER.info("Error Result :" + code);
      switch (code) {
        case "00":
          resultObj2 = result.get("extra").getAsJsonObject();
          fullName = resultObj2.get("fullname").getAsString();
          dueAmount = resultObj2.get("dueAmount").getAsString();
          dueDate = resultObj2.get("dueDate").getAsString();
          LOGGER.info("NAME ::" + fullName);
          LOGGER.info("DUE AMT ::" + dueAmount);
          LOGGER.info("INVOICE PERIOD :" + invoicePeriod);
          strB1 = new StringBuilder();
          strB1.append("Hi, ").append(fullName.trim()).append("~");
          strB1.append("Your due amount is =N=").append(dueAmount).append("~");
          strB1.append("Due date is ").append(dueDate).append("~");
          strB1.append("Do you want to make payment?").append("~");
          strB1.append("1. Proceed");
          response = "[BANK:" + dueAmount + "]|" + strB1.toString();
          return response;
        case "56":
          response = "[BANK:1]|Invalid Smart Card Number Supplied, Please confirm and try again. Thank you";
          return response;
      } 
      response = "[BANK:1]|Error occured processing request, please try again later. Thank you";
    } catch (JsonSyntaxException ex) {
      LOGGER.info("VasGate Error ", (Throwable)ex);
      response = "[BANK:1]|Error occured processing request, please try again later. Thank you";
    } 
    return response;
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("getPayTV")
  public String getPayTV(HashMap<String, String> req) {
    String cableTV = req.get("cableTV");
    String response = "";
    switch (cableTV) {
      case "1":
        response = "[DUE:1]|Pay DSTV~~Enter Smart Card Number";
        break;
      case "2":
        response = "[DUE:2]|Pay GOTV~~Enter IUC Number";
        break;
      case "3":
        response = "[DUE:3]|Dial *389*0# to Register on PocketMoni Today. Thank you";
        break;
    } 
    LOGGER.info(response);
    return response;
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("doPayTV")
  public String doPayTV(HashMap<String, String> req) {
    String serviceid, alias, cardNumber, pin = ((String)req.get("pin")).split("\\|")[0];
    String confirmPin = ((String)req.get("confirmPin")).split("\\|")[0];
    if (!pin.equals(confirmPin))
      return "Both PIN did not match. Try again with correct pin. Thank you"; 
    String mobile = req.get("cli");
    String[] bankList = ((String)req.get("bankList")).split("\\-");
    String msg = ((String)req.get("pin")).split("\\|")[1];
    List<UssdSubscriber> subscribers = this.dBUtilities.getUssdSubscriberByMobileNoAppCode(mobile, "222");
    for (UssdSubscriber sb : subscribers) {
      if (msg.startsWith("Henceforth")) {
        sb.setPin(DigestUtils.sha256Hex(((String)req.get("pin")).split("\\|")[0]));
        this.bean.merge(sb);
      } 
    } 
    if (!((UssdSubscriber)subscribers.get(0)).getPin().equals(DigestUtils.sha256Hex(pin)))
      return "Both PIN did not match in our record. Try again with correct pin. Thank you"; 
    String dueAmount = req.get("dueAmount");
    JsonObject appLimitObj = this.ussdUtilities.getAppLimit("222", mobile, dueAmount, req.get("userFirstDial"));
    LOGGER.info("APP Limit OBJ " + appLimitObj);
    if (appLimitObj.get("responseCode").getAsInt() == 200) {
      JsonObject responseMessage = (new JsonParser()).parse(appLimitObj.get("responseMessage").getAsString()).getAsJsonObject();
      JsonObject jsonB = responseMessage.get("dailyLimit").getAsJsonObject();
      boolean single = jsonB.get("single").getAsJsonObject().get("allow").getAsBoolean();
      boolean cumulative = jsonB.get("cumulative").getAsJsonObject().get("allow").getAsBoolean();
      if (!single)
        return jsonB.get("single").getAsJsonObject().get("errorMessage").getAsString(); 
      if (!cumulative)
        return jsonB.get("cumulative").getAsJsonObject().get("errorMessage").getAsString(); 
    } 
    String cableTV = req.get("cableTV");
    String network = req.get("network");
    String reference = UssdUtilities.generateUniqueId(mobile);
    if (req.containsKey("smartCardNumber")) {
      serviceid = ((String)req.get("smartCardNumber")).split("\\|")[0];
    } else {
      String[] userFirstDial = ((String)req.get("userFirstDial")).split("\\*");
      serviceid = userFirstDial[userFirstDial.length - 1];
    } 
    String payTV = cableTV.equals("1") ? "DSTVNEW" : "GOTVNEW";
    if (payTV.equalsIgnoreCase("DSTVNEW")) {
      alias = "DSTV";
    } else {
      alias = "GOTV";
    } 
    LOGGER.info("Smart Card Number >>>>>>>>>>> " + serviceid);
    double amount = Double.parseDouble(dueAmount);
    String bankCode = bankList[0];
    String acctNo = bankList[2];
    String cardExpDate = "777777";
    String Apin = "7777";
    String nar = serviceid + ":R:" + payTV + ";" + serviceid;
    if (bankCode.equals("063") || bankCode.equals("039")) {
      cardNumber = bankCode + "KKK" + acctNo;
    } else {
      cardNumber = bankCode + "ZZZ" + acctNo;
    } 
    if (bankCode.equals("214"))
      nar = "Service ID : " + serviceid + ":R:" + alias + ";Service I"; 
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.execute((Runnable)new PayTVProcessor(this.dBUtilities, this.bean, this.switchUtilities, this.ussdUtilities, this.vasgateUtilities, network, reference, cardNumber, Apin, cardExpDate, amount, nar, serviceid, payTV, mobile, acctNo, bankCode, alias, this.PAYTV_MERCHANT, this.PAYTV_FEE_CAT));
    executorService.shutdown();
    String response = "Your request has been received and is being processed at the moment. Thank You.~Dial *389*amount*phone number# for instant airtime topup to loved ones!";
    return response;
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("doPayTVAddAccount")
  public String doPayTVAddAccount(HashMap<String, String> req) {
    try {
      String serviceid, bankCode, alias, clientStrings[], providerId, clientId;
      if (req.get("pin") == null && req.get("pina") == null)
        return "Please Create a 4-digit PIN"; 
      String pin = null;
      String confirmPin = null;
      if (req.get("pina") != null) {
        pin = ((String)req.get("payTV")).split("\\|")[0];
        confirmPin = req.get("pina");
      } else {
        pin = ((String)req.get("pin")).split("\\|")[0];
        confirmPin = ((String)req.get("confirmPin")).split("\\|")[0];
      } 
      if (!pin.equals(confirmPin))
        return "Both PIN did not match. Try again with correct pin. Thank you"; 
      String dueAmount = req.get("dueAmount");
      String mobile = req.get("cli");
      JsonObject appLimitObj = this.ussdUtilities.getAppLimit("222", mobile, dueAmount, req.get("userFirstDial"));
      LOGGER.info("APP Limit OBJ " + appLimitObj);
      if (appLimitObj.get("responseCode").getAsInt() == 200) {
        JsonObject responseMessage = (new JsonParser()).parse(appLimitObj.get("responseMessage").getAsString()).getAsJsonObject();
        JsonObject jsonB = responseMessage.get("dailyLimit").getAsJsonObject();
        boolean single = jsonB.get("single").getAsJsonObject().get("allow").getAsBoolean();
        boolean cumulative = jsonB.get("cumulative").getAsJsonObject().get("allow").getAsBoolean();
        if (!single)
          return jsonB.get("single").getAsJsonObject().get("errorMessage").getAsString(); 
        if (!cumulative)
          return jsonB.get("cumulative").getAsJsonObject().get("errorMessage").getAsString(); 
      } 
      String cableTV = req.get("cableTV");
      String reference = UssdUtilities.generateUniqueId(mobile);
      if (req.containsKey("smartCardNumber")) {
        serviceid = ((String)req.get("smartCardNumber")).split("\\|")[0];
      } else {
        String[] userFirstDial = ((String)req.get("userFirstDial")).split("\\*");
        serviceid = userFirstDial[userFirstDial.length - 1];
      } 
      LOGGER.info("smart card number >>>>>>>>>> " + serviceid);
      String accountNum = ((String)req.get("accountNum")).split("\\|")[0];
      String provider = req.get("network");
      if (req.containsKey("bankITListNEXT")) {
        bankCode = req.get("bankITListNEXT");
      } else {
        bankCode = req.get("bankITList");
      } 
      String response = "";
      String payTV = cableTV.equals("1") ? "DSTVNEW" : "GOTVNEW";
      if (payTV.equalsIgnoreCase("DSTVNEW")) {
        alias = "DSTV";
      } else {
        alias = "GOTV";
      } 
      LOGGER.info("Selected BankCode >>>>>>>>>>>>>>> " + bankCode);
      double amount = Double.parseDouble(dueAmount);
      boolean isVerified = false;
      switch (bankCode) {
        case "063":
          isVerified = this.ussdUtilities.verifyDiamond_AcctNumber(provider, dueAmount, reference, mobile, accountNum);
          break;
        case "033":
          isVerified = AddAcctUtils.verifyUBA_AccountNum(mobile, accountNum);
          break;
        default:
          clientStrings = UssdUtilities.getClientDetails(bankCode);
          providerId = clientStrings[0];
          clientId = clientStrings[1];
          isVerified = this.ussdUtilities.verifyAccountNumber(clientId, providerId, mobile, accountNum);
          break;
      } 
      LOGGER.info("Phone >>>>> " + mobile + " Account >>>>>> " + accountNum + ", bankCode: " + bankCode + ", match status >>>>>>" + isVerified);
      if (isVerified) {
        String cardNumber, cardExpDate = "777777";
        String Apin = "7777";
        String nar = serviceid + ":R:" + payTV + ";" + serviceid;
        if (bankCode.equals("063") || bankCode.equals("039")) {
          cardNumber = bankCode + "KKK" + accountNum;
        } else {
          cardNumber = bankCode + "ZZZ" + accountNum;
        } 
        if (bankCode.equals("214"))
          nar = "Service ID : " + serviceid + ":R:" + alias + ";Service I"; 
        int res = (isVerified == true) ? 0 : -1;
        String message = "Your request has been received and is being processed at the moment. Thank You.~Dial *389*amount*phone number# for instant airtime topup to loved ones!";
        ExecutorService es = Executors.newCachedThreadPool();
        es.execute((Runnable)new PayTVAddAccountProcessor(this.dBUtilities, this.bean, this.switchUtilities, this.ussdUtilities, this.vasgateUtilities, reference, cardNumber, Apin, cardExpDate, amount, nar, serviceid, payTV, mobile, accountNum, bankCode, alias, provider, this.PAYTV_MERCHANT, this.PAYTV_FEE_CAT));
        es.shutdown();
        UssdSubscriber subscriber = this.dBUtilities.getUssdSubscriber(bankCode, accountNum, "222", mobile);
        LOGGER.info("UssdSubscriber  >>>>>>>>>>>> " + subscriber);
        if (subscriber == null) {
          UssdSubscriber ussdSubscr = new UssdSubscriber();
          ussdSubscr.setAccountNo(accountNum);
          ussdSubscr.setBankCode(bankCode);
          ussdSubscr.setActive(true);
          ussdSubscr.setMobileNo(mobile);
          ussdSubscr.setCreated(new Date());
          ussdSubscr.setLastTranTime(new Date());
          ussdSubscr.setAppcode("222");
          ussdSubscr.setPin(DigestUtils.sha256Hex(pin));
          ussdSubscr.setModified(new Date());
          this.bean.create(ussdSubscr);
          LOGGER.info("UssdSubscriber Created >>>>>>>>>>>> " + subscriber);
        } else {
          subscriber.setPin(subscriber.getPin());
          this.bean.merge(subscriber);
          LOGGER.info("UssdSubscriber Updated >>>>>>>>>>>> " + subscriber);
          LOGGER.info("Merged with old PIN");
        } 
        UssdTransactionLog tLog = new UssdTransactionLog();
        tLog.setActionType(payTV.toUpperCase());
        tLog.setAmount(amount);
        tLog.setBankCode("700");
        tLog.setUserBankCode(bankCode);
        tLog.setUniqueTransId(reference);
        tLog.setTrans_date(new Date());
        tLog.setMobileNo(mobile);
        tLog.setAppid("222");
        tLog.setProvider(provider.toUpperCase());
        tLog.setShortCode("*389*9*" + serviceid);
        tLog.setResponseCode("" + res);
        tLog.setResponseMessage(message);
        LOGGER.info("Logging Transactions......." + tLog);
        this.dBUtilities.logTransaction(tLog);
        response = message;
      } else {
        String message = "You have entered an account number that is not tied to your phone number with " + UssdUtilities.getBankName(bankCode) + " bank. ~ Ref:" + reference + ". Thank You";
        UssdRequest ussdRequest = new UssdRequest();
        ussdRequest.setAppid("Biller");
        ussdRequest.setCreated(new Date());
        ussdRequest.setMobileNo(mobile);
        ussdRequest.setProvider(provider);
        ussdRequest.setMessage(message);
        ussdRequest.setReference(reference);
        this.dBUtilities.logUssdRequest(ussdRequest);
        response = message;
      } 
      LOGGER.info(response);
      return response;
    } catch (Exception xc) {
      xc.printStackTrace();
      return "Something went wrong!~Please dial the code again.Thank you";
    } 
  }
}
