package iudx.catalogue.server.database.cache.models;

import java.util.List;

public interface QueryModel {
  String toSQL();
  List<Object> getQueryParams();
}
