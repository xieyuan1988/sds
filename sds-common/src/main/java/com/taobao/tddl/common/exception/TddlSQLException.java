package com.taobao.tddl.common.exception;

import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Tddl nestabled {@link java.sql.SQLException}
 *
 * @author jianghang 2013-10-24 下午2:54:56
 * @since 5.0.0
 */
public class TddlSQLException extends SQLException {

    private static final long serialVersionUID = -4558269080286141706L;

    public TddlSQLException(SQLException cause) {
        this(null, cause);
    }

    public TddlSQLException(String message, SQLException cause) {
        super(message);
        if (cause == null) {
            throw new IllegalArgumentException("必须填入SQLException");
        }
        this.cause = cause;
    }

    protected final SQLException cause;

    /**
     * {@inheritDoc}
     */
    public SQLException getCause() {
        return cause;
    }

    public String getMessage() {
        if (super.getMessage() != null) {
            return super.getMessage();
        } else if (cause != null) {
            return cause.toString();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void printPartialStackTrace(PrintWriter out) {
        super.printStackTrace(out);
    }

}
