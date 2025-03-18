package iudx.catalogue.server.database.postgres.models;

import java.util.List;

public interface QueryModel {
  String toSQL();
  List<Object> getQueryParams();
}
