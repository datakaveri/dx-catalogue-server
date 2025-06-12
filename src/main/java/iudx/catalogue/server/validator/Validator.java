package iudx.catalogue.server.validator;

import static iudx.catalogue.server.util.Constants.RESULTS;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Validator {

  public static final Logger LOGGER = LogManager.getLogger(Validator.class);
  private static final String PKGBASE;

  static {
    final String pkgName = Validator.class.getPackage().getName();
    PKGBASE = '/' + pkgName.replace(".", "/");
  }

  private final JsonSchema schema;

  /**
   * Creates a new instance of Validator that can validate JSON objects against a given JSON schema.
   *
   * @param schemaPath a String that represents the path of the JSON schema file
   * @throws IOException if there is an error reading the schema file
   */
  public Validator(String schemaPath) throws IOException, ProcessingException {
    final JsonNode schemaNode = loadResource(schemaPath);
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    schema = factory.getJsonSchema(schemaNode);
  }

  /**
   * Load one resource from the current package as a {@link JsonNode}.
   *
   * @param name name of the resource (<b>MUST</b> start with {@code /}
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadResource(final String name) throws IOException {
    return JsonLoader.fromResource(PKGBASE + name);
  }

  /**
   * Load one resource from a string {@link JsonNode}.
   *
   * @param obj Json encoded object
   * @return a JSON document
   * @throws IOException resource not found
   */
  public static JsonNode loadString(final String obj) throws IOException {
    return JsonLoader.fromString(obj);
  }

  /**
   * Check validity of json encoded string.
   *
   * @param obj Json encoded string object
   * @return isValid boolean
   */
  public Future<String> validateSearchCriteria(String obj) {
    Promise<String> promise = Promise.promise();
    boolean isValid;
    List<String> schemaErrorList = new ArrayList<>();
    try {
      JsonNode jsonobj = loadString(obj);
      ProcessingReport report = schema.validate(jsonobj);
      report.forEach(
          x -> {
            if (x.getLogLevel().toString().equalsIgnoreCase("error")) {
              LOGGER.error(x.getMessage());
              schemaErrorList.add(x.getMessage());
            }
          });
      isValid = report.isSuccess();

    } catch (IOException | ProcessingException e) {
      isValid = false;
      schemaErrorList.add(e.getMessage());
    }

    if (isValid) {
      promise.complete();
    } else {
      promise.fail(schemaErrorList.toString());
    }
    return promise.future();
  }

  /**
   * Validates a JSON-encoded string against the loaded schema.
   *
   * <p>This method performs full JSON schema validation and collects all validation errors.
   * It logs each error and builds a structured error response that includes:
   * <ul>
   *   <li>A high-level message listing all the invalid fields</li>
   *   <li>A detailed list of human-readable validation messages for each field</li>
   * </ul>
   *
   * <p>If validation passes, the returned {@link Future} is completed. If validation fails,
   * the future is failed with a JSON-encoded {@link JsonObject} that contains:
   * <ul>
   *   <li><b>detail</b>: A comma-separated list of invalid fields</li>
   *   <li><b>results</b>: An array of field-specific validation messages</li>
   * </ul>
   *
   * @param obj A JSON-encoded string to validate
   * @return a {@link Future} that is:
   *         <ul>
   *           <li>completed if the input is valid</li>
   *           <li>failed with a structured error message if the input is invalid</li>
   *         </ul>
   */
  public Future<String> validate(String obj) {
    Promise<String> promise = Promise.promise();
    List<String> schemaErrorList = new ArrayList<>();
    Set<String> errorFields = new LinkedHashSet<>(); // Preserve order

    try {
      JsonNode jsonObj = loadString(obj);
      ProcessingReport report = schema.validate(jsonObj);

      report.forEach(msg -> {
        if ("error".equalsIgnoreCase(msg.getLogLevel().toString())) {
          JsonNode msgNode = msg.asJson();

          String pointer = "";
          if (msgNode.has("instance") && msgNode.get("instance").has("pointer")) {
            pointer = msgNode.get("instance").get("pointer").asText(); // e.g., "/accessPolicy"
          }

          String errorMessage =
              msgNode.has("message") ? msgNode.get("message").asText() : "Validation failed";

          // Beautify pointer
          //String field = pointer.isEmpty() ? "Unknown field" : pointer.substring(1); // remove
          String field;
          if (!pointer.isEmpty()) {
            field = pointer.substring(1); // e.g., "/mediaURL" => "mediaURL"
          } else if (errorMessage.contains("missing required properties")) {
            Matcher matcher = Pattern.compile("\\[\"(.*?)\"\\]").matcher(errorMessage);
            if (matcher.find()) {
              field = matcher.group(1);
            } else {
              field = "Unknown field";
            }
          } else {
            field = "Unknown field";
          }
          // leading slash
          errorFields.add(field);

          String userMessage = String.format("Field \"%s\": %s", field, errorMessage);
          LOGGER.error(userMessage);
          schemaErrorList.add(userMessage);
        }
      });

      if (report.isSuccess()) {
        promise.complete();
      } else {
        String fieldListStr = String.join(", ", errorFields);
        String detailMessage = "Schema validation failed for fields: " + fieldListStr;

        JsonObject errorResponse = new JsonObject()
            .put("detail", detailMessage)
            .put(RESULTS, parseValidationMessages(schemaErrorList.toString()));

        promise.fail(errorResponse.encode());
      }

    } catch (IOException | ProcessingException e) {
      LOGGER.error("Exception during validation", e);
      promise.fail(e.getMessage());
    }

    return promise.future();
  }

  private JsonArray parseValidationMessages(String rawMessage) {
    JsonArray userMessages = new JsonArray();

    String[] rawMessages = rawMessage.replaceAll("^\\[|\\]$", "")
        .split(",(?=\\s*Field)");

    for (String msg : rawMessages) {
      msg = msg.trim().replaceAll("\\\\\"", "\"").replaceAll("\\\\", "");

      String fieldName = null;
      Matcher fieldMatcher = Pattern.compile("Field\\s+\"([^\"]+)\"").matcher(msg);
      if (fieldMatcher.find()) {
        fieldName = fieldMatcher.group(1);
        // Remove repeated "Field \"fieldName\":" prefix from message if present
        msg = msg.replaceFirst("Field\\s+\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*", "").trim();
      }

      if (msg.contains("ECMA 262 regex")) {
        if (msg.contains(
            "^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$")) {
          userMessages.add(
              String.format("Field \"%s\": does not match pattern for UUID.", fieldName));

        } else if (msg.contains("^(RESTRICTED|OPEN|PRIVATE)$")) {
          userMessages.add(String.format(
              "Field \"%s\": must be one of: \"RESTRICTED\", \"OPEN\", or \"PRIVATE\".",
              fieldName));

        } else {
          userMessages.add(
              String.format("Field \"%s\": does not match required format.", fieldName));
        }

      } else if (msg.contains("missing required properties")) {
        Matcher matcher = Pattern.compile("\\[\"(.*?)\"\\]").matcher(msg);
        if (matcher.find()) {
          userMessages.add("Field \"" + matcher.group(1) + "\": is required.");
        } else {
          userMessages.add("A required field is missing.");
        }

      } else if (msg.contains("not found in enum")) {
        Matcher matcher =
            Pattern.compile("instance value \\(\"(.*?)\"\\).*?values: \\[(.*?)\\]").matcher(msg);
        if (matcher.find()) {
          String rawEnumValues = matcher.group(2);

          // Quote all values including empty string
          List<String> valuesList = new ArrayList<>();
          for (String val : rawEnumValues.split(",")) {
            val = val.trim().replaceAll("^\"|\"$", "");
            valuesList.add(val.isEmpty() ? "\"\"" : "\"" + val + "\"");
          }

          String formattedEnumValues = String.join(", ", valuesList);
          userMessages.add(
              String.format("Field \"%s\": must be one of: %s.", fieldName, formattedEnumValues));

        } else {
          userMessages.add(String.format("Field \"%s\": contains an invalid value.", fieldName));
        }

      } else if (msg.matches(
          ".*instance type \\(.*\\) does not match any allowed primitive type.*")) {
        // Handle type mismatch messages
        Matcher matcher =
            Pattern.compile("instance type \\((.*?)\\).*allowed: \\[(.*?)\\]").matcher(msg);
        if (matcher.find()) {
          String foundType = matcher.group(1);
          String allowedTypes =
              matcher.group(2).replaceAll("\"", ""); // Remove quotes from allowed types
          userMessages.add(String.format("Field \"%s\": expected type %s, but found %s.", fieldName,
              allowedTypes, foundType));
        } else {
          userMessages.add(String.format("Field \"%s\": has a type mismatch.", fieldName));
        }
      } else if (msg.matches(".*is too long \\(length: \\d+, maximum allowed: \\d+\\).*")) {
        Matcher matcher = Pattern.compile("length: (\\d+), maximum allowed: (\\d+)").matcher(msg);
        if (matcher.find()) {
          String actualLength = matcher.group(1);
          String maxLength = matcher.group(2);
          userMessages.add(String.format(
              "Field \"%s\": exceeds maximum allowed length of %s characters (provided: %s).",
              fieldName, maxLength, actualLength));
        } else {
          userMessages.add(
              String.format("Field \"%s\": exceeds maximum allowed length.", fieldName));
        }

      } else {
        userMessages.add(
            "Field " + (fieldName != null ? "\"" + fieldName + "\"" : "unknown") + ": " + msg);
      }
    }

    return userMessages;
  }


}
