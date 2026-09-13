package com.tomazwoloszyn.javauipath;

/**
 * Stores information on the document classification type
 * associated ti the document type Id.
 * Provides the human-readable classification name
 */
public enum PayslipDocumentType {

    PAPER_PAYSLIP(
            "00aa8760-ac9b-f111-9b33-002248a056d7",
            "Paper Payslip",
            "c2b6f168-939f-f111-9b33-000d3ab67660"
    ),

    ELECTRONIC_PAYSLIP(
            "41309319-4f8a-f111-b337-0022489f5fe3",
            "Electronic Payslip",
            "05d55b81-939f-f111-9b33-000d3ab67660"
    ),

    IT_PAYSLIP(
            "97f7e520-4f8a-f111-b337-0022489f5fe3",
            "IT Payslip",
            "40a5b5fe-939f-f111-9b33-000d3ab67660"
    );

    private final String documentTypeId;
    private final String displayName;
    private final String extractorId;

    PayslipDocumentType(
            String documentTypeId,
            String displayName,
            String extractorId) {

        this.documentTypeId = documentTypeId;
        this.displayName = displayName;
        this.extractorId = extractorId;
    }

    public String getDocumentTypeId() {
        return documentTypeId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtractorId() {
        return extractorId;
    }

    public static PayslipDocumentType fromId(String id) {
        for (PayslipDocumentType type : values()) {
            if (type.documentTypeId.equals(id)) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown document type ID: " + id
        );
    }
}
