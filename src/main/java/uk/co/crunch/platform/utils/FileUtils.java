package uk.co.crunch.platform.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static uk.co.crunch.platform.utils.StringUtils.replace;

public class FileUtils {

    public static String getFriendlyFileName(final String path) {
        return getFriendlyFileName(Paths.get(path));
    }

    public static String getFriendlyFileName(final File file) {
        if (file == null) {
            return "";
        }

        return replace(
                replace(file.getAbsolutePath(), System.getProperty("user.dir"), "."), System.getProperty("user.home"), "~");
    }

    public static String getFriendlyFileName(final Path path) {
        if (path == null) {
            return "";
        }

        return replace(
                replace(path.toAbsolutePath().toString(), System.getProperty("user.dir"), "."), System.getProperty("user.home"), "~");
    }
}
