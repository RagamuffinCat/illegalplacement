package io.github.ragamuffincat.illegalplacement.config;

import io.github.ragamuffincat.illegalplacement.IllegalPlacement;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

// java.util.Properties with typed getters. Missing or invalid keys are written back with their default.
final class SimpleProperties {

    private final Path file;
    private final String header;
    private final Properties props = new Properties();
    private boolean dirty;

    SimpleProperties(Path file, String header) {
        this.file = file;
        this.header = header;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                IllegalPlacement.LOG.warn("Could not read {}, using defaults", file, e);
            }
        } else {
            dirty = true;
        }
    }

    boolean getBoolean(String key, boolean def) {
        String raw = props.getProperty(key);
        if (raw == null) {
            set(key, Boolean.toString(def));
            return def;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    int getInt(String key, int def, int min, int max) {
        String raw = props.getProperty(key);
        if (raw != null) {
            try {
                return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        set(key, Integer.toString(def));
        return def;
    }

    double getDouble(String key, double def, double min, double max) {
        String raw = props.getProperty(key);
        if (raw != null) {
            try {
                return Math.max(min, Math.min(max, Double.parseDouble(raw.trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        set(key, Double.toString(def));
        return def;
    }

    void set(String key, String value) {
        if (!value.equals(props.getProperty(key))) {
            props.setProperty(key, value);
            dirty = true;
        }
    }

    void saveIfDirty() {
        if (!dirty) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(writer, header);
            }
            dirty = false;
        } catch (IOException e) {
            IllegalPlacement.LOG.warn("Could not write {}", file, e);
        }
    }
}
