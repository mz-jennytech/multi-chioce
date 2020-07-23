package com.etranzact.multichoiseussd;

import com.etz.ussd.processor.lib.dao.USSDDBBean;
import com.etz.ussd.processor.lib.util.DBUtilities;
import com.etz.ussd.processor.lib.util.SwitchUtilities;
import com.etz.ussd.processor.lib.util.UssdUtilities;
import com.etz.ussd.processor.lib.util.VasgateUtilities;
import com.google.gson.JsonObject;
import java.util.HashMap;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.apache.log4j.Logger;

@Path("/MultiChoiceUSSD")
public class AddAccountStraightDial {
  private static final Logger LOGGER = Logger.getLogger(AddAccountStraightDial.class);
  
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
  @Path("getDueAmountStraightDial")
  public String getDueAmount(HashMap<String, String> req) {
    String mobileNum = req.get("cli");
    String reference = UssdUtilities.generateUniqueId(mobileNum);
    String cableTV = req.get("cableTV");
    String[] userFirstDial = ((String)req.get("userFirstDial")).split("\\*");
    String smartCardNumber = userFirstDial[userFirstDial.length - 1];
    LOGGER.info("Smart Card Number >>>>>>>>>>>>>> " + smartCardNumber);
    String response = "";
    String payTV = cableTV.equals("1") ? "DSTVNEW" : "GOTVNEW";
    try {
      String dueAmount;
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
      String fullName = "";
      String dueDate = "";
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
    } catch (Exception ex) {
      LOGGER.info("VasGate Error ", ex);
      response = "[BANK:1]|Error occured processing request, please try again later. Thank you";
    } 
    return response;
  }
}
