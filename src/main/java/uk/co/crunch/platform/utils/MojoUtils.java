package uk.co.crunch.platform.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.model.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

public class MojoUtils {

    public static File pickResourcesDirectory(final List<Resource> resources) {
        if (resources.isEmpty()) {
            return null;
        }
        return new File(resources.iterator().next().getDirectory());
    }

    // https://stackoverflow.com/a/20073154/954442
    public static Set<String> resourceFilesFromPath(final String path) throws IOException {
        final File jarFile = new File(MojoUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath());

        final Set<String> files = new LinkedHashSet<>();

        if (jarFile.isFile()) {
            try (var jar = new JarFile(jarFile)) {
                Collections.list(jar.entries()).forEach(eachJarEntry -> {
                    final String name = eachJarEntry.getName();
                    if (name.startsWith(path + "/") && !name.endsWith("/")) {
                        files.add(name);
                    }
                });
            }
        } else {
            final URL url = MojoUtils.class.getResource("/" + path);
            if (url != null) {
                try {
                    for (File eachChild : FileUtils.listFiles(new File(url.toURI()), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
                        final String fullPath = eachChild.getAbsolutePath();
                        files.add(fullPath.substring(fullPath.indexOf(path)));
                    }
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return files;
    }
}
