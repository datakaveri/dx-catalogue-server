package iudx.catalogue.server.database.elastic.util;

public enum ResponseFilter {
  SOURCE_ONLY,          // Returns only the source
  DOC_ID_ONLY,          // Returns only the doc_id
  SOURCE_AND_DOC_ID,    // Returns both source and doc_id
  SOURCE_AND__ID,
  IDS_ONLY,          // Returns only extracted IDs
  AGGREGATIONS,
  SOURCE_WITHOUT_EMBEDDINGS
}

