package com.taobao.tddl.common.utils.extension;

/**
 * @author jianghang 2013-9-13 下午4:03:51
 */
public class ExtensionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -654893533794556357L;

    public ExtensionNotFoundException(String errorCode) {
        super(errorCode);
    }

    public ExtensionNotFoundException(String errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ExtensionNotFoundException(String errorCode, String errorDesc) {
        super(errorCode + ":" + errorDesc);
    }

    public ExtensionNotFoundException(String errorCode, String errorDesc, Throwable cause) {
        super(errorCode + ":" + errorDesc, cause);
    }

    public ExtensionNotFoundException(Throwable cause) {
        super(cause);
    }

    public Throwable fillInStackTrace() {
        return this;
    }

}
