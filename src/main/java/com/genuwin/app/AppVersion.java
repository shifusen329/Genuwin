package com.genuwin.app;

/**
 * Centralized version management for the Genuwin app
 * This class provides a single source of truth for version information
 * that can be easily updated and used throughout the application.
 */
public class AppVersion {
    
    /**
     * The current version of the application
     * Format: v[major].[minor].[patch]-[stage]
     */
    public static final String VERSION = "0.1.3-beta";
    
    /**
     * Version code for internal use (should increment with each release)
     */
    public static final int VERSION_CODE = 5;
    
    /**
     * Build type identifier
     */
    public static final String BUILD_TYPE = "beta";
    
    /**
     * Get the formatted version string for display
     * @return The version string (e.g., "v0.0.1-alpha")
     */
    public static String getVersionString() {
        return VERSION;
    }
    
    /**
     * Get the version code
     * @return The version code as integer
     */
    public static int getVersionCode() {
        return VERSION_CODE;
    }
    
    /**
     * Get the build type
     * @return The build type (e.g., "alpha", "beta", "release")
     */
    public static String getBuildType() {
        return BUILD_TYPE;
    }
    
    /**
     * Check if this is a pre-release version
     * @return true if this is alpha or beta, false for release
     */
    public static boolean isPreRelease() {
        return BUILD_TYPE.equals("alpha") || BUILD_TYPE.equals("beta");
    }
}
