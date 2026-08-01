package com.erp.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;

/**
 * Tenant interceptor that rejects every schema-qualified table before tenant processing.
 */
public class FailClosedTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    /**
     * Creates a fail-closed tenant interceptor.
     *
     * @param tenantLineHandler tenant line handler
     */
    public FailClosedTenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
        super(tenantLineHandler);
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        rejectSchemaQualifiedTables(select);
        super.processSelect(select, index, sql, obj);
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        rejectSchemaQualifiedTables(insert);
        super.processInsert(insert, index, sql, obj);
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        rejectSchemaQualifiedTables(update);
        super.processUpdate(update, index, sql, obj);
    }

    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        rejectSchemaQualifiedTables(delete);
        super.processDelete(delete, index, sql, obj);
    }

    private void rejectSchemaQualifiedTables(Statement statement) {
        for (String tableName : new TablesNamesFinder().getTableList(statement)) {
            if (tableName != null && tableName.contains(".")) {
                throw new IllegalStateException(
                        "Schema-qualified table is not allowed in tenant SQL: " + tableName);
            }
        }
    }
}
