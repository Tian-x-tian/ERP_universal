package com.erp.saas.control.config;

import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.*;

public class SaasControlCatalogSchemaValidator {
    @FunctionalInterface
    interface SchemaReader { Map<String, SaasControlCatalogSchemaManifest.Table> read(); }

    private final SchemaReader reader;

    public SaasControlCatalogSchemaValidator(DataSource dataSource) {
        this(() -> readSchema(new JdbcTemplate(dataSource)));
    }

    SaasControlCatalogSchemaValidator(SchemaReader reader) { this.reader = reader; }

    public void validate() {
        Map<String, SaasControlCatalogSchemaManifest.Table> expected = SaasControlCatalogSchemaManifest.tables();
        Map<String, SaasControlCatalogSchemaManifest.Table> actual = reader.read();
        for (var entry : expected.entrySet()) {
            SaasControlCatalogSchemaManifest.Table normalizedExpected = normalize(entry.getValue());
            SaasControlCatalogSchemaManifest.Table normalizedActual = normalize(actual.get(entry.getKey()));
            if (!normalizedExpected.equals(normalizedActual)) {
                throw new IllegalStateException("Incompatible SaaS control schema: " + entry.getKey()
                        + " (" + mismatch(normalizedExpected, normalizedActual) + ")");
            }
        }
    }

    private static Map<String, SaasControlCatalogSchemaManifest.Table> readSchema(JdbcTemplate jdbc) {
        LinkedHashMap<String, SaasControlCatalogSchemaManifest.Table> result = new LinkedHashMap<>();
        for (String tableName : SaasControlCatalogSchemaManifest.tables().keySet()) {
            List<Map<String, Object>> tableRows = jdbc.queryForList(
                    "SELECT ENGINE, TABLE_COLLATION FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    tableName);
            if (tableRows.size() != 1) { continue; }
            Map<String, Object> tableRow = tableRows.get(0);
            LinkedHashMap<String, SaasControlCatalogSchemaManifest.Column> columns = new LinkedHashMap<>();
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, GENERATION_EXPRESSION, COLLATION_NAME, EXTRA "
                            + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                    tableName)) {
                String name = text(row, "COLUMN_NAME");
                String generation = nullableNonBlankText(row.get("GENERATION_EXPRESSION"));
                columns.put(name, new SaasControlCatalogSchemaManifest.Column(name,
                        text(row, "COLUMN_TYPE"), "YES".equalsIgnoreCase(text(row, "IS_NULLABLE")),
                        nullableText(row.get("COLUMN_DEFAULT")), generation, nullableText(row.get("COLLATION_NAME")),
                        generation != null && text(row, "EXTRA").toUpperCase(Locale.ROOT).contains("STORED")));
            }
            LinkedHashMap<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART, COLLATION "
                            + "FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY INDEX_NAME, SEQ_IN_INDEX",
                    tableName)) {
                grouped.computeIfAbsent(text(row, "INDEX_NAME"), ignored -> new ArrayList<>()).add(row);
            }
            LinkedHashMap<String, SaasControlCatalogSchemaManifest.Index> indexes = new LinkedHashMap<>();
            grouped.forEach((name, rows) -> indexes.put(name, new SaasControlCatalogSchemaManifest.Index(name,
                    number(rows.get(0).get("NON_UNIQUE")) == 0,
                    rows.stream().map(row -> new SaasControlCatalogSchemaManifest.IndexColumn(
                            text(row, "COLUMN_NAME"), nullableNumber(row.get("SUB_PART")),
                            nullableText(row.get("COLLATION")))).toList())));
            LinkedHashMap<String, SaasControlCatalogSchemaManifest.Check> checks = new LinkedHashMap<>();
            for (Map<String, Object> row : jdbc.queryForList(
                    "SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE, tc.ENFORCED "
                            + "FROM information_schema.TABLE_CONSTRAINTS tc "
                            + "JOIN information_schema.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA "
                            + "AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME WHERE tc.TABLE_SCHEMA = DATABASE() "
                            + "AND tc.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'CHECK' ORDER BY tc.CONSTRAINT_NAME",
                    tableName)) {
                checks.put(text(row, "CONSTRAINT_NAME"), new SaasControlCatalogSchemaManifest.Check(
                        text(row, "CHECK_CLAUSE"), "YES".equalsIgnoreCase(text(row, "ENFORCED"))));
            }
            result.put(tableName, new SaasControlCatalogSchemaManifest.Table(tableName,
                    text(tableRow, "ENGINE"), text(tableRow, "TABLE_COLLATION"),
                    columns, indexes, checks));
        }
        return result;
    }

    private static SaasControlCatalogSchemaManifest.Table normalize(SaasControlCatalogSchemaManifest.Table table) {
        if (table == null) { return null; }
        LinkedHashMap<String, SaasControlCatalogSchemaManifest.Column> columns = new LinkedHashMap<>();
        table.columns().forEach((name, value) -> columns.put(name.toLowerCase(Locale.ROOT),
                new SaasControlCatalogSchemaManifest.Column(name.toLowerCase(Locale.ROOT), normalizeType(value.type()),
                        value.nullable(), normalizeDefault(value.defaultValue()), normalizeExpression(value.generationExpression()),
                        lower(value.collation()), value.storedGenerated())));
        LinkedHashMap<String, SaasControlCatalogSchemaManifest.Index> indexes = new LinkedHashMap<>();
        table.indexes().forEach((name, value) -> indexes.put(name.toLowerCase(Locale.ROOT),
                new SaasControlCatalogSchemaManifest.Index(name.toLowerCase(Locale.ROOT), value.unique(),
                        value.columns().stream().map(column -> new SaasControlCatalogSchemaManifest.IndexColumn(
                                lower(column.name()), column.prefixLength(), upper(column.order()))).toList())));
        LinkedHashMap<String, SaasControlCatalogSchemaManifest.Check> checks = new LinkedHashMap<>();
        table.checks().forEach((name, value) -> checks.put(name.toLowerCase(Locale.ROOT),
                new SaasControlCatalogSchemaManifest.Check(normalizeExpression(value.expression()), value.enforced())));
        return new SaasControlCatalogSchemaManifest.Table(lower(table.name()), lower(table.engine()),
                lower(table.collation()), columns, indexes, checks);
    }

    private static String normalizeExpression(String value) {
        if (value == null || value.isBlank()) { return null; }
        String source = value.replace("\\'", "'");
        StringBuilder canonical = new StringBuilder(source.length());
        boolean quoted = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (quoted) {
                canonical.append(current);
                if (current == '\'' && i + 1 < source.length() && source.charAt(i + 1) == '\'') {
                    canonical.append(source.charAt(++i));
                } else if (current == '\'') {
                    quoted = false;
                }
                continue;
            }
            if (current == '\'') {
                quoted = true;
                canonical.append(current);
            } else if (current == '`' || Character.isWhitespace(current)) {
                continue;
            } else if (current == '_' && isCharsetIntroducer(source, i)) {
                while (i + 1 < source.length() && isIdentifierPart(source.charAt(i + 1))) { i++; }
            } else {
                canonical.append(Character.toUpperCase(current));
            }
        }
        String normalized = canonical.toString();
        while (isWrapped(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }
    private static boolean isCharsetIntroducer(String value, int offset) {
        int cursor = offset + 1;
        while (cursor < value.length() && isIdentifierPart(value.charAt(cursor))) { cursor++; }
        return cursor > offset + 1 && cursor < value.length() && value.charAt(cursor) == '\'';
    }
    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }
    private static boolean isWrapped(String value) {
        if (!value.startsWith("(") || !value.endsWith(")")) { return false; }
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'') {
                if (quoted && i + 1 < value.length() && value.charAt(i + 1) == '\'') { i++; continue; }
                quoted = !quoted;
                continue;
            }
            if (quoted) { continue; }
            if (current == '(') { depth++; }
            if (current == ')' && --depth == 0 && i < value.length() - 1) { return false; }
        }
        return depth == 0;
    }
    private static String normalizeDefault(String value) {
        if (value == null) { return null; }
        String normalized = value.trim();
        return normalized.startsWith("'") && normalized.endsWith("'")
                ? normalized.substring(1, normalized.length() - 1) : normalized;
    }
    private static String normalizeType(String value) {
        String normalized = lower(value);
        if (normalized == null) { return null; }
        return normalized.replaceFirst("^(smallint|mediumint|int|integer|bigint)\\(\\d+\\)", "$1");
    }
    private static String mismatch(SaasControlCatalogSchemaManifest.Table expected,
            SaasControlCatalogSchemaManifest.Table actual) {
        if (actual == null) { return "missing table"; }
        if (!Objects.equals(expected.engine(), actual.engine())) { return "engine"; }
        if (!Objects.equals(expected.collation(), actual.collation())) { return "table collation"; }
        if (!Objects.equals(expected.columns(), actual.columns())) { return "columns"; }
        if (!Objects.equals(expected.indexes(), actual.indexes())) { return "indexes"; }
        if (!Objects.equals(expected.checks(), actual.checks())) { return "checks"; }
        return "metadata";
    }
    private static String text(Map<String, Object> row, String key) { return String.valueOf(row.get(key)); }
    private static String nullableText(Object value) { return value == null ? null : String.valueOf(value); }
    private static String nullableNonBlankText(Object value) {
        String text = nullableText(value);
        return text == null || text.isBlank() ? null : text;
    }
    private static int number(Object value) { return value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value)); }
    private static Integer nullableNumber(Object value) { return value == null ? null : number(value); }
    private static String lower(String value) { return value == null ? null : value.toLowerCase(Locale.ROOT); }
    private static String upper(String value) { return value == null ? null : value.toUpperCase(Locale.ROOT); }
}
