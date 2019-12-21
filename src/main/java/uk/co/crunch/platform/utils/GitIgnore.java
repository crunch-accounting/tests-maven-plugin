package uk.co.crunch.platform.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GitIgnore {

    public static void ensureEntry(final File dir, final Predicate<String> entryMatcher, final String entryToEnsure) {
        final File gitIgnore = new File(dir, ".gitignore");
        if (!gitIgnore.exists()) {
            try {
                gitIgnore.createNewFile();

                Files.writeString(gitIgnore.toPath(), entryToEnsure, UTF_8);
                return;
            } catch (IOException e) {
                throw new RuntimeException("Could not create .gitignore");
            }
        }

        try {
            boolean wasEmpty = true;
            for (String each : Files.readAllLines(gitIgnore.toPath(), UTF_8)) {
                if (entryMatcher.test(each)) {
                    return;  // Either we added, or the user did, let's leave well alone
                }
                wasEmpty = false;
            }

            Files.writeString(gitIgnore.toPath(), (wasEmpty ? "" : "\n") + entryToEnsure, UTF_8, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Could not read .gitignore");
        }
    }
}
