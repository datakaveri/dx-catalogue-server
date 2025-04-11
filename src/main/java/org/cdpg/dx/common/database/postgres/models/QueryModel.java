package org.cdpg.dx.common.database.postgres.models;

import java.util.List;

public interface QueryModel {
  String toSQL();
  List<Object> getQueryParams();
}
