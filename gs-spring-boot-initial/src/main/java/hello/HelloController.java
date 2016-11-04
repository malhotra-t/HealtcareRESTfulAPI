package hello;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;

@RestController
public class HelloController {
	// step 1 - allow json input
	@RequestMapping(method = RequestMethod.POST, consumes = "application/json", path = "/{schema}")
	public String createPlan(@PathVariable String schema, HttpServletRequest request, HttpServletResponse resp) {

		// step 2 - read json and parse it using json simple
		System.out.println("testing value of schema: " + schema);

		StringBuffer jsonStringBuffer = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jsonStringBuffer.append(line);
			}

		} catch (Exception e) {
			/* report an error */ }
		System.out.println(jsonStringBuffer);

		String jsonData = jsonStringBuffer.toString();

		// step 3 - schema validation
		
		InputStream inputStream = null;
		try {
			org.json.JSONArray incJsonArr = new org.json.JSONArray(jsonData);
			inputStream = new FileInputStream(
					"C:/Users/parit/Documents/workspace-sts-3.7.3.RELEASE/gs-spring-boot-initial/resources/schema.json");
			org.json.JSONObject rawSchema = new org.json.JSONObject(new JSONTokener(inputStream));
			Schema schemaJson = SchemaLoader.load(rawSchema);
			// schema.validate throws a ValidationException if this object is
			// invalid
			schemaJson.validate(incJsonArr);
			System.out.println("validation successful");
			Jedis jedis = new Jedis("localhost");
			List<String> uidList = new ArrayList<>();
			for (int i=0;i<incJsonArr.length();i++) {
				
				UUID uid = UUID.randomUUID();
				uidList.add(uid.toString());
				jedis.set(schema + "-" + uid.toString(), incJsonArr.get(i).toString());
			}
			return uidList.toString();
			
		} catch(JSONException je){
			resp.setStatus(HttpStatus.BAD_REQUEST.value());
			return "Input provided is not a JSON." + je.getMessage();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			resp.setStatus(HttpStatus.NO_CONTENT.value());
			return "Schema not found.";
		} catch(ValidationException ve){
			resp.setStatus(HttpStatus.BAD_REQUEST.value());
			return "Sorry JSON could not be validated against the schema." + ve.getErrorMessage();
		}
		

		// step 4 - json parser
		
//		JSONParser parser = new JSONParser();
//
//		Object obj = null;
//		try {
//			obj = parser.parse(jsonStringBuffer.toString());
//			System.out.println("obj::  " + obj);
//
//			List jsonArr = (List) obj;
//
//			System.out.println("jsonArr:: " + jsonArr);
//			//
//			// Map<String,String> jsonDataMap = (Map<String,String>)obj;
//			// String nameVal = jsonDataMap.get("planName");
//			Jedis jedis = new Jedis("localhost");
//			for (Object listObj : jsonArr) {
//				UUID uid = UUID.randomUUID();
//				jedis.set("plan-" + uid.toString(), listObj.toString());
//			}
//		} catch (ParseException pe) {
//			// 
//			pe.printStackTrace();
//			resp.setStatus(HttpStatus.BAD_REQUEST.value());
//			return "JSON could not be parsed";
//		} catch(Exception e){
//			
//			// http status 
//			resp.setStatus(HttpStatus.BAD_REQUEST.value());
//			return "failure";
//		}


		
	}

	@RequestMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

	// step 1 - allow GET  request
	@RequestMapping(method = RequestMethod.GET, path = "/{schema}/{id}")
	public String getEntity(@PathVariable String schema, @PathVariable String id, HttpServletRequest request, HttpServletResponse resp) {


		// Step 2 - Read database

		Jedis jedis = new Jedis("localhost");
		System.out.println("Connection to server sucessfully");
		// check whether server is running or not
		System.out.println("Server is running: " + jedis.ping());

		//StringBuffer strBuff = new StringBuffer();
		
//		org.json.JSONArray jArr = new org.json.JSONArray();

			
//		jArr.put(new org.json.JSONObject();
		
		String jsonEntity = jedis.get(schema + "-" + id);		
		
//   return HTTP status code 204 for No Content.		
		if (jsonEntity == null) {
			resp.setStatus(HttpStatus.NO_CONTENT.value());
		}

		// Step 3 - return appropriate response
		
		return jsonEntity;
	}
	
	
}
