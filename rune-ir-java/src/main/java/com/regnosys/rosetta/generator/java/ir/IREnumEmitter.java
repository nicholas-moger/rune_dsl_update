package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumHelper;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.JavaStringUtil;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRType;
import com.rosetta.util.DottedPath;

/**
 * THE ENUM EMITTER (v3.3 seat 6, PR #642 - decision D55, ruling R3 stage 1): writes a Java enum file from an
 * {@link IREnumNode} ALONE. It reads no AST node and calls no class of the old enum generator
 * ({@code EnumGenerator}, {@code RJavaEnum}, {@code RJavaEnumValue}); every byte of the file is a function of the
 * declaration IR's named facts (decision D55) and of the template's fixed text. The old generator's template
 * {@code java-enum.stg} is NOT shared: this emitter carries its own copy ({@code templates/ir-java-enum.stg},
 * byte-identical at the copy - the rings pin both to the same goldens, so the two cannot drift apart silently),
 * and the old generator could be deleted with this file still written. What IS shared, said plainly (the seat's
 * probe, {@code target/v33-seat6-instruments/scratch/enum-emitter-map.md}): FIVE pure string functions and TWO
 * formatting utilities of the shipping generator - {@link EnumHelper#formatEnumName} and {@link EnumHelper#stripEscape}
 * (the Java value-name mangling), {@link JavaPackageName#escape} (the package escape), {@link JavaStringUtil#escapeJava}
 * (the constructor literal), {@link ModelGeneratorUtil#escapeHtml} (the javadoc text); {@link ImportCollector} (the
 * sorted import set) and {@link TemplateRenderer} (the ST4 wrapper that renders this emitter's own template). None of
 * them reads a model. The route's generator, {@link IREnumGenerator}, still EXTENDS the old {@code EnumGenerator} and
 * the ServiceLoader seam is typed by it, so deleting the old generator class needs the seam re-typed first - what could
 * be deleted with every enum file still written is the old generator's RENDERING: {@code RJavaEnum}, {@code RJavaEnumValue},
 * {@code EnumTemplateModel} and {@code java-enum.stg}.
 *
 * <p>The byte contract, fact by fact: the package is the declaring namespace escaped ({@link IREnumNode#namespace()});
 * the simple name is {@link IREnumNode#name()} with that namespace stripped (a name that does not start with it is a
 * REFUSAL); the values are the PARENT CHAIN's first - {@link IREnumNode#parent()} is a reference, so the emitter
 * resolves it through the workspace-wide index the host supplies ({@link IREnumIndex}; a parent may live in another
 * model, even one outside the cell's emission filter - DRR's {@code CommodityTimeUnitEnum extends TimeUnitEnum} reaches
 * into the transitive CDM) and an absent parent is a REFUSAL, never a silently flat enum; a value's Java name is the
 * mangled rosetta name, its rosetta name the IR name with the parser-escape caret stripped (the fork's lexer already
 * strips it at the token, so the strip is a belt), its display name RAW in the annotation and Java-escaped in the
 * constructor (the one escape law, D46 F1), its synonyms one {@code @RosettaSynonym} per (value, source), RAW; the
 * javadocs are the ENUM style (no continuation prefix, no path line) with the doc references rendered from
 * {@link IRDocReference} - the corpus from its RESOLVED declaration (the type keyword, the own name, the display name and
 * the definition, HTML-escaped) or, unresolved, the written reference and three spaces; the version stamp is the
 * host's (the model's version is not an IR fact).
 */
public final class IREnumEmitter {

    static final String TEMPLATE_GROUP = "templates/ir-java-enum.stg";

    private final TemplateRenderer renderer;
    private final Function<String, Optional<IREnumNode>> index;
    private final LinkedHashSet<String> written = new LinkedHashSet<>();

    /** @param index the workspace-wide index of adapted enums, keyed by {@code namespace.SimpleName} */
    public IREnumEmitter(Function<String, Optional<IREnumNode>> index) {
        this.index = Objects.requireNonNull(index, "index");
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /** The output keys of every file this emitter wrote, in order - the generator's {@code filesWrittenByIrEmitter()}. */
    public Set<String> written() {
        return Collections.unmodifiableSet(written);
    }

    /**
     * The output key of the enum's file - the old generator's {@code getCanonicalName().withForwardSlashes() + ".java"}:
     * the escaped package with forward slashes, then the simple name.
     */
    public static String outputKey(IREnumNode enumeration) {
        return packageOf(enumeration).getName().child(simpleName(enumeration)).withForwardSlashes() + ".java";
    }

    /**
     * Writes the enum's file from the IR alone and books its key. Every refusal is a {@link GenerationException}
     * carrying the target path (the host's error gate names the file).
     */
    public String emit(IREnumNode enumeration, String version) {
        String key = outputKey(enumeration);
        try {
            String text = renderer.render(TEMPLATE_GROUP, "enumFile", "m", model(enumeration, version));
            written.add(key);
            return text;
        } catch (GenerationException e) {
            e.setTargetPath(key);
            throw e;
        } catch (RuntimeException e) {
            GenerationException wrapped = new GenerationException("IR enum emitter failed for " + enumeration.name()
                    + ": " + e.getMessage(), null, null, e);
            wrapped.setTargetPath(key);
            throw wrapped;
        }
    }

    // ------------------------------------------------------------------------------------------------ the model

    EnumFile model(IREnumNode enumeration, String version) {
        String packageName = packageOf(enumeration).getName().withDots();
        String enumName = simpleName(enumeration);
        List<IREnumValue> values = flattenedValues(enumeration);

        // the sorted import set: five fixed, RosettaSynonym iff any value (a parent's included) carries a synonym
        ImportCollector imports = new ImportCollector(packageName);
        imports.addImport("com.rosetta.model.lib.annotations.RosettaEnum");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaEnumValue");
        imports.addImport("java.util.Collections");
        imports.addImport("java.util.Map");
        imports.addImport("java.util.concurrent.ConcurrentHashMap");
        boolean hasSynonyms = values.stream().anyMatch(v -> !v.synonyms().isEmpty());
        if (hasSynonyms) {
            imports.addImport("com.rosetta.model.lib.annotations.RosettaSynonym");
        }

        String javadoc = javadoc(enumeration.definition(), enumeration.docReferences(), version);

        List<EnumValueRow> rows = new ArrayList<>();
        for (IREnumValue value : values) {
            String valueJavadoc = javadoc(value.definition(), value.docReferences(), null);
            if (valueJavadoc != null) {
                valueJavadoc = valueJavadoc.lines().map(line -> "\t" + line)
                        .collect(Collectors.joining("\n"));
            }
            List<SynonymRow> synonyms = new ArrayList<>();
            for (IREnumSynonym synonym : value.synonyms()) {
                synonyms.add(new SynonymRow(synonym.value(), synonym.sources()));
            }
            String rosettaName = EnumHelper.stripEscape(value.name());
            String displayName = value.displayName().orElse(null);
            rows.add(new EnumValueRow(EnumHelper.formatEnumName(rosettaName), JavaStringUtil.escapeJava(rosettaName),
                    displayName, displayName == null ? null : JavaStringUtil.escapeJava(displayName),
                    valueJavadoc, synonyms));
        }
        return new EnumFile(packageName, enumName, javadoc, imports.getImports(), rows);
    }

    /** The parent chain's values first (recursively, through the index), then the enum's own - a parent value keeps every fact of its own. */
    private List<IREnumValue> flattenedValues(IREnumNode enumeration) {
        List<IREnumValue> values = new ArrayList<>();
        enumeration.parent().ifPresent(parent -> values.addAll(flattenedValues(resolveParent(enumeration, parent))));
        values.addAll(enumeration.values());
        return values;
    }

    /** The parent's adapted node through the index - a parent the index does not hold is a refusal, never a flat enum. */
    private IREnumNode resolveParent(IREnumNode child, IRType parent) {
        Optional<String> qualified = parent.resolvedQualifiedName();
        if (qualified.isEmpty()) {
            throw new GenerationException("IR enum emitter: the parent of " + child.name() + " is unresolved in the IR ("
                    + parent.name() + ")", null, null);
        }
        return index.apply(qualified.get()).orElseThrow(() -> new GenerationException("IR enum emitter: the parent "
                + qualified.get() + " of " + child.name() + " is not in the workspace's enum index", null, null));
    }

    private static JavaPackageName packageOf(IREnumNode enumeration) {
        String namespace = enumeration.namespace().orElseThrow(() -> new GenerationException(
                "IR enum emitter: " + enumeration.name() + " carries no namespace", null, null));
        return JavaPackageName.escape(DottedPath.splitOnDots(namespace));
    }

    /** {@link IREnumNode#name()} is {@code namespace.Simple}; a name that does not start with its namespace is refused. */
    static String simpleName(IREnumNode enumeration) {
        String namespace = enumeration.namespace().orElse("");
        String prefix = namespace.isEmpty() ? "" : namespace + ".";
        if (!enumeration.name().startsWith(prefix) || enumeration.name().length() == prefix.length()) {
            throw new GenerationException("IR enum emitter: the name " + enumeration.name()
                    + " does not start with its namespace " + namespace, null, null);
        }
        return enumeration.name().substring(prefix.length());
    }

    // ---------------------------------------------------------------------------------------------- the javadoc

    /**
     * The ENUM-style javadoc from the IR facts - the three definition states of the old renderer kept (absent: no
     * body line, and null when nothing else is there; explicitly empty: the block without a body line; non-empty:
     * the HTML-escaped body), then {@code @version}, then one doc reference block each.
     */
    static String javadoc(Optional<String> definition, List<IRDocReference> docReferences, String version) {
        boolean hasDefinition = definition.isPresent();
        boolean hasDefinitionBody = hasDefinition && !definition.get().isEmpty();
        boolean hasDocRefs = !docReferences.isEmpty();
        boolean hasVersion = version != null && !version.isEmpty();
        if (!hasDefinition && !hasDocRefs && !hasVersion) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        if (hasDefinitionBody) {
            sb.append(" * ").append(ModelGeneratorUtil.escapeHtml(definition.get())).append("\n");
        }
        if (hasVersion) {
            sb.append(" * @version ").append(version).append("\n");
        }
        for (IRDocReference ref : docReferences) {
            sb.append(" *\n");
            appendDocReference(sb, ref);
        }
        sb.append(" */");
        return sb.toString();
    }

    /**
     * One doc reference, enum style (no path line, no continuation prefix): the Body line, every corpus on ONE line
     * (the resolved declaration's keyword, own name, display name and quoted definition, each followed by a space;
     * an unresolved corpus is its written reference and three spaces), the segments on one line, a blank star line,
     * the Provision line, a blank star line.
     */
    private static void appendDocReference(StringBuilder sb, IRDocReference ref) {
        ref.body().ifPresent(body -> sb.append(" * Body ").append(body).append("\n"));
        for (IRDocReference.Corpus corpus : ref.corpora()) {
            sb.append(" * Corpus ");
            if (corpus.resolved().isPresent()) {
                IRDocReference.Corpus.Declaration declaration = corpus.resolved().get();
                declaration.typeKeyword().ifPresent(keyword -> sb.append(keyword).append(" "));
                sb.append(declaration.name()).append(" ");
                declaration.displayName().ifPresent(dn -> sb.append(ModelGeneratorUtil.escapeHtml(dn)));
                sb.append(" ");
                declaration.definition().ifPresent(d -> sb.append("\"").append(ModelGeneratorUtil.escapeHtml(d)).append("\""));
                sb.append(" ");
            } else {
                sb.append(corpus.reference()).append("   ");
            }
        }
        if (!ref.corpora().isEmpty()) {
            sb.append("\n");
        }
        if (!ref.segments().isEmpty()) {
            sb.append(" * ");
            for (int i = 0; i < ref.segments().size(); i++) {
                IRDocReference.Segment segment = ref.segments().get(i);
                if (i > 0) {
                    sb.append(" * ");
                }
                sb.append(segment.name()).append(" \"").append(ModelGeneratorUtil.escapeHtml(segment.value())).append("\"");
            }
            sb.append("\n");
        }
        sb.append(" *\n");
        String provision = ref.provision().orElse(null);
        if (provision != null && !provision.isEmpty()) {
            sb.append(" * Provision ").append(provision).append("\n");
        } else {
            sb.append(" * Provision \n");
        }
        sb.append(" *\n");
    }

    // ------------------------------------------------------------------------------ the template's view (getters)

    /** The file's view for {@code ir-java-enum.stg} - every member pre-computed, the template is pure formatting. */
    public static final class EnumFile {
        private final String packageName;
        private final String enumName;
        private final String javadoc;
        private final List<String> imports;
        private final List<EnumValueRow> values;

        EnumFile(String packageName, String enumName, String javadoc, List<String> imports, List<EnumValueRow> values) {
            this.packageName = packageName;
            this.enumName = enumName;
            this.javadoc = javadoc;
            this.imports = List.copyOf(imports);
            this.values = List.copyOf(values);
        }

        public String getPackageName() { return packageName; }
        public String getEnumName() { return enumName; }
        public String getJavadoc() { return javadoc; }
        public List<String> getImports() { return imports; }
        public List<EnumValueRow> getValues() { return values; }
    }

    /** One value's view: the display name RAW for the annotation and Java-escaped for the constructor (D46 F1). */
    public static final class EnumValueRow {
        private final String javaName;
        private final String rosettaName;
        private final String displayName;
        private final String displayNameLiteral;
        private final String javadoc;
        private final List<SynonymRow> synonyms;

        EnumValueRow(String javaName, String rosettaName, String displayName, String displayNameLiteral,
                     String javadoc, List<SynonymRow> synonyms) {
            if ((displayName == null) != (displayNameLiteral == null)
                    || (displayName != null && !JavaStringUtil.escapeJava(displayName).equals(displayNameLiteral))) {
                throw new IllegalArgumentException("displayNameLiteral must be the Java escape of displayName: "
                        + displayName + " / " + displayNameLiteral);
            }
            this.javaName = javaName;
            this.rosettaName = rosettaName;
            this.displayName = displayName;
            this.displayNameLiteral = displayNameLiteral;
            this.javadoc = javadoc;
            this.synonyms = List.copyOf(synonyms);
        }

        public String getJavaName() { return javaName; }
        public String getRosettaName() { return rosettaName; }
        public String getDisplayName() { return displayName; }
        public String getDisplayNameLiteral() { return displayNameLiteral; }
        public String getJavadoc() { return javadoc; }
        public List<SynonymRow> getSynonyms() { return synonyms; }
    }

    /** One synonym's view: the value and the sources RAW (the annotation seat's law, D46 F1). */
    public static final class SynonymRow {
        private final String value;
        private final List<String> sources;

        SynonymRow(String value, List<String> sources) {
            this.value = value;
            this.sources = List.copyOf(sources);
        }

        public String getValue() { return value; }
        public List<String> getSources() { return sources; }
    }
}
