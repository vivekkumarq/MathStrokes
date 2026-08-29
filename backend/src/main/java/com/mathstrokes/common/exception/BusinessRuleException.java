package com.mathstrokes.common.exception;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleException(ErrorCode code, String message) {
        super(code, message);
    }
}
