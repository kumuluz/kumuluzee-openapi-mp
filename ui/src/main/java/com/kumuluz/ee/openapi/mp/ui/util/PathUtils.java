package com.kumuluz.ee.openapi.mp.ui.util;

/**
 * Utility class to aid path normalization like removing of trailing slashes and so.
 *
 * @author Michal Vrsansky
 */
public class PathUtils {

    private PathUtils() {
    }

    /**
     * Add leading '/' if missing and removes trailing '/'.
     *
     * @param pathToNormalize path to be normalized
     * @return corrected path
     */
    public static String removeTrailingAndAddLeadingSlash(String pathToNormalize) {
        if (pathToNormalize == null || pathToNormalize.isEmpty()) {
            return pathToNormalize;
        }

        String normalizedPath = pathToNormalize;

        normalizedPath = removeTrailingSlashes(normalizedPath);

        if (!normalizedPath.startsWith("/")) {
            normalizedPath += "/";
        }

        return normalizedPath;
    }

    /**
     * Removes trailing '/'.
     *
     * @param pathToNormalize path to be normalized
     * @return corrected path
     */
    public static String removeTrailingSlashes(String pathToNormalize) {
        if (pathToNormalize == null || pathToNormalize.isEmpty()) {
            return pathToNormalize;
        }

        String normalizedPath = pathToNormalize;

        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        return normalizedPath;
    }
}
