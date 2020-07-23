package com.etranzact.multichoiseussd;

import com.etz.ussd.processor.lib.util.UssdUtilities;
import com.google.gson.JsonObject;
import java.util.HashMap;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/MultiChoiceUSSD")
public class OptOut {
  @Inject
  private UssdUtilities ussdUtilities;
  
  @POST
  @Consumes({"application/json"})
  @Produces({"text/plain"})
  @Path("optOut")
  public String optout(HashMap<String, String> req) {
    String mobile = req.get("cli");
    String userFirstDial = req.get("userFirstDial");
    JsonObject obj = this.ussdUtilities.doServiceOptOut("222", mobile);
    if (obj.get("responseCode").getAsInt() == 200)
      return "[AA:1]|You have successfully optout of " + userFirstDial + ". Thank you"; 
    return "OptOut is unsuccessful. Please try again after sometime. Thank you";
  }
}
