package com.erp.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

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
        new QualificationRejectingTablesFinder().getTableList(statement);
    }

    private static final class QualificationRejectingTablesFinder extends TablesNamesFinder {

        @Override
        public void visit(Table table) {
            boolean hasDatabaseQualifier = table.getDatabase() != null
                    && StringUtils.hasText(table.getDatabase().getFullyQualifiedName());
            if (StringUtils.hasText(table.getSchemaName()) || hasDatabaseQualifier) {
                throw new IllegalStateException(
                        "Schema-qualified table is not allowed in tenant SQL: "
                                + table.getFullyQualifiedName());
            }
        }

        @Override
        public void visit(PlainSelect plainSelect) {
            super.visit(plainSelect);
            visitJoinOnExpressions(plainSelect.getJoins());
        }

        @Override
        public void visit(Insert insert) {
            super.visit(insert);
            visitWithItems(insert.getWithItemsList());
            visitExpressions(insert.getDuplicateUpdateExpressionList());
            visitExpressions(insert.getSetExpressionList());
        }

        @Override
        public void visit(Update update) {
            super.visit(update);
            visitWithItems(update.getWithItemsList());
            visitJoinOnExpressions(update.getStartJoins());
            visitJoinOnExpressions(update.getJoins());
        }

        @Override
        public void visit(Delete delete) {
            super.visit(delete);
            visitWithItems(delete.getWithItemsList());
            visitJoinOnExpressions(delete.getJoins());
        }

        private void visitJoinOnExpressions(List<Join> joins) {
            if (joins == null) {
                return;
            }
            for (Join join : joins) {
                visitExpressions(join.getOnExpressions());
            }
        }

        private void visitExpressions(Collection<? extends Expression> expressions) {
            if (expressions == null) {
                return;
            }
            for (Expression expression : expressions) {
                if (expression != null) {
                    expression.accept(this);
                }
            }
        }

        private void visitWithItems(List<WithItem> withItems) {
            if (withItems == null) {
                return;
            }
            for (WithItem withItem : withItems) {
                withItem.accept(this);
            }
        }
    }
}
