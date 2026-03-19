package com.codebugs.payment.processor;

public record ProcessorResult(boolean success, String referenceId, String declineReason) {

    public static ProcessorResult approved(String referenceId) {
        return new ProcessorResult(true, referenceId, null);
    }

    public static ProcessorResult declined(String reason) {
        return new ProcessorResult(false, null, reason);
    }
}
