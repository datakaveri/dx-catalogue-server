package iudx.catalogue.server.apiserver.stack.util;

public class StackConstants {
  public static final String DOC_ID = "_id";
  static String PATCH_QUERY =
      "{\n"
          + "  \"script\": {\n"
          + "    \"source\": \"ctx._source.links.add(params)\",\n"
          + "    \"lang\": \"painless\",\n"
          + "    \"params\": {\n"
          + "      \"rel\": \"$1\",\n"
          + "      \"href\": \"$2\"\n";

  static String TYPE = " \"type\": \"$1\"\n";
  static String TITLE = " \"title\": \"$1\"\n";

  static String CLOSED_QUERY = "    }\n" + "  }\n" + "}";
}
