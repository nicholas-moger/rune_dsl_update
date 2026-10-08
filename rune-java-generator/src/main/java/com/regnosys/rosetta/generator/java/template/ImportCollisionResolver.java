package com.regnosys.rosetta.generator.java.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * facet fqnWitness (PR #227) — the render-order-aware, first-claim-wins import
 * collision resolver: the general form of the upstream
 * {@code ImportingStringConcatenation.internalDoImportIfPossible} streaming
 * import law that PRs #194–#197 patched at individual <em>guaranteed-first</em>
 * anchor seats (input/output/to-enum/witness-vs-output).
 *
 * <p>The fork renders type names into body strings at handler time and collects
 * {@code JavaClass} refs into an unordered set; the {@link ImportCollector} then
 * imports every distinct canonical name. When two DIFFERENT canonical types share
 * a Java simple name in one generated class (e.g. the fpml input witness
 * {@code fpml.consolidated.shared.Offset} and the cdm construction type
 * {@code cdm.base.datetime.Offset}), that produces a DUPLICATE same-simple-name
 * import — a Java compile error — so every such carrier is already
 * non-compiling/waivered (green-safe by construction).
 *
 * <p>Upstream resolves this by emission order: the FIRST type reference to claim a
 * simple name keeps the bare name + import; any LATER reference to a different
 * canonical with the same simple name renders FULLY-QUALIFIED inline and is NOT
 * imported. Unlike PRs #194–#197 (whose anchor — the signature output/input — is
 * always emitted first), the #227 collisions are body-internal (a navigation
 * witness vs a builder-ctor / hoisted local-var) whose winner depends on render
 * order, so there is no fixed anchor: both witness-loses and ctor-loses occur.
 *
 * <p>The colliding seats emit a {@link #typeRef(String) type sentinel} carrying the
 * full canonical name instead of the bare simple name; {@link #resolve} then scans
 * the rendered bodies IN RENDER ORDER, claiming each simple name for the first
 * canonical it sees (rendered bare) and FQN-ing later different-canonical references
 * (whose import is suppressed). For a NON-colliding class every sentinel resolves to
 * the bare simple name and nothing is suppressed, so the output is byte-identical to
 * the pre-facet form.
 *
 * <p>v3.2 seat 11 (PR #632, D50 — the law at SIX kinds): the POJO, the XMETA, the CARDINALITY /
 * ONLY_EXISTS / TYPE_FORMAT validators and the DATA_RULE render every TYPE write as a
 * {@link #typeRefOrBare sentinel} (the library tokens through the templates' {@link #typeRefs token
 * maps}, the model types through the generators) and resolve the WHOLE class text once through
 * {@link #resolveClass}, seeded with the file's own top-level class exactly as upstream's
 * {@code JavaClassScope.createAndRegisterIdentifier} registers it before a byte is written, the
 * losers dropped from the collector's import list — the same law the function bodies and the
 * data-rule body have carried since PR #227, at every kind whose file scope upstream models. Three
 * upstream literals stay outside it by design (a same-package nested builder written bare under a
 * lost outer, the list setter's {@code new ArrayList<>()}, the only-exists validator's
 * {@code Map.Entry} method refs — each the released plugin's own non-compiling Java, pinned as such).
 * A class text with no collision resolves to the pre-seat bytes: the invariant the 34,686 vendored
 * goldens witness at every chain.
 *
 * <p>This is a templating-layer operation on the generator's OWN sentinel tokens
 * (the idiom the fork already uses for statement hoists / indentation), NOT a
 * structural analysis of generated Java — it never inspects Java syntax, only the
 * private-use-area sentinel delimiters this class emits.
 */
public final class ImportCollisionResolver {

    /** Sentinel open delimiter (Unicode private-use area — never present in generated Java). */
    public static final char OPEN = (char) 0xE000;
    /** Sentinel close delimiter (Unicode private-use area — never present in generated Java). */
    public static final char CLOSE = (char) 0xE001;

    private static final Pattern SENTINEL = // ci-allowlist: regex-on-structured-content (the resolver's OWN private-use-area sentinel delimiters — never Java syntax; see the class javadoc)
            Pattern.compile(Pattern.quote(String.valueOf(OPEN))
                    + "([^" + CLOSE + "]+)"
                    + Pattern.quote(String.valueOf(CLOSE)));

    private ImportCollisionResolver() {
    }

    /**
     * Emit a first-claim-wins type sentinel carrying the canonical (dotted) name.
     * The caller renders this in place of the bare simple name at a colliding type
     * position; {@link #resolve} later replaces it with either the bare simple name
     * (first claim) or the FQN (later different-canonical claim).
     *
     * @param canonicalName the fully-qualified dotted type name (never {@code null}/blank)
     * @return the sentinel token
     */
    public static String typeRef(String canonicalName) {
        return OPEN + canonicalName + CLOSE;
    }

    /**
     * v3.2 seat 11 (D50 — the file-scope first-claim law at the data-type kinds): the sentinel for
     * a type WRITE, or the bare simple name when the type is a TOP-LEVEL {@code java.lang} class.
     * Upstream's {@code JavaFileScope} takes every top-level {@code java.lang} simple name implicitly
     * ({@code isNameImplicitlyTaken}), so such a class is never imported and never loses its bare
     * name to a model type — it is written bare, outside the claim order (a NESTED or SUB-PACKAGE
     * {@code java.lang} type is a sentinel like every other class — the {@code @return} tag; round 3
     * of PR #632 swept this paragraph to the law the tag and the code already stated); a MODEL
     * type whose simple name collides with {@code java.lang} is the caller's PR #306 canonical law
     * ({@code collidesWithJavaLang}), also outside this resolver. Every other type — a library
     * token the template writes, a model type, a meta / validator / condition class — claims its
     * simple name at its first write in text order through the sentinel this returns.
     *
     * @param canonicalName the fully-qualified dotted type name (never {@code null}/blank)
     * @return the sentinel, or the bare simple name for a top-level {@code java.lang} class. A {@code java.lang}
     *         NESTED type ({@code java.lang.Thread.State}) and a {@code java.lang} SUB-PACKAGE type
     *         ({@code java.lang.reflect.Method}) are sentinels like every other class: upstream writes the nested
     *         one as its bare simple name ({@code State}) when the name is free and as the canonical when taken
     *         ({@code JavaFileScope.getIdentifier} over the {@code java.lang} package; then
     *         {@code internalDoImportIfPossible} finds {@code Thread} implicitly taken) and imports the sub-package
     *         one bare - the three RENDERED forms (the name as written) the sentinel's first-claim resolution
     *         reproduces. The IMPORT of a nested {@code java.lang} WINNER is the collector's question, not this
     *         method's: {@code ImportCollector.addImport} skips a canonical only when its package IS {@code java.lang},
     *         so {@code java.lang.Thread.State} (package {@code java.lang.Thread}) would be imported where upstream
     *         emits no import - latent today (no caller passes a nested {@code java.lang} canonical; the
     *         {@link #typeRefs} maps carry library tokens only), recorded here at round 4 of PR #632 and not healed.
     *         (Round 1 of PR #632 had
     *         special-cased the nested form as {@code Outer.Inner} - a form upstream never writes, and a branch that
     *         swallowed the sub-package case; round 2's spec MF-1 / cq MF-1 reverted it against the vendored source.)
     */
    public static String typeRefOrBare(String canonicalName) {
        if (canonicalName.startsWith("java.lang.") && canonicalName.indexOf('.', "java.lang.".length()) < 0) {
            return simpleOf(canonicalName);
        }
        return typeRef(canonicalName);
    }

    /**
     * v3.2 seat 11, round 1 (cq SF-1): the ONE test every value-site law shares - the simple name IS the canonical's
     * last segment (a parameterised or array rendering keeps its own spelling and takes no sentinel). Read by the POJO's
     * value-site and meta-value renders and by the validators' cast type; LAW 69 at one declaration.
     */
    public static boolean simpleIsLastSegment(String fqn, String simple) {
        return simple.equals(fqn.substring(fqn.lastIndexOf('.') + 1));
    }

    /**
     * v3.2 seat 11 (D50): the template-side token map — simple name → {@link #typeRefOrBare sentinel}
     * — for the library types a {@code .stg} class-body template writes ({@code <m.t.List>},
     * {@code <m.t.Validator>}, …). One map per generator, built once from the canonical names the
     * template's text carries; a key the template asks for that the map lacks renders EMPTY under
     * ST4, so every template token is listed by its generator and the D11 / hold-out bars catch an
     * omission as a missing type name, never a silent bare one.
     */
    public static Map<String, String> typeRefs(List<String> canonicalNames) {
        Map<String, String> refs = new LinkedHashMap<>();
        for (String canonical : canonicalNames) {
            String prior = refs.put(simpleOf(canonical), typeRefOrBare(canonical));
            if (prior != null) {
                throw new IllegalArgumentException("two template tokens share the simple name "
                        + simpleOf(canonical) + ": " + prior + " and " + canonical);
            }
        }
        return refs;
    }

    /**
     * The whole-class resolution (v3.2 seat 11, D50): the rendered CLASS text (everything after the
     * import block — the javadoc, the annotations, the header and the body, in the file's text order)
     * with every {@link #typeRefOrBare sentinel} resolved first-claim-wins from ONE seed, the file's own
     * top-level class — exactly the identifier {@code JavaClassScope.createAndRegisterIdentifier}
     * registers before upstream writes a byte — and the collector's import list with every LOSER
     * (a canonical rendered fully qualified somewhere and bare nowhere) dropped. A class text with no
     * collision resolves every sentinel to its bare simple name and drops no import: byte-identical
     * to the sentinel-free render, which is the no-regression witness over every golden that carries
     * no collision (the 25 vendored cells carry none — no {@code java.util.X<} is written canonical in
     * any of their 34,686 goldens).
     *
     * @param classText        the rendered class text carrying sentinels
     * @param ownClassCanonical the file's own top-level class, dotted (package + simple name)
     * @param imports          the collector's regular imports, every canonical the file MAY import
     * @return the resolved class text and the import list minus the suppressed losers
     */
    public static ClassResolution resolveClass(String classText, String ownClassCanonical, List<String> imports) {
        if (!hasSentinel(classText)) {
            return new ClassResolution(classText, List.copyOf(imports));
        }
        Result r = resolve(List.of(classText), seedFromCanonicals(List.of(ownClassCanonical)));
        List<String> kept = new ArrayList<>(imports.size());
        for (String imp : imports) {
            if (!r.suppressedCanonicals().contains(imp)) {
                kept.add(imp);
            }
        }
        return new ClassResolution(r.bodies().get(0), kept);
    }

    /**
     * The result of {@link #resolveClass}.
     *
     * @param classText the class text with every sentinel resolved (bare winner / canonical loser)
     * @param imports   the import list with the losers dropped (sorted as the collector sorted it)
     */
    public record ClassResolution(String classText, List<String> imports) {
    }

    /** True when {@code s} carries at least one type sentinel. */
    public static boolean hasSentinel(String s) {
        return s != null && s.indexOf(OPEN) >= 0;
    }

    /** The simple (last-segment) name of a dotted canonical name. */
    private static String simpleOf(String canonical) {
        int dot = canonical.lastIndexOf('.');
        return dot >= 0 ? canonical.substring(dot + 1) : canonical;
    }

    /**
     * The resolution result.
     *
     * @param bodies               the input bodies with every sentinel replaced by the
     *                             bare simple name (first claim) or the FQN (loser)
     * @param suppressedCanonicals the canonical names rendered FQN-inline (losers) — their
     *                             imports must be removed (the first-claim winner keeps its
     *                             import)
     */
    public record Result(List<String> bodies, Set<String> suppressedCanonicals) {
    }

    /**
     * Resolve type sentinels across the ordered bodies with a claim map CARRIED body-to-body —
     * true file-order first-claim-wins.
     *
     * <p>Within a single rendered body the sentinel order IS the true left-to-right render order,
     * so first-claim-wins is exact there. ACROSS bodies, the caller's {@code orderedBodies}
     * (operations → aliases → conditions, {@code FunctionGenerator.buildStandardModel}) matches the
     * generated file's textual method order for the OPERATIONS-vs-ALIASES split — {@code assignOutput}
     * precedes the alias-method impls — so carrying the claim map forward resolves a collision SPLIT
     * across those bodies exactly like upstream's whole-file render. CONDITION expressions are the
     * one order approximation: the template emits them INSIDE {@code evaluate()} (textually FIRST,
     * ahead of {@code assignOutput}) while they resolve LAST here — immaterial today (ZERO corpus
     * carriers have a sentinel collision split between a condition and another body; a green file
     * cannot carry one — it would be a duplicate same-simple-name import), and a future condition-split
     * carrier would surface as a within-waiver wrong-winner in the regscan, never a green break (facet witnessDupImportCrossBody, PR #345: golden
     * MapEquityOptionPayout claims {@code cdm.product.template.PassThroughItem} in
     * {@code assignOutput} and FQNs the fpml witness in the LATER {@code prm()} alias body; the
     * pre-#345 per-body reset left both bare + BOTH imports — a duplicate same-simple-name import
     * that never compiled, so every cross-body carrier is already waivered → green-safe by
     * construction). The signature types (output/input/superclass + alias-return elements) emit
     * textually AHEAD of every body and stay pre-claimed via {@code seedClaims}, so the abstract
     * declarations' winners are unaffected by the carry.
     *
     * @param orderedBodies the rendered bodies IN FILE ORDER (operations → aliases → conditions;
     *                      the appended alias return-type strings resolve against the same carried
     *                      state — their elements are seeded, so position is immaterial for them)
     * @param seedClaims    simpleName → canonical for types claimed BEFORE every body (the signature
     *                      output/input/superclass types, always emitted first); a body sentinel of a
     *                      seeded simple name with a different canonical is FQN-ed
     * @return the resolved bodies (same size/order) + the suppressed (always-loser) canonicals
     */
    public static Result resolve(List<String> orderedBodies, Map<String, String> seedClaims) {
        Set<String> everBare = new HashSet<>();   // canonicals rendered bare somewhere (a winner)
        Set<String> everFqn = new HashSet<>();    // canonicals rendered FQN somewhere (a loser)
        List<String> out = new ArrayList<>(orderedBodies.size());
        // facet witnessDupImportCrossBody (PR #345): ONE claim map across all bodies (was: reset
        // per body, which left a split-across-body collision unresolved — both bare + duplicate
        // imports, non-compiling). File-order first-claim; the seeds stay first.
        Map<String, String> claimed = new HashMap<>(seedClaims);
        for (String body : orderedBodies) {
            if (!hasSentinel(body)) {
                out.add(body);
                continue;
            }
            // ci-allowlist: regex-on-structured-content (a scan for the resolver's own sentinel
            // delimiters, not for Java syntax — see the class javadoc)
            Matcher m = SENTINEL.matcher(body);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String canonical = m.group(1);
                String simple = simpleOf(canonical);
                String prior = claimed.get(simple);
                String replacement;
                if (prior == null) {
                    claimed.put(simple, canonical);
                    replacement = simple;           // first claim in this body → bare
                    everBare.add(canonical);
                } else if (prior.equals(canonical)) {
                    replacement = simple;           // same canonical → bare (already claimed)
                    everBare.add(canonical);
                } else {
                    replacement = canonical;        // collision in this body → FQN inline
                    everFqn.add(canonical);
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            out.add(sb.toString());
        }
        // Suppress a canonical's import only when it is FQN-ed (a loser) in some body AND never
        // rendered bare (a winner) anywhere — so a canonical that wins one body keeps its import
        // even if a different body FQN-es a same-simple-name sibling.
        Set<String> suppressed = new HashSet<>();
        for (String canonical : everFqn) {
            if (!everBare.contains(canonical)) {
                suppressed.add(canonical);
            }
        }
        return new Result(out, suppressed);
    }

    /**
     * Strip every sentinel to its bare canonical name with NO collision handling — the neutraliser
     * for a caller that needs to read a sentinel-spelt text OUTSIDE the {@code resolve} path
     * ({@link #resolve} handles every sentinel emitted into the operation / alias / condition
     * bodies it scans). Live caller since PR #607: the optimised route's
     * {@code AliasValueSeamPolicy.bareSimpleName}, which compares a seam string's element (spelt
     * as a sentinel when its simple name collides with another import of the file) against the
     * derived witness by canonical name. Idempotent on sentinel-free input.
     */
    public static String stripToBare(String body) {
        if (!hasSentinel(body)) {
            return body;
        }
        // ci-allowlist: regex-on-structured-content (the same sentinel scan as resolve — see the class javadoc)
        Matcher m = SENTINEL.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(simpleOf(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Build an ordered seed-claim map (simpleName → canonical) from the bare-imported
     * signature/field type canonical names emitted ahead of the bodies. The FIRST canonical
     * for a given simple name wins (mirrors the template's top-to-bottom emission); later
     * different-canonical seeds are ignored (they are themselves the #194–#197 anchor cases
     * resolved at their own seats).
     *
     * <p>{@code java.lang} canonicals are SKIPPED: they are never imported (the bare name is
     * implicitly available), so they are never a duplicate-import collision — the green-safety
     * invariant ("a same-simple-name collision was already a duplicate import = a compile error =
     * waivered") does NOT cover them. Seeding {@code java.lang.String} would let a body witness of a
     * (hypothetical) corpus type named {@code String} be FQN-ed against an unimported seed and have
     * its import suppressed, regressing a file that was green pre-facet. No such carrier exists today;
     * the skip closes the latent gap conservatively (a body sentinel colliding with a {@code java.lang}
     * signature type simply stays bare = the pre-facet form, so the file keeps its prior waiver state).
     */
    public static Map<String, String> seedFromCanonicals(List<String> canonicalsInOrder) {
        Map<String, String> seed = new LinkedHashMap<>();
        for (String canonical : canonicalsInOrder) {
            if (canonical == null || canonical.isBlank() || canonical.startsWith("java.lang.")) {
                continue;
            }
            seed.putIfAbsent(simpleOf(canonical), canonical);
        }
        return seed;
    }
}
