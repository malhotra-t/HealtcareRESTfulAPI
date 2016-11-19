package hello;

import java.io.BufferedReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;

@RestController
public class HelloController {
	Jedis jedis = new Jedis("localhost", 6379);

	@RequestMapping(method = RequestMethod.POST, consumes = "application/json", path = "/schema")
	public String createSchema(HttpServletRequest request, HttpServletResponse resp) {

		System.out.println("start of createSchema");

		StringBuffer jsonStringBuffer = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jsonStringBuffer.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(jsonStringBuffer);

		String jsonData = jsonStringBuffer.toString();

		org.json.JSONObject incJsonObj = new org.json.JSONObject(jsonData);

		String entityTitle = incJsonObj.get("title").toString();
		System.out.println("extracted value of title:   " + entityTitle);
		String schemaKey = "schema_" + entityTitle;
		jedis.set(schemaKey, jsonData);
		System.out.println("end of createSchema");
		return schemaKey;
	}

	// step 1 - allow json input
	@RequestMapping(method = RequestMethod.POST, consumes = "application/json", path = "/{entitySchema}")
	public String createEntity(@PathVariable String entitySchema, HttpServletRequest request,
			HttpServletResponse resp) {

		String bearerToken = request.getHeader("Authorization");
		System.out.println("bearerToken" + bearerToken);
		
		if (bearerToken == null || bearerToken.isEmpty()){
			resp.setStatus(HttpStatus.UNAUTHORIZED.value());
			return "";
		}
		String decrytptedVal = Authentication.decrypt("secretkeytanyamalhotra", bearerToken);
		System.out.println("decrytptedVal" + decrytptedVal);
		if (decrytptedVal.isEmpty() || !decrytptedVal.equals("validuser23")) {
			resp.setStatus(HttpStatus.UNAUTHORIZED.value());
			return "";
		}
		
		// step 2 - read json and parse it using json simple

		StringBuffer jsonStringBuffer = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null) {
				jsonStringBuffer.append(line);
			}
		} catch (Exception e) {
			/* report an error */ }

		String jsonData = jsonStringBuffer.toString();

		// step 3 - schema validation

		try {
			org.json.JSONObject incJsonObj = new org.json.JSONObject(jsonData);

			// Fetch schema from Redis for validation
			String schemaJsonStr = jedis.get("schema_" + entitySchema);
			org.json.JSONObject rawSchema = new org.json.JSONObject(schemaJsonStr);
			Schema schemaJson = SchemaLoader.load(rawSchema);

			// validate incoming json input
			schemaJson.validate(incJsonObj);

			Map<String, Map<String, String>> individualObjects = new HashMap<>();
			Map<String, List<String>> relationships = new HashMap<>();

			System.out.println("validation successful");

			String rootObjKey = updateObjectsAndRelationships(incJsonObj, entitySchema, individualObjects,
					relationships);

			for (String key : individualObjects.keySet()) {
				Map<String, String> simpleValMap = individualObjects.get(key);
				jedis.hmset(key, simpleValMap);
			}
			for (String key : relationships.keySet()) {
				List<String> simpleValList = relationships.get(key);
				String[] simpleValArr = simpleValList.toArray(new String[simpleValList.size()]);
				jedis.lpush(key, simpleValArr);
			}

			System.out.println("individualObjects" + individualObjects);

			System.out.println("relationships" + relationships);

			String[] keyContents = rootObjKey.split("_");

			return keyContents[1];

		} catch (JSONException je) {
			resp.setStatus(HttpStatus.BAD_REQUEST.value());
			return "Input provided is not a JSON." + je.getMessage();
		} catch (ValidationException ve) {
			resp.setStatus(HttpStatus.BAD_REQUEST.value());
			return "Sorry JSON could not be validated against the schema." + ve.getErrorMessage();
		}

	}

	private String updateObjectsAndRelationships(JSONObject incJsonObj, String parentSchema,
			Map<String, Map<String, String>> individualObjects, Map<String, List<String>> relationships) {

		Map<String, String> simpleValues = new HashMap<>();
		UUID uid = UUID.randomUUID();

		for (Object key : incJsonObj.keySet()) {
			Object value = incJsonObj.get(key.toString());
			if (value instanceof String) {
				simpleValues.put(key.toString(), value.toString());
			} else if (value instanceof Boolean) {

				Boolean boolval = (Boolean) value;

				String boolStrRep = boolval.toString();

				simpleValues.put(key.toString(), boolStrRep);

			} else if (value instanceof Integer) {

				simpleValues.put(key.toString(), value.toString());

			} else if (value instanceof Float) {

				simpleValues.put(key.toString(), value.toString());

			} else if (value instanceof JSONObject) {
				String relationKey = parentSchema + "_" + uid.toString() + "_" + key.toString();
				List<String> relationshipsList = new ArrayList<>();
				relationships.put(relationKey, relationshipsList);

				// recursively update objects and relationships maps
				relationshipsList.add(updateObjectsAndRelationships((JSONObject) value, key.toString(),
						individualObjects, relationships));

			} else if (value instanceof JSONArray) {

				int len = ((JSONArray) value).length();
				for (int i = 0; i < len; i++) {
					JSONObject item = ((JSONArray) value).getJSONObject(i);
					String relationKey = parentSchema + "_" + uid.toString() + "_" + key.toString();
					List<String> relationshipsList = relationships.get(relationKey);
					if (relationshipsList == null) {
						relationshipsList = new ArrayList<>();
						relationships.put(relationKey, relationshipsList);
					}

					// recursively update objects and relationships maps
					relationshipsList
							.add(updateObjectsAndRelationships(item, key.toString(), individualObjects, relationships));
				}
			} else {
				System.out.println("reached else block - Neither String nor JSONObject");
			}
		}

		simpleValues.put("id", uid.toString());
		String objectKey = parentSchema + "_" + uid;
		individualObjects.put(objectKey, simpleValues);

		return objectKey;
	}

	@RequestMapping("/")
	public String index() {
		return "Greetings from Spring Boot!";
	}

	// step 1 - allow GET request
	@RequestMapping(method = RequestMethod.GET, path = "/{schema}/{id}")
	public String getEntity(@PathVariable String schema, @PathVariable String id, HttpServletRequest request,
			HttpServletResponse resp) {
		
		
		// Step 2 - Read database

		

		// fetch simple values of root object from redis using schema and id
		String inputKey = schema + "_" + id;
		Map<String, String> jsonEntity = jedis.hgetAll(inputKey);

		// return HTTP status code 204 for No Content.
		if (jsonEntity == null) {
			resp.setStatus(HttpStatus.NO_CONTENT.value());
			return "";
		}

		JSONObject jsonObj = new JSONObject(jsonEntity);
		
		//get deeply nested JSON object
		
		appendDeeplyNestedObjects(inputKey, jsonObj);

		// Step 3 - return appropriate response
		String jsonEntityStr = jsonObj.toString();
		// generate Etag
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
	        md.update(jsonEntityStr.getBytes());

	        byte byteData[] = md.digest();

	        //convert the byte to hex format method 1
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < byteData.length; i++) {
	         sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	        }
			
			String etag = sb.toString();
			resp.setHeader("ETag", etag);
			
			String reqEtag = request.getHeader("If-None-Match");
			System.out.println("req etag content:  "+reqEtag);
			if(etag.equals(reqEtag)){
				resp.setStatus(HttpStatus.NOT_MODIFIED.value());
				return "";
			}
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return jsonEntityStr;
	}
	
	private void appendDeeplyNestedObjects(String nestedObjKey, JSONObject jsonObj) {
		Set<String> relKeySet = jedis.keys(nestedObjKey + "_*");

		for (String key : relKeySet) {

			Long keyListLen = jedis.llen(key);
			List<String> relChildKeys = jedis.lrange(key, 0, keyListLen);

			if (relChildKeys.size() > 1) {
				JSONArray arr = new JSONArray();
				for (String childKey : relChildKeys) {
					Map<String, String> childMap = jedis.hgetAll(childKey);
					JSONObject innerObj = new JSONObject(childMap);
					appendDeeplyNestedObjects(childKey, innerObj);
					arr.put(innerObj);
				}
				String[] keyContents = relChildKeys.get(0).split("_");
				jsonObj.put(keyContents[0], arr);
			} else {
				String innerObjKey = relChildKeys.get(0);
				Map<String, String> childMap = jedis.hgetAll(innerObjKey);
				String[] keyContents = innerObjKey.split("_");
				JSONObject innerObj = new JSONObject(childMap);
				jsonObj.put(keyContents[0], innerObj);
				appendDeeplyNestedObjects(innerObjKey, innerObj);
			}
		}
		
	}

	@RequestMapping(method = RequestMethod.PATCH, consumes = "application/json", path = "/{entitySchema}")
	public String mergeEntity(@PathVariable String entitySchema, HttpServletRequest request,
			HttpServletResponse resp) {
		// step 2 - read json and parse it using json simple

				StringBuffer jsonStringBuffer = new StringBuffer();
				String line = null;
				try {
					BufferedReader reader = request.getReader();
					while ((line = reader.readLine()) != null) {
						jsonStringBuffer.append(line);
					}
					
					String jsonData = jsonStringBuffer.toString();
					org.json.JSONObject incJsonObj = new org.json.JSONObject(jsonData);
					// Fetch schema from Redis for validation
					String schemaJsonStr = jedis.get("schema_" + entitySchema);
					org.json.JSONObject rawSchema = new org.json.JSONObject(schemaJsonStr);
					Schema schemaJson = SchemaLoader.load(rawSchema);

					// validate incoming json input
					schemaJson.validate(incJsonObj);
					mergeIndividualObjects(incJsonObj,entitySchema);
					return "merge successful";
				} catch (ValidationException ve) {
					resp.setStatus(HttpStatus.BAD_REQUEST.value());
					return "Sorry JSON could not be validated against the schema." + ve.getErrorMessage();
				}catch (Exception e) {
					resp.setStatus(HttpStatus.BAD_REQUEST.value());
					return ""; 
				}				

		
		
	}

	private void mergeIndividualObjects(JSONObject incJsonObj, String entitySchema) {

		
		String objId = (String)incJsonObj.get("id");
		String rootObjKey = entitySchema+"_"+objId;
		
		for(Object keyObj:incJsonObj.keySet()){
			String field = keyObj.toString();			
			Object value = incJsonObj.get(field);
			if (value instanceof String || value instanceof Integer || value instanceof Float || value instanceof Boolean) {
				jedis.hset(rootObjKey, field, value.toString());
			} else if (value instanceof JSONObject) {
				mergeIndividualObjects((JSONObject)value, field);

			} else if (value instanceof JSONArray) {
				JSONArray valueArr = (JSONArray) value; 
				int len = valueArr.length();
				for (int i = 0; i < len; i++) {
					mergeIndividualObjects((JSONObject)valueArr.get(i), field);
				}
			}
			
		}
		
	}

}
