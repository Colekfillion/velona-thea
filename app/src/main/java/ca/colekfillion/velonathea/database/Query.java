package ca.colekfillion.velonathea.database;

import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class Query {
    String query;

    public Query(Query.Builder builder) {
        this.query = builder.builtQuery;
    }

    public String getQuery() {
        return query;
    }

    public static class Builder {
        private String baseTable;
        private Set<Pair<String, String>> selectColumns = new LinkedHashSet<>();
        private String builtQuery;
        private Set<String> joins = new LinkedHashSet<>();
        private ArrayList<String> whereClauses = new ArrayList<>();
        private String groupBy;
        private Set<Query.Builder> intersects = new LinkedHashSet<>();
        private int havingCount = 0;
        private Set<Pair<String, String>> orderBy = new LinkedHashSet<>();

        public Builder select(String columnName, String tableName) {
            selectColumns.add(new Pair<>(columnName, tableName));
            return this;
        }

        public Builder from(String tableName) {
            baseTable = tableName;
            return this;
        }

        public Builder join(String baseTable, String baseColumn, String targetTable, String targetColumn) {
            joins.add("JOIN " + targetTable + " ON " + baseTable + "." + baseColumn + " = " + targetTable + "." + targetColumn + " ");
            return this;
        }

//        public Builder leftJoin(String table1, String column1, String table2, String column2) {
//            joins.add("LEFT JOIN ")
//        }

        public Builder where(String tableName, String columnName, int numValues, boolean like, boolean include, boolean or) {
            String clause = "";
            for (int i = 0; i < numValues; i++) {
                clause += tableName + "." + columnName + " ";
                if (!include && !like) {
                    clause += "!";
                } else if (!include) {
                    clause += "NOT ";
                }
                if (like) {
                    clause += "LIKE ";
                } else {
                    clause += "= ";
                }
                clause += "? ";
                if (or) {
                    clause += "OR ";
                } else {
                    clause += "AND ";
                }
            }
            clause = clause.trim();
            clause = clause.substring(0, clause.length() - 3);
            if (clause.endsWith("A")) {
                clause = clause.substring(0, clause.length() - 1);
            }
            whereClauses.add(clause);
            return this;
        }

        public Builder whereIn(String tableName, String columnName, int numValues, boolean include) {
            String clause = tableName + "." + columnName + " ";
            if (!include) {
                clause += "NOT ";
            }
            clause += "IN (";
            for (int i = 0; i < numValues; i++) {
                clause += "?, ";
            }
            clause = clause.substring(0, clause.length() - 2);
            clause += ")";
            whereClauses.add(clause);
            return this;
        }

        public Builder whereIn(String tableName, String columnName, Query.Builder qb, boolean include) {
            String clause = tableName + "." + columnName + " ";
            if (!include) {
                clause += "NOT ";
            }
            clause += "IN (" + qb.build().getQuery() + ")";
            whereClauses.add(clause);
            return this;
        }

        public Builder groupBy(String tableName, String columnName) {
            groupBy = tableName + "_" + columnName;
            return this;
        }

        public Builder orderBy(String tableName, String columnName) {
            orderBy.add(new Pair<>(tableName, columnName));
            return this;
        }

        public Builder orderBy(String function) {
            orderBy.add(new Pair<>(function, null));
            return this;
        }

        public Builder having(int target) {
            havingCount = target;
            return this;
        }

        public Builder intersect(Query.Builder qb) {
            intersects.add(qb);
            return this;
        }

        private void validate(Query query) {
            if (baseTable == null) {
                throw new IllegalStateException("Query must have a base table");
            }
        }

        public Query build() {
            builtQuery = "SELECT ";
            for (Pair<String, String> pair : selectColumns) {
                builtQuery += pair.first + "." + pair.second + " AS " + pair.first + "_" + pair.second + ", ";
            }
            builtQuery = builtQuery.substring(0, builtQuery.length() - 2) + " ";

            builtQuery += "FROM " + baseTable + " ";

            if (joins.size() > 0) {
                for (String join : joins) {
                    builtQuery += join;
                }
            }

            if (whereClauses.size() > 0) {
                builtQuery += "WHERE ";
                for (String clause : whereClauses) {
                    builtQuery += "(" + clause + ") AND ";
                }
                builtQuery = builtQuery.substring(0, builtQuery.length() - 4);
            }

            if (intersects.size() > 0) {
                builtQuery = "SELECT * FROM (" + builtQuery;
                for (Builder qb : intersects) {
                    builtQuery += " INTERSECT " + qb.build().getQuery() + " ";
                }
                builtQuery += " ) ";
            }

            if (groupBy != null) {
                builtQuery += "GROUP BY " + groupBy + " ";
            }

            if (intersects.size() > 0) {
                builtQuery += "HAVING COUNT(*) >= " + "1" + " ";
            }

            if (orderBy.size() > 0) {
                builtQuery += "ORDER BY ";
                for (Pair<String, String> p : orderBy) {
                    if (p.second != null) {
                        builtQuery += p.first + "_" + p.second + ", ";
                    } else {
                        builtQuery += p.first + ", ";
                    }
                }
                builtQuery = builtQuery.substring(0, builtQuery.length() - 2) + " ";
            }


            Query query = new Query(this);
            validate(query);
            return query;
        }
    }
}
