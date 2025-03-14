package iudx.catalogue.server.database.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.StringBuilder;


public final class Summarizer {

  private static String jsonArtifactMatcher = "[\n:{},\"\\[\\]]";
  private static String spaceMatcher = "\\s+";
  /**
   * Summarizer - Summarize a doument for Elastic text search.
   *
   *
   * @param doc is a JsonObject
   * @TODO XPack Security
   */

  public static String summarize(JsonObject doc) {
  
    StringBuilder sb = new StringBuilder(); 
  

    /* name and label */
    if (doc.containsKey("name")) {
      sb.append(doc.getString("name"));
      sb.append(" ");
    }
    if (doc.containsKey("label")) {
      sb.append(doc.getString("label"));
      sb.append(" ");
    }

    /* Tags */
    if (doc.containsKey("tags")) {
      JsonArray tags = doc.getJsonArray("tags");
      for (int i = 0; i < tags.size(); i++) {
        sb.append(tags.getString(i) + " ");
      }
    }

    /* Description */
    if (doc.containsKey("description")) {
      sb.append(doc.getString("description"));
      sb.append(" ");
    }

    /* Data Descriptor */
    if (doc.containsKey("descriptor")) {
      String descriptor = doc.getJsonObject("descriptor").toString();
      descriptor = descriptor.replaceAll(jsonArtifactMatcher, " ");
      descriptor = descriptor.replaceAll(spaceMatcher, " ");
      sb.append(descriptor);
      sb.append(" ");
    }

    return sb.toString();
  }

}
