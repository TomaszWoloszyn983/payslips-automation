package com.tomazwoloszyn.javauipath.dto;

import java.util.Map;

public class PayslipResult {
    private String fileName;
    private Map<String, String> fields;

    public PayslipResult() {}

    public PayslipResult(String fileName, Map<String, String> fields) {
        this.fileName = fileName;
        this.fields = fields;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }
}
