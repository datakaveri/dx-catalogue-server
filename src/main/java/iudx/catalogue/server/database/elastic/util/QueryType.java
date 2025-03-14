package iudx.catalogue.server.database.elastic.util;

public enum QueryType {
  MATCH_ALL,
  MATCH,
  TERM,
  TERMS,
  SHOULD,
  BOOL,
  RANGE,
  WILDCARD,
  GEO_SHAPE,
  GEO_BOUNDING_BOX,
  TEXT,
  SCRIPT_SCORE,
  QUERY_STRING
}
