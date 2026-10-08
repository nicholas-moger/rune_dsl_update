package com.regnosys.rosetta.generator.java.util;

import com.regnosys.rosetta.ast.RDefinable;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.util.List;

/**
 * Shared generation utilities: Javadoc rendering, HTML escaping, version
 * formatting. Used by all generators for consistent documentation output.
 *
 * <p>Ported from upstream Xtend — replaced Guava {@code HtmlEscapers} with
 * hand-rolled HTML escaping ({@code &, <, >, ", '}).
 */
public class ModelGeneratorUtil {

    private final RWorkspace workspace;

    public ModelGeneratorUtil() {
        this.workspace = null;
    }

    public ModelGeneratorUtil(RWorkspace workspace) {
        this.workspace = workspace;
    }

    /**
     * Generate a Javadoc comment block from definition, doc references, and version.
     * Returns {@code null} only if ALL inputs are absent. Distinguishes three
     * definition states (matches upstream rune-dsl exactly):
     * <ul>
     *   <li>{@code definition == null} (absent) — no body line emitted; if
     *       docRefs / version also absent, return null (no javadoc block).</li>
     *   <li>{@code definition.isEmpty()} (explicit {@code <"">} in source) —
     *       still warrants a javadoc block (empty placeholder), but no body
     *       line. The empty-string definition is a deliberate source-level
     *       distinction in rune-dsl grammar that upstream codegen honors.</li>
     *   <li>{@code definition} non-empty — body line emitted as
     *       {@code  * &lt;text&gt;}.</li>
     * </ul>
     *
     * <p>Per P2.1.1 T3 Cluster A pilot: the prior gate
     * {@code hasDefinition = definition != null && !definition.isEmpty()}
     * collapsed the absent and empty-string cases, suppressing the empty
     * placeholder block that upstream emits for explicit {@code <"">}
     * definitions on enum values (e.g. DRR
     * {@code OrganizationCharacteristicEnum.CaptiveFinanceUnit <"">}).
     */
    public String javadoc(String definition, List<? extends RDocReference> docRefs, String version) {
        return javadoc(definition, docRefs, version, true);
    }

    /**
     * facet pojoOverrideNaming (PR #323): {@code pojoStyle} selects between the two
     * upstream javadoc renderings, both verified against the 9.83.0 goldens:
     * <ul>
     *   <li>{@code true} — the POJO/function style (upstream ModelGeneratorUtil
     *       templates): multi-line definition/provision/corpus texts take the one-space
     *       continuation prefix ({@link #xtendIndent}) and a PATHED doc reference emits
     *       its {@code  * a -> b} path line before the Body line.</li>
     *   <li>{@code false} — the enum style (upstream EnumGenerator's own template, whose
     *       interpolations sit at column 0): continuations stay RAW and no path line is
     *       emitted. The green cdm6 {@code RatingPriorityResolutionEnum} golden carries
     *       the raw form — prefixing regressed it, which is how the two styles were
     *       discriminated empirically.</li>
     * </ul>
     */
    public String javadoc(String definition, List<? extends RDocReference> docRefs, String version,
                          boolean pojoStyle) {
        boolean hasDefinition = definition != null;
        boolean hasDefinitionBody = hasDefinition && !definition.isEmpty();
        boolean hasDocRefs = docRefs != null && !docRefs.isEmpty();
        boolean hasVersion = version != null && !version.isEmpty();

        if (!hasDefinition && !hasDocRefs && !hasVersion) {
            return null;
        }

        var sb = new StringBuilder();
        sb.append("/**\n");
        if (hasDefinitionBody) {
            String body = escapeHtml(definition);
            sb.append(" * ").append(pojoStyle ? xtendIndent(body) : body).append("\n");
        }
        if (hasVersion) {
            sb.append(" * @version ").append(version).append("\n");
        }
        if (hasDocRefs) {
            for (var ref : docRefs) {
                sb.append(" *\n");
                appendDocReference(sb, ref, pojoStyle);
            }
        }
        sb.append(" */");
        return sb.toString();
    }

    /**
     * Generate a Javadoc comment with just a version tag.
     */
    public String emptyJavadocWithVersion(String version) {
        // Xtend null interpolation produces empty string; Java would produce "null"
        return "/**\n * @version " + (version != null ? version : "") + "\n */";
    }

    /**
     * Generate Javadoc for a definable element with optional version.
     */
    public String javadoc(RDefinable element, String version) {
        String definition = element.definition().orElse(null);
        // Doc references would come from the element if it has them
        // For now, pass empty list — generators will provide doc refs directly
        return javadoc(definition, List.of(), version);
    }

    /**
     * facet pojoOverrideNaming (PR #323): the Xtend template auto-indent law for an
     * interpolated multi-line value — upstream's javadoc templates interpolate the
     * definition / provision / corpus texts on lines whose leading whitespace is ONE
     * SPACE ({@code ' * Provision «provision»'}), and Xtend prefixes every continuation
     * line of the interpolated value with that leading whitespace. The 9.83.0 goldens
     * therefore carry a one-space prefix on every embedded-newline continuation line;
     * the fork appended the raw text. Green-safe by construction: golden ALWAYS carries
     * the prefix, so no green file can hold the unprefixed form — every multi-line
     * carrier is an already-waivered mismatch.
     */
    private static String xtendIndent(String text) {
        if (text == null || text.indexOf('\n') < 0) {
            return text;
        }
        return text.replace("\n", "\n ");
    }

    /**
     * Escape HTML special characters for Javadoc.
     * Replaces Guava's {@code HtmlEscapers.htmlEscaper().escape()}.
     */
    public static String escapeHtml(String text) {
        if (text == null) return null;
        var sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Render a doc reference in javadoc format, matching upstream exactly:
     * <pre>
     *  * Body ISDA
     *  * Corpus Scheme FpML_Coding_Scheme
     *  * schemeLocation "http://..."
     *  *
     *  * Provision
     *  *
     * </pre>
     */
    private void appendDocReference(StringBuilder sb, RDocReference ref, boolean pojoStyle) {
        // facet pojoOverrideNaming (PR #323): a PATHED doc reference
        // ([regulatoryReference for a -> b ...]) emits its path as a javadoc line
        // BEFORE the Body line — upstream `«IF reference.path !== null» * «path»«ENDIF»`,
        // folded root / item / `r -> attr` / `r ->> attr` (deep). Verified against the
        // 9.83.0 goldens (`* periodicPayment -> fixedRateDayCountConvention`). Green-safe
        // by construction: golden ALWAYS emits the path line for a pathed reference, so
        // no green file carries the pathless form — every carrier is already waivered.
        // POJO/function style only (the enum style emits neither path line nor prefix).
        if (pojoStyle) {
            ref.forPath().ifPresent(path ->
                    sb.append(" * ").append(javadocPath(path)).append("\n"));
        }
        var docRef = ref.regulatoryDocRef();
        if (docRef != null) {
            // Body line
            if (docRef.bodyRef() != null) {
                sb.append(" * Body ").append(docRef.bodyRef()).append("\n");
            }
            // Corpus line — resolve full corpus metadata from workspace
            // Upstream format: " * Corpus {type} {name} {displayName} {\"definition\"} "
            // Each field separated by space; trailing space before the next clause.
            // facet corpusJoin (PR #320): ALL corpus refs of ONE doc reference render on a
            // SINGLE line (like the segment refs below) — upstream emits no per-corpus
            // newline, so a multi-corpus [regulatoryReference <body> <corpus1> <corpus2> …]
            // joins ` * Corpus <c1> … * Corpus <c2> …` with ONE trailing newline. The fork
            // emitted one line per corpus, which never byte-matched: 0 of the 34,686 goldens
            // carry consecutive ` * Corpus` lines (plain-grep over corpus-baseline-9.83),
            // and the 6 joined-form golden carriers are all already-waivered drr POJO files.
            // A 0- or 1-corpus reference renders byte-identically to the pre-#320 form (only
            // the newline placement moves, never the content) — green-safe by construction.
            for (String corpusName : docRef.corpusRefs()) {
                sb.append(" * Corpus ");
                RCorpus corpus = lookupCorpus(ref, corpusName);
                if (corpus != null) {
                    if (corpus.corpusTypeKeyword() != null) {
                        sb.append(corpus.corpusTypeKeyword()).append(" ");
                    }
                    // facet docrefCorpusScope (PR #330): render the RESOLVED corpus's own
                    // simple name, never the raw reference text — an import-ALIASED
                    // qualified ref (`cftcTrade.Trade`) prints golden's `Trade`; a plain
                    // ref is byte-identical (name == reference text).
                    sb.append(corpus.name()).append(" ");
                    if (corpus.displayName().isPresent()) {
                        String dn = escapeHtml(corpus.displayName().get());
                        sb.append(pojoStyle ? xtendIndent(dn) : dn);
                    }
                    sb.append(" ");
                    if (corpus.definition().isPresent()) {
                        String cd = escapeHtml(corpus.definition().get());
                        sb.append("\"").append(pojoStyle ? xtendIndent(cd) : cd).append("\"");
                    }
                    sb.append(" ");
                } else {
                    sb.append(corpusName).append("   ");
                }
            }
            if (!docRef.corpusRefs().isEmpty()) {
                sb.append("\n");
            }
            // Segment refs: all on one line, with " * " prefix
            // Upstream: «FOR segment» * segmentName "segmentValue"«ENDFOR»
            if (!docRef.segmentRefs().isEmpty()) {
                sb.append(" * ");
                for (int i = 0; i < docRef.segmentRefs().size(); i++) {
                    var seg = docRef.segmentRefs().get(i);
                    if (i > 0) sb.append(" * ");
                    String sv = escapeHtml(seg.value());
                    sb.append(seg.segmentName())
                      .append(" \"").append(pojoStyle ? xtendIndent(sv) : sv).append("\"");
                }
                sb.append("\n");
            }
        }
        sb.append(" *\n");
        // Provision — a multi-line provision's continuation lines take the one-space
        // template prefix (xtendIndent), matching the 9.83.0 goldens.
        String provision = ref.provision().orElse(null);
        if (provision != null && !provision.isEmpty()) {
            sb.append(" * Provision ").append(pojoStyle ? xtendIndent(provision) : provision).append("\n");
        } else {
            sb.append(" * Provision \n");
        }
        sb.append(" *\n");
    }

    /**
     * facet pojoOverrideNaming (PR #323): the annotation-path javadoc fold — upstream
     * {@code javadocPath}: the root name (or the {@code item} keyword), then
     * {@code " -> name"} per shallow step / {@code " ->> name"} per deep step.
     */
    private static String javadocPath(
            com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression path) {
        var sb = new StringBuilder(path.isRootItem() ? "item" : path.root());
        for (var seg : path.segments()) {
            sb.append(seg.isDeep() ? " ->> " : " -> ").append(seg.name());
        }
        return sb.toString();
    }

    /**
     * Look up the corpus declaration a doc reference names, resolved in the
     * REFERENCING file's scope.
     *
     * <p>facet docrefCorpusScope (PR #330): the parser stores corpus refs as raw
     * qualified-name STRINGS (no resolved binding), so all disambiguation happens
     * here. The pre-#330 lookup was a bare {@code workspace.findByName} — a GLOBAL
     * name index whose first {@code RCorpus} wins — so a name declared in several
     * namespaces (drr {@code DTCC_Specs} ×5, {@code CDE} ×3, one per regime/version
     * with DIFFERENT displayName/definition text) rendered the wrong version's text
     * for every non-first referencing file. Upstream's corpus is an Xtext
     * cross-reference resolved in the referencing file's scope; mirror that:
     * <ol>
     *   <li>a QUALIFIED ref ({@code cftcTrade.Trade}) resolves its prefix as a
     *       WILDCARD import ALIAS of the referencing model (then as a literal
     *       namespace) and looks the simple name up in that namespace — an
     *       aliased NAMED import binds a simple name, never a prefix (mirrors
     *       {@code ImportEntry.lookup}; #330 Copilot R1);</li>
     *   <li>a SIMPLE ref resolves in the referencing model's OWN namespace first,
     *       then its imports in declaration order — un-aliased wildcard imports
     *       expose every name in their target namespace, NAMED imports (aliased
     *       or not) expose exactly their alias-or-last-segment (mirrors
     *       {@code ImportEntry.lookup}; both named-import arms have ZERO corpus
     *       carriers today — the frozen baseline's only named import is the type
     *       {@code cdm.product.asset.FloatingRateBase} — so they are byte-neutral
     *       hardening; #330 Copilot R1);</li>
     *   <li>the global first-match stays as the last-resort fallback (byte-preserving
     *       for any reference the scoped walk cannot place — including a detached
     *       node with no {@code RModel} ancestor).</li>
     * </ol>
     * Green-safe: every green file rendering a multi-declared corpus name matches
     * golden under the global pick today, which means the global pick IS its
     * same-namespace pick — the scoped walk returns the same declaration.
     *
     * <p>PUBLIC since v3.3 seat 5 (decision D55): the declaration IR carries each doc
     * reference's corpus RESOLVED, and the IR route resolves it through THIS lookup —
     * one producer, so the IR's fact and the javadoc line below cannot disagree.
     */
    public RCorpus lookupCorpus(RDocReference ref, String corpusName) {
        if (workspace == null) return null;
        RModel model = AstWalker.findAncestor(ref, RModel.class).orElse(null);
        int dot = corpusName.lastIndexOf('.');
        if (dot >= 0) {
            String prefix = corpusName.substring(0, dot);
            String simple = corpusName.substring(dot + 1);
            if (model != null) {
                for (RImport imp : model.imports()) {
                    if (imp.isWildcard() && imp.alias().filter(prefix::equals).isPresent()) {
                        RCorpus c = corpusInNamespace(imp.qualifiedName(), simple);
                        if (c != null) return c;
                    }
                }
            }
            RCorpus c = corpusInNamespace(prefix, simple);
            if (c != null) return c;
        } else if (model != null) {
            RCorpus own = corpusInNamespace(model.namespace(), corpusName);
            if (own != null) return own;
            for (RImport imp : model.imports()) {
                if (imp.alias().isEmpty() && imp.isWildcard()) {
                    RCorpus c = corpusInNamespace(imp.qualifiedName(), corpusName);
                    if (c != null) return c;
                } else if (!imp.isWildcard()) {
                    String qn = imp.qualifiedName();
                    int qnDot = qn.lastIndexOf('.');
                    String localName = qnDot < 0 ? qn : qn.substring(qnDot + 1);
                    if (qnDot >= 0 && imp.alias().orElse(localName).equals(corpusName)) {
                        RCorpus named = corpusInNamespace(qn.substring(0, qnDot), localName);
                        if (named != null) return named;
                    }
                }
            }
        }
        List<RNode> results = workspace.findByName(corpusName);
        for (RNode node : results) {
            if (node instanceof RCorpus corpus) {
                return corpus;
            }
        }
        return null;
    }

    /** The corpus declared as {@code simpleName} in exactly the namespace {@code ns}, or null. */
    private RCorpus corpusInNamespace(String ns, String simpleName) {
        if (ns == null || ns.isEmpty()) return null;
        return workspace.namespace(ns)
                .map(scope -> scope.allMatching(simpleName))
                .orElse(List.of())
                .stream()
                .filter(RCorpus.class::isInstance)
                .map(RCorpus.class::cast)
                .findFirst()
                .orElse(null);
    }
}
