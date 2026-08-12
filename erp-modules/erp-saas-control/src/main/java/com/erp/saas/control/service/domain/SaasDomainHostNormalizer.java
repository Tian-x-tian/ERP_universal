package com.erp.saas.control.service.domain;

import org.springframework.stereotype.Component;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SaasDomainHostNormalizer {
    private static final int MAX_HOST_LENGTH = 253;
    private static final int MAX_LABEL_LENGTH = 63;

    public String normalize(String rawHost) {
        if (rawHost == null) {
            throw invalid();
        }
        String value = rawHost.trim();
        if (value.isEmpty() || containsForbiddenSyntax(value)) {
            throw invalid();
        }
        value = removePort(value).replace('\u3002', '.').replace('\uFF0E', '.').replace('\uFF61', '.');
        if (value.endsWith(".")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty() || value.endsWith(".")) {
            throw invalid();
        }
        String[] labels = value.split("\\.", -1);
        List<String> asciiLabels = new ArrayList<>(labels.length);
        try {
            for (String label : labels) {
                if (label.isEmpty()) {
                    throw invalid();
                }
                String ascii = IDN.toASCII(label, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
                if (ascii.isEmpty() || ascii.length() > MAX_LABEL_LENGTH) {
                    throw invalid();
                }
                asciiLabels.add(ascii);
            }
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        String normalized = String.join(".", asciiLabels);
        if (normalized.length() > MAX_HOST_LENGTH || isIpv4Literal(asciiLabels)) {
            throw invalid();
        }
        return normalized;
    }

    private static boolean containsForbiddenSyntax(String value) {
        if (value.contains("://") || value.indexOf('/') >= 0 || value.indexOf('?') >= 0
                || value.indexOf('#') >= 0 || value.indexOf('@') >= 0 || value.indexOf('*') >= 0
                || value.indexOf('[') >= 0 || value.indexOf(']') >= 0) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current) || Character.isWhitespace(current)) {
                return true;
            }
        }
        return false;
    }

    private static String removePort(String value) {
        int firstColon = value.indexOf(':');
        if (firstColon < 0) {
            return value;
        }
        if (firstColon != value.lastIndexOf(':') || firstColon == 0 || firstColon == value.length() - 1) {
            throw invalid();
        }
        String portText = value.substring(firstColon + 1);
        if (!portText.chars().allMatch(Character::isDigit)) {
            throw invalid();
        }
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                throw invalid();
            }
        } catch (NumberFormatException exception) {
            throw invalid();
        }
        return value.substring(0, firstColon);
    }

    private static boolean isIpv4Literal(List<String> labels) {
        return labels.size() == 4 && labels.stream().allMatch(label -> label.chars().allMatch(Character::isDigit));
    }

    private static SaasDomainException invalid() {
        return new SaasDomainException(SaasDomainException.ErrorCode.INVALID_HOST, "Invalid domain host");
    }
}
