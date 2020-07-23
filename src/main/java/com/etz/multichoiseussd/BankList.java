package com.etz.multichoiseussd;


import com.etz.multichoise.utils.AddAcctUtils;
import com.etz.ussd.processor.lib.dao.USSDDBBean;
import com.etz.ussd.processor.lib.model.UssdSubscriber;
import com.etz.ussd.processor.lib.util.DBUtilities;
import com.etz.ussd.processor.lib.util.UssdUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.log4j.Logger;

@Path("/MultiChoiceUSSD")
public class BankList {
  @Inject
  private DBUtilities bUtilities;
  
  @Inject
  private USSDDBBean bean;
  
  private static final Logger LOGGER = Logger.getLogger(BankList.class);
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("bankList")
  public String bankList(HashMap<String, String> req) {
    String mobileNo = req.get("cli");
    List<UssdSubscriber> bankList = this.bUtilities.getUssdSubscriberByMobileNoAppCode(mobileNo, "222");
    StringBuilder sb = new StringBuilder();
    StringBuilder sbx = new StringBuilder();
    boolean isBankListEmpty = bankList.isEmpty();
    LOGGER.info("isBankListEmpty >>> " + isBankListEmpty);
    if (isBankListEmpty)
      return "[ADDACCT:1]|Pay DSTV~~No Account previously added, proceed with add account~~1. Add Account"; 
    int count = 1;
    for (int i = 0; i < bankList.size(); i++) {
      if (i < 8) {
        String bankNo = ((UssdSubscriber)bankList.get(i)).getAccountNo();
        String bankCode = ((UssdSubscriber)bankList.get(i)).getBankCode();
        String appID = ((UssdSubscriber)bankList.get(i)).getAppcode();
        String bankName = UssdUtilities.getBankName(bankCode);
        sb.append("~").append("[COMF:").append(bankCode).append("-").append(bankName).append("-").append(bankNo).append("-").append(appID).append("]");
        sbx.append("~").append(count).append(".").append(bankName).append(" ").append("*****").append(bankNo.substring(6));
        count++;
      } 
    } 
    LOGGER.info("<<<<<<< >>>>>> " + sbx);
    return sb.toString().substring(1) + "~[ADDACCT:1]|Select your bank~~" + sbx.toString().substring(1) + "~" + count + ". Add Account";
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("confirmAccount")
  public String confirmAccount(HashMap<String, String> req) {
    String[] userBank = ((String)req.get("bankList")).split("\\-");
    String mobile = req.get("cli");
    List<UssdSubscriber> us = this.bUtilities.getUssdSubscriberByMobileNoAppCode(mobile, "222");
    boolean isPinRequired = false;
    for (UssdSubscriber subscriber : us) {
      LOGGER.info("USSD " + subscriber.getPin());
      if (subscriber.getPin() != null) {
        LOGGER.info("NULL field captured.");
        isPinRequired = true;
      } 
    } 
    LOGGER.info("isPinRequired " + isPinRequired);
    if (isPinRequired)
      return "You have selected " + userBank[1] + " ******" + userBank[2].substring(6) + " Enter your PIN to confirm."; 
    return "Henceforth, PIN will be required when carrying out transactions on *389*9#. Enter your four(4) digit PIN";
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("confirmPin")
  public String confirmPin(HashMap<String, String> req) {
    return "Confirm your four(4) digit pin";
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("bankListAddAccount")
  public String bankListAddAccount(HashMap<String, String> req) {
    String cableTV = ((String)req.get("cableTV")).equals("1") ? "DSTV" : "GOTV";
    String BANK_LIST = AddAcctUtils.getBankList("BANK_LIST");
    JsonArray array = (new JsonParser()).parse(BANK_LIST).getAsJsonArray();
    StringBuilder sb = new StringBuilder();
    StringBuilder sbx = new StringBuilder();
    for (int i = 0; i <= 5; i++) {
      JsonObject jo = array.get(i).getAsJsonObject();
      String bank = jo.get("bank").getAsString();
      String bankCode = jo.get("bankCode").getAsString();
      sb.append("~").append("[BANKACC:").append(bankCode).append("]");
      sbx.append("~").append(i + 1).append(". ").append(bank);
    } 
    return sb.toString().substring(1) + "~[NEXT:1]|Pay " + cableTV + "~~Select your Bank~~" + sbx.toString().substring(1) + "~7. NEXT";
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("bankListAddAccountNext")
  public String bankListAddAccountNext(HashMap<String, String> req) {
    String cableTV = ((String)req.get("cableTV")).equals("1") ? "DSTV" : "GOTV";
    String BANK_LIST = AddAcctUtils.getBankList("BANK_LIST");
    JsonArray array = (new JsonParser()).parse(BANK_LIST).getAsJsonArray();
    StringBuilder sb = new StringBuilder();
    StringBuilder sbx = new StringBuilder();
    int j = 1;
    for (int i = 6; i < array.size(); i++) {
      JsonObject jo = array.get(i).getAsJsonObject();
      String bank = jo.get("bank").getAsString();
      String bankCode = jo.get("bankCode").getAsString();
      sb.append("~").append("[BANKACC:").append(bankCode).append("]");
      sbx.append("~").append(j).append(". ").append(bank);
      j++;
    } 
    return sb.toString().substring(1) + "|Pay " + cableTV + "~~Select your Bank~~" + sbx.toString().substring(1);
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("confirmPayTVForAddAcct")
  public String confirmPayTV(HashMap<String, String> req) {
    String cableTV = ((String)req.get("cableTV")).equals("1") ? "DSTV" : "GOTV";
    return "Pay " + cableTV + "~~Enter your account number";
  }
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("confirmAddAccount")
  public String confirmAddAccount(HashMap<String, String> req) {
    String accountNum = ((String)req.get("accountNum")).split("\\|")[0];
    String mobile = req.get("cli");
    List<UssdSubscriber> us = this.bUtilities.getUssdSubscriberByMobileNoAppCode(mobile, "222");
    boolean isPinRequired = false;
    for (UssdSubscriber subscriber : us) {
      if (subscriber.getPin() != null)
        isPinRequired = true; 
    } 
    String bankCode = req.get("bankITList");
    if (bankCode != null && bankCode.length() < 3) {
      bankCode = req.get("bankITListNEXT");
      return "You have selected " + UssdUtilities.getBankName(bankCode) + " ******" + accountNum.substring(6) + " Enter your PIN to confirm.";
    } 
    return "You have selected " + UssdUtilities.getBankName(bankCode) + " ******" + accountNum.substring(6) + " Enter your PIN to confirm.";
  }
}
