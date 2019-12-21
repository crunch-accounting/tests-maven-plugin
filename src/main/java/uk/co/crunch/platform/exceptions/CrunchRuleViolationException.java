package uk.co.crunch.platform.exceptions;

public class CrunchRuleViolationException extends RuntimeException {
    public CrunchRuleViolationException(final String msg) {
        super(/* Extra visibility in Maven logs */ "CrunchRuleViolationException: " + msg);
    }
}
