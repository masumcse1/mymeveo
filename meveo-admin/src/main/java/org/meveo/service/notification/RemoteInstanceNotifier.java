package org.meveo.service.notification;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings("deprecation")
@Stateless
public class RemoteInstanceNotifier {

	@Inject
	private Logger log;
	
	public void invoke(String input,String url) {
		/*
		try {
			log.debug("Request  ={}",input);
			log.debug("Url ={}",url);
			ClientRequest request = new ClientRequest(url);			
			JSONParser jsonParser =  new JSONParser();		
			request.body("application/json", jsonParser.parse(input));
			request.accept("application/json");

			ClientResponse<String> response = request.post(String.class);
			String jsonResponse = "";
			if (response.getStatus() != 201) {
				log.debug("invoke Failed : HTTP error code : "+ response.getStatus());
			} else {    
				jsonResponse=response.getEntity();
				log.info("Response jsonResponse ={}",jsonResponse);				
				JSONObject jsonResponseObject = (JSONObject) jsonParser.parse(jsonResponse);
				JSONObject jsonActionStatus =  (JSONObject) jsonResponseObject.get("actionStatus");
				String responseStatus  = (String) jsonActionStatus.get("status");
				
				if("SUCCESS".equals(responseStatus)){
					log.debug("invoke remote service ok");
				}else{
					log.debug("invoke remote service fail");
				}
			}
			
		} catch (Exception e) {
			log.error("Exception on invoke : ",e);
			
		}
		*/
	}
	
//	public void invoke(String input,String url) {
//		Response response =null;
//		try {		
//			log.debug("Request Check Update ={}",input);
//			log.debug("Request Check Update url={}",url);
//			Client client = ClientBuilder.newClient();
//			response = client.target(url).request().post(Entity.entity(input, MediaType.APPLICATION_JSON));
//			String value = response.readEntity(String.class);          
//			if (response.getStatus() != 201) {
//				log.debug("ChekUpdate Failed : HTTP error code : "+ response.getStatus());
//			} else {    
//				log.debug("ChekUpdate ok, response: "+ value);
//			}
//		} catch (Exception e) {
//			log.error("Exception on getVersionOutput : ",e);
//
//		}finally{
//			if(response != null){
//				response.close();
//			}
//		}
//	}
}
