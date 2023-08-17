package ca.colekfillion.velonathea.database;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ca.colekfillion.velonathea.pojo.Constants;

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
        private Set<Pair<String, String>> selectColumns = new HashSet<>();
        private String builtQuery;
        private Set<String> joins = new LinkedHashSet<>();
        private ArrayList<String> whereClauses = new ArrayList<>();
        private String groupBy;
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

        public Builder join(String table1, String column1, String table2, String column2) {
            joins.add("JOIN " + table2 + " ON " + table1 + "." + column1 + " = " + table2 + "." + column2 + " ");
            return this;
        }

//        public Builder leftJoin(String table1, String column1, String table2, String column2) {
//            joins.add("LEFT JOIN ")
//        }

        public Builder whereCondition(String tableName, String columnName, ArrayList<String> values, boolean like, boolean matchAll, boolean exclude) {
            String clause = "";
            String valueComparer = like ? "LIKE " : "= ";
            if (exclude && like) {
                valueComparer = "NOT " + valueComparer;
            } else if (exclude) {
                valueComparer = "!" + valueComparer;
            }
            String joiner = matchAll ? "AND " : "OR ";
            for (String value : values) {
                clause += tableName + "." + columnName + " " + valueComparer + "\"";
                clause += like ? "%" + value + "%\" " : value + "\" ";
                clause += joiner;
            }
            clause = clause.substring(0, clause.lastIndexOf(joiner) - 1);
            whereClauses.add(clause);
            return this;
        }

        public Builder whereIn(String tableName, String columnName, ArrayList<String> values, boolean exclude, int havingCount) {
            this.havingCount += havingCount;
            String clause = tableName + "." + columnName + " ";
            if (exclude) {
                clause += "NOT ";
            }
            clause += "IN (";
            for (String value : values) {
                clause += value + ", ";
            }
            clause = clause.substring(0, clause.length() - 2);
            clause += ") ";
            whereClauses.add(clause);
            return this;
        }

        public Builder groupBy(String tableName, String columnName) {
            groupBy = tableName + "." + columnName;
            return this;
        }

        public Builder having(int target) {
            havingCount = target;
            return this;
        }

        public Builder orderByRandom() {
            orderBy.add(new Pair<>("RANDOM()", null));
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
                builtQuery += pair.second + "." + pair.first + " AS " + pair.second + "_" + pair.first + ", ";
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
                for (String whereClause : whereClauses) {
                    builtQuery += "(" + whereClause + ") AND ";
                }
                builtQuery = builtQuery.substring(0, builtQuery.length() - 5) + " ";
            }

            if (groupBy != null) {
                builtQuery += "GROUP BY (" + groupBy + ") ";
                if (havingCount > 0) {
                    builtQuery += " HAVING COUNT(*) >= " + havingCount + " ";
                }
            }

            if (orderBy.size() > 0) {
                builtQuery += "ORDER BY ";
                for (Pair<String, String> orderByPair : orderBy) {
                    builtQuery += orderByPair.first + " ";
                    if (!Constants.isStringEmpty(orderByPair.second)) {
                        builtQuery += orderByPair.second;
                    }
                    builtQuery += ", ";
                }
                builtQuery = builtQuery.substring(0, builtQuery.length() - 2) + " ";
            }

            Query query = new Query(this);
            validate(query);
            return query;
        }
    }
}
