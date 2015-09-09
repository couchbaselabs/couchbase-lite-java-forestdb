package com.couchbase.lite.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class NativeLibUtils {
    public static final String TAG = "Native";
    private static final Map<String, Boolean> LOADED_LIBRARIES = new HashMap<String, Boolean>();

    public static boolean loadLibrary(String libraryName) {
        // If the library has already been loaded then no need to reload.
        if (LOADED_LIBRARIES.containsKey(libraryName)) return true;

        try {
            File libraryFile = null;

            String libraryPath = _getConfiguredLibraryPath(libraryName);

            if (libraryPath != null) {
                // If library path is configured then use it.
                libraryFile = new File(libraryPath);
            } else {
                libraryFile = _extractLibrary(libraryName);
            }

            System.load(libraryFile.getAbsolutePath());

            LOADED_LIBRARIES.put(libraryName, true);
        } catch (Exception e) {
            System.err.println("Error loading library: " + libraryName);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static String _getConfiguredLibraryPath(String libraryName) {
        String key = String.format("com.couchbase.lite.lib.%s.path", libraryName);

        return System.getProperty(key);
    }

    private static String _getLibraryFullName(String libraryName) {
        String name = System.mapLibraryName(libraryName);

        // Workaround discrepancy issue between OSX Java6 (.jnilib)
        // and Java7 (.dylib) native library file extension.
        if (name.endsWith(".jnilib")) {
            name = name.replace(".jnilib", ".dylib");
        }

        return name;
    }

    private static File _extractLibrary(String libraryName) throws IOException {
        String libraryResourcePath = _getLibraryResourcePath(libraryName);
        String targetFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();

        File targetFile = new File(targetFolder, _getLibraryFullName(libraryName));

        // If the target already exists, and it's unchanged, then use it, otherwise delete it and
        // it will be replaced.
        if (targetFile.exists()) {
            // Remove old native library file.
            if (!targetFile.delete()) {
                // If we can't remove the old library file then log a warning and try to use it.
                System.err.println("Failed to delete existing library file: " + targetFile.getAbsolutePath());
                return targetFile;
            }
        }

        // Extract the library to the target directory.
        InputStream libraryReader = NativeLibUtils.class.getResourceAsStream(libraryResourcePath);
        if (libraryReader == null) {
            System.err.println("Library not found: " + libraryResourcePath);
            return null;
        }

        FileOutputStream libraryWriter = new FileOutputStream(targetFile);
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;

            while ((bytesRead = libraryReader.read(buffer)) != -1) {
                libraryWriter.write(buffer, 0, bytesRead);
            }
        } finally {
            libraryWriter.close();
            libraryReader.close();
        }

        // On non-windows systems set up permissions for the extracted native library.
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                Runtime.getRuntime().exec(new String[]{"chmod", "755", targetFile.getAbsolutePath()}).waitFor();
            } catch (Throwable e) {
                System.err.println("Error executing 'chmod 755' on extracted native library");
                e.printStackTrace();
            }
        }

        return targetFile;
    }

    private static String _getLibraryResourcePath(String libraryName) {
        // Root native folder.
        String path = "/native";

        // OS part of path.
        String osName = System.getProperty("os.name");
        if (osName.contains("Linux")) {
            path += "/linux";
        } else if (osName.contains("Mac")) {
            path += "/osx";
        } else if (osName.contains("Windows")) {
            path += "/windows";
        } else {
            path += "/" + osName.replaceAll("\\W", "").toLowerCase();
        }

        // Architecture part of path.
        String archName = System.getProperty("os.arch");
        path += "/" + archName.replaceAll("\\W", "");

        // Platform specific name part of path.
        path += "/" + _getLibraryFullName(libraryName);

        return path;
    }
}

