package gov.usgs.cida;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.models.apideclaration.ApiDeclaration;
import io.swagger.parser.SwaggerCompatConverter;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.models.resourcelisting.ResourceListing;
import io.swagger.parser.util.RemoteUrl;
import io.swagger.report.MessageBuilder;
import io.swagger.transform.migrate.ApiDeclarationMigrator;
import io.swagger.transform.migrate.ResourceListingMigrator;
import io.swagger.util.Json;
import java.io.File;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map.Entry;


/**
 * This is an extension module to the SwaggerParser portion of Swagger CodeGen 
 * specifically made to handle the Swagger documents returned from Aquarius. 
 * It is loaded into the SwaggerParser application at runtime using the Java 
 * Service Provider Interface. See the 
 * src/main/resources/META-INF/services/io.swagger.parser.SwaggerParserExtension
 * file for the service definition.
 * 
 * @author zmoore
 */
public class AQSwaggerParser extends SwaggerCompatConverter {
	private static final Logger log = LoggerFactory.getLogger(AQSwaggerParser.class);
		
	@Override
    public ResourceListing readResourceListing(String input, MessageBuilder messages, List<AuthorizationValue> auths) {
        ResourceListing output = null;
        JsonNode jsonNode = null;
        try {
            if (input.startsWith("http")) {
                String json = RemoteUrl.urlToString(input, auths);
				json = applyJsonOverrides(json);
                jsonNode = Json.mapper().readTree(json);
            } else {
                jsonNode = Json.mapper().readTree(new File(input));
            }
            if (jsonNode.get("swaggerVersion") == null) {
                return null;
            }
			
            ResourceListingMigrator migrator = new ResourceListingMigrator();
            JsonNode transformed = migrator.migrate(messages, jsonNode);
            output = Json.mapper().convertValue(transformed, ResourceListing.class);
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log.error("failed to read resource listing", e);
        }
        return output;
    }
	
	@Override
	public ApiDeclaration readDeclaration(String input, MessageBuilder messages, List<AuthorizationValue> auths) {
        ApiDeclaration output = null;
        try {
            JsonNode jsonNode = null;
            if (input.startsWith("http")) {
                String json = RemoteUrl.urlToString(input, auths);
				json = applyJsonOverrides(json);
                jsonNode = Json.mapper().readTree(json);
            } else {
                jsonNode = Json.mapper().readTree(new java.io.File(input));
            }

            // this should be moved to a json patch
            if (jsonNode.isObject()) {
                ((ObjectNode) jsonNode).remove("authorizations");
            }

            ApiDeclarationMigrator migrator = new ApiDeclarationMigrator();
            JsonNode transformed = migrator.migrate(messages, jsonNode);
            output = Json.mapper().convertValue(transformed, ApiDeclaration.class);
        } catch (java.lang.IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            log.error("failed to read api declaration", e);
        }
        return output;
    }
	
	public String applyJsonOverrides(String data){
		HashMap<String,String> jsonReplaceMappings = new HashMap<>();

		log.info("Replacing JSON Types...");
		jsonReplaceMappings.put("\"Array\"", "\"array\"");
		
		for(Entry<String,String> entry : jsonReplaceMappings.entrySet()){
			log.info("..." + entry.getKey()+ " ==> " + entry.getValue());
			data = data.replaceAll(entry.getKey(), entry.getValue());
		}
		
		return data;
	}
}
