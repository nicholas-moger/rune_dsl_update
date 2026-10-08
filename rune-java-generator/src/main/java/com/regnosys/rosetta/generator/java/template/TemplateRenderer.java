package com.regnosys.rosetta.generator.java.template;

import com.regnosys.rosetta.generator.GenerationException;
import org.stringtemplate.v4.AutoIndentWriter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STWriter;
import org.stringtemplate.v4.misc.STMessage;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renders template models to strings using StringTemplate 4 (ST4).
 *
 * <p>Loads {@code .stg} group files from classpath or filesystem.
 * Uses {@link AutoIndentWriter} which preserves literal template indentation
 * and auto-indents multi-line expression output to match the call site.
 * ({@link org.stringtemplate.v4.NoIndentWriter} was initially considered but
 * strips ALL indentation including literal template text, breaking D11.)
 * Output is normalized to Unix line endings ({@code \n}).
 *
 * <p>Thread safety: {@link STGroupFile} instances are shared (thread-safe
 * after loading). Individual {@link ST} instances are created fresh per
 * render call (not thread-safe, so never shared).
 *
 * <p>Template errors are captured via ST4's {@link STErrorListener} and
 * reported through the existing {@link GenerationException} framework.
 *
 * @see <a href="https://github.com/antlr/stringtemplate4">StringTemplate 4</a>
 */
public class TemplateRenderer {

    private final Map<String, STGroup> groups = new ConcurrentHashMap<>();

    /**
     * Load an {@code .stg} group file from the classpath.
     *
     * @param classpathResource e.g., "templates/java-enum.stg"
     * @throws IllegalArgumentException if the resource is not found
     */
    public void loadGroupFromClasspath(String classpathResource) {
        URL url = getClass().getClassLoader().getResource(classpathResource);
        if (url == null) {
            throw new IllegalArgumentException(
                "Template not found on classpath: " + classpathResource);
        }
        STGroupFile group = new STGroupFile(url, "UTF-8", '<', '>');
        group.setListener(new ThrowingErrorListener());
        group.load();
        groups.put(classpathResource, group);
    }

    /**
     * Load an {@code .stg} group file from a filesystem path.
     * Supports external template loading for hot-reload development (FE-4).
     *
     * @param path absolute path to the .stg file
     */
    public void loadGroupFromPath(String path) {
        STGroupFile group = new STGroupFile(path, "UTF-8", '<', '>');
        group.setListener(new ThrowingErrorListener());
        group.load();
        groups.put(path, group);
    }

    /**
     * Load an {@code .stg} group file from a filesystem path with custom delimiters.
     * Supports external loading of {@code $}-delimited groups (e.g., package-info.stg).
     *
     * @param path absolute path to the .stg file
     * @param delimiterStartChar start delimiter (e.g., '$')
     * @param delimiterStopChar stop delimiter (e.g., '$')
     */
    public void loadGroupFromPath(String path, char delimiterStartChar, char delimiterStopChar) {
        STGroupFile group = new STGroupFile(path, "UTF-8", delimiterStartChar, delimiterStopChar);
        group.setListener(new ThrowingErrorListener());
        group.load();
        groups.put(path, group);
    }

    /**
     * Load an {@code .stg} group file from the classpath with custom delimiters.
     * Used for templates where the default {@code <} / {@code >} delimiters
     * conflict with output content (e.g., package-info.stg with HTML {@code <p>} tags).
     *
     * @param classpathResource e.g., "templates/java-package-info.stg"
     * @param delimiterStartChar start delimiter (e.g., '$')
     * @param delimiterStopChar stop delimiter (e.g., '$')
     */
    public void loadGroupFromClasspath(String classpathResource,
                                        char delimiterStartChar, char delimiterStopChar) {
        URL url = getClass().getClassLoader().getResource(classpathResource);
        if (url == null) {
            throw new IllegalArgumentException(
                "Template not found on classpath: " + classpathResource);
        }
        STGroupFile group = new STGroupFile(url, "UTF-8",
                delimiterStartChar, delimiterStopChar);
        group.setListener(new ThrowingErrorListener());
        group.load();
        groups.put(classpathResource, group);
    }

    /**
     * Render a named template from a specific loaded group.
     * Always requires the group key to avoid non-deterministic lookup
     * when multiple groups define templates with the same name.
     *
     * @param groupKey the key used when loading the group
     * @param templateName the template name within the group
     * @param args alternating name/value pairs: "name1", value1, "name2", value2, ...
     * @return the rendered string with Unix line endings
     * @throws IllegalArgumentException if group or template not found
     * @throws GenerationException if rendering fails
     */
    public String render(String groupKey, String templateName, Object... args) {
        STGroup group = groups.get(groupKey);
        if (group == null) {
            throw new IllegalArgumentException("Group not loaded: " + groupKey);
        }
        ST st = group.getInstanceOf(templateName);
        if (st == null) {
            throw new IllegalArgumentException(
                "Template '" + templateName + "' not found in group '" + groupKey + "'");
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                "args must be alternating name/value pairs (even count), got " + args.length);
        }
        for (int i = 0; i < args.length; i += 2) {
            Object argName = args[i];
            if (!(argName instanceof String)) {
                throw new IllegalArgumentException(
                    "args[" + i + "] must be a non-null String template argument name, got " +
                        (argName == null ? "null" : argName.getClass().getName()));
            }
            st.add((String) argName, args[i + 1]);
        }
        return renderST(st);
    }

    /**
     * Get a loaded group by key.
     *
     * @param key the key used when loading the group
     * @return the STGroup, or null if not loaded
     */
    public STGroup getGroup(String key) {
        return groups.get(key);
    }

    /**
     * Render a named template using NoIndentWriter (for templates that manage
     * all indentation explicitly via {@code <\t>} and pre-computed model attributes).
     * Function templates use this to avoid AutoIndentWriter's interference with
     * whitespace-only blank lines.
     */
    public String renderNoIndent(String groupKey, String templateName, Object... args) {
        STGroup group = groups.get(groupKey);
        if (group == null) {
            throw new IllegalArgumentException("Group not loaded: " + groupKey);
        }
        ST st = group.getInstanceOf(templateName);
        if (st == null) {
            throw new IllegalArgumentException(
                "Template '" + templateName + "' not found in group '" + groupKey + "'");
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException(
                "args must be alternating name/value pairs (even count), got " + args.length);
        }
        for (int i = 0; i < args.length; i += 2) {
            Object argName = args[i];
            if (!(argName instanceof String)) {
                throw new IllegalArgumentException(
                    "args[" + i + "] must be a non-null String template argument name, got " +
                        (argName == null ? "null" : argName.getClass().getName()));
            }
            st.add((String) argName, args[i + 1]);
        }
        return renderSTNoIndent(st);
    }

    private String renderST(ST st) {
        try {
            StringWriter writer = new StringWriter();
            // AutoIndentWriter preserves literal indentation from templates and
            // auto-indents multi-line expression output to match the call site.
            STWriter stWriter = new AutoIndentWriter(writer);
            st.write(stWriter);
            // Normalize line endings — Windows may produce \r\n, D11 requires \n
            return writer.toString().replace("\r\n", "\n");
        } catch (IOException e) {
            throw new GenerationException(
                "Template rendering failed: " + e.getMessage(), null, null, e);
        }
    }

    private String renderSTNoIndent(ST st) {
        try {
            StringWriter writer = new StringWriter();
            // NoIndentWriter preserves literal indentation from templates without
            // adding auto-indentation. All indentation is controlled explicitly
            // via <\t> expressions and pre-computed model attributes.
            STWriter stWriter = new org.stringtemplate.v4.NoIndentWriter(writer);
            st.write(stWriter);
            return writer.toString().replace("\r\n", "\n").replace("\r", "\n");
        } catch (IOException e) {
            throw new GenerationException(
                "Template rendering failed: " + e.getMessage(), null, null, e);
        }
    }

    /**
     * Error listener that throws on template errors rather than logging silently.
     * Ensures D11 correctness — template errors produce fast failures instead
     * of silently wrong output.
     */
    private static class ThrowingErrorListener implements STErrorListener {
        @Override
        public void compileTimeError(STMessage msg) {
            throw new GenerationException("ST4 compile error: " + msg, null, null, null);
        }

        @Override
        public void runTimeError(STMessage msg) {
            // NO_SUCH_ATTRIBUTE fires for genuinely missing attribute names (typos),
            // NOT for null-valued attributes. All attributes are explicitly added via
            // render() args, so this would indicate a real bug. Let it throw.
            throw new GenerationException("ST4 runtime error: " + msg, null, null, null);
        }

        @Override
        public void IOError(STMessage msg) {
            throw new GenerationException("ST4 IO error: " + msg, null, null, null);
        }

        @Override
        public void internalError(STMessage msg) {
            throw new GenerationException("ST4 internal error: " + msg, null, null, null);
        }
    }
}
