package com.schema;

import java.io.IOException;
import java.io.InputStream;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/*
 * Valiation Service for JSON schemas from the internal icd
 */
@Service
public class SchemaValidator {
  private final ResourceLoader resourceLoader;

  public SchemaValidator(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public boolean validateJson(InputStream schemaStream, JSONObject jsonNode) {
    boolean valid = false;
    try {
      JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
      Schema schema = SchemaLoader.load(rawSchema);
      schema.validate(jsonNode);
    } catch (Exception e) {
      System.out.println("Exception caught: " + e.getLocalizedMessage());
      return valid;
    }
    valid = true;
    return valid;
  }

  public InputStream getSchemaStream(String jsonPath) {
    String location = "classpath:" + jsonPath;
    Resource resource = resourceLoader.getResource(location);
    System.out.println("Loading schema at: " + location + " | Exists? " + resource.exists());
    try {
        return resource.exists() ? resource.getInputStream() : null;
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}

}

