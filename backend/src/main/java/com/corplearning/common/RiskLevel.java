package com.corplearning.common;

public enum RiskLevel {
    NONE, LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel fromSeverity(String severity) {
        if (severity == null) {
            return NONE;
        }
        switch (severity.toUpperCase()) {
            case "CRITICAL": return CRITICAL;
            case "HIGH": return HIGH;
            case "MEDIUM": return MEDIUM;
            case "LOW": return LOW;
            default: return NONE;
        }
    }

    public static RiskLevel max(RiskLevel a, RiskLevel b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
