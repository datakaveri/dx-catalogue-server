package iudx.catalogue.server.rating.util;

public class Constants {

  public static final String USER_ID = "userID";
  public static final String ID = "id";
  public static final String PENDING = "pending";
  public static final String APPROVED = "approved";
  public static final String DENIED = "denied";
  public static final String RATING_ID = "ratingID";
  public static final String TYPE = "type";
  public static final String STATUS = "status";
  public static final String AUDIT_INFO_QUERY =
      "SELECT count(*) from $1 where userId='$2' and resourceid='$3'";
}
