package com.prx.mercury.constant;

/**
 * Enum representing a key for the Mercury project.
 * This enum provides identification and descriptive details of the project.
 */
public enum MercuryKey {
    APPLICATION_ID("MCY", "Mercury");

    private final String projectId;
    private final String projectName;

    MercuryKey(String projectId, String projectName) {
        this.projectId = projectId;
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }
}
