package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.ir.print.IRPrinter;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Type-subset drift sentinel (F9). The expression-IR type subset — the type name, an
 * {@code RNumberType}'s {@code fractionalDigits}, and the sorted meta-attribute set — is rendered by
 * <strong>two independent</strong> code paths: {@code RTypeFormatter} (the print text form, via
 * {@link IRPrinter}) and {@link IRJsonSerializer}'s {@code typeObject} (the JSON wire form). Both are
 * <em>deliberately</em> a constrained subset (the broader numeric/string/alias constraints are deferred, see
 * each renderer's class javadoc). This test pins that they agree fact-for-fact, so a future change that
 * widens or narrows ONE renderer's subset without the other fails here rather than silently diverging the two
 * surfaces.
 *
 * <p>It is format-agnostic: facts are parsed back out of each rendered form and the two {@link TypeFacts}
 * are compared, not the raw strings. Both renderers are exercised through their public surface — the printer
 * via {@link IRPrinter#print(IRExpr)} (whose header embeds {@code RTypeFormatter.format}) and the serializer
 * via {@link IRJsonSerializer#toJson(IRExpr)} (parsed back with {@link JsonReader}) — over a shared carrier
 * expression so neither path is privileged.
 */
class TypeSubsetCrossPinTest {

    /** The neutral type facts both surfaces are obliged to carry: name, fractionalDigits, sorted meta set. */
    private record TypeFacts(String name, OptionalInt fractionalDigits, List<String> meta) {}

    private static List<RMetaAnnotatedType> fixtures() {
        RNumberType frac2 = new RNumberType(OptionalInt.empty(), OptionalInt.of(2), Optional.empty(), Optional.empty());
        RNumberType frac4 = new RNumberType(OptionalInt.empty(), OptionalInt.of(4), Optional.empty(), Optional.empty());
        return List.of(
                RMetaAnnotatedType.withNoMeta(RBasicType.BOOLEAN),
                RMetaAnnotatedType.withNoMeta(RStringType.unconstrained()),
                RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained()),
                RMetaAnnotatedType.withNoMeta(RNumberType.intType()),       // name "int", fractionalDigits 0
                RMetaAnnotatedType.withNoMeta(frac2),                        // number<fractionalDigits=2>
                RMetaAnnotatedType.withMeta(RBasicType.BOOLEAN, List.of("scheme", "location")), // meta only
                RMetaAnnotatedType.withMeta(frac4, List.of("scheme", "location")));             // frac + meta
    }

    @Test
    void printerAndSerializerCarryTheSameTypeFacts() {
        for (RMetaAnnotatedType type : fixtures()) {
            assertEquals(serializerFacts(type), printerFacts(type),
                    "printer vs serializer type-fact drift for " + type);
        }
    }

    // ---- printer side: parse the type token out of the rendered expression header --------------------------

    private static TypeFacts printerFacts(RMetaAnnotatedType type) {
        String header = new IRPrinter().print(carrier(type)).strip();
        // header = `<label> : <typeToken> [<cardinality>, <optionality>]`; the only " [" is the cardinality
        // bracket (a meta token uses "meta[" with no preceding space), so split between " : " and the last " [".
        String token = header.substring(header.indexOf(" : ") + 3, header.lastIndexOf(" ["));
        return parseToken(token);
    }

    private static TypeFacts parseToken(String token) {
        String rest = token;
        List<String> meta = List.of();
        int metaIdx = rest.indexOf(" meta[");
        if (metaIdx >= 0) {
            String body = rest.substring(metaIdx + " meta[".length(), rest.length() - 1); // drop trailing ']'
            meta = body.isEmpty() ? List.of() : List.of(body.split(", "));
            rest = rest.substring(0, metaIdx);
        }
        OptionalInt frac = OptionalInt.empty();
        int fracIdx = rest.indexOf("<fractionalDigits=");
        if (fracIdx >= 0) {
            String n = rest.substring(fracIdx + "<fractionalDigits=".length(), rest.length() - 1); // drop '>'
            frac = OptionalInt.of(Integer.parseInt(n));
            rest = rest.substring(0, fracIdx);
        }
        return new TypeFacts(rest, frac, meta);
    }

    // ---- serializer side: parse the "type" object out of the serialized expression -----------------------

    private static TypeFacts serializerFacts(RMetaAnnotatedType type) {
        Json.Obj root = (Json.Obj) JsonReader.parse(new IRJsonSerializer().toJson(carrier(type)));
        Json.Obj typeObject = (Json.Obj) required(root, "type");
        String name = ((Json.Str) required(typeObject, "name")).value();
        OptionalInt frac = OptionalInt.empty();
        Json fracJson = optional(typeObject, "fractionalDigits");
        if (fracJson != null) {
            frac = OptionalInt.of(Integer.parseInt(((Json.Raw) fracJson).token()));
        }
        List<String> meta = new ArrayList<>();
        Json metaJson = optional(typeObject, "meta");
        if (metaJson != null) {
            for (Json element : ((Json.Arr) metaJson).elements()) {
                meta.add(((Json.Str) element).value());
            }
        }
        return new TypeFacts(name, frac, List.copyOf(meta));
    }

    private static Json required(Json.Obj obj, String name) {
        Json value = optional(obj, name);
        if (value == null) {
            throw new AssertionError("missing JSON member: " + name);
        }
        return value;
    }

    private static Json optional(Json.Obj obj, String name) {
        for (Json.Member member : obj.members()) {
            if (member.name().equals(name)) {
                return member.value();
            }
        }
        return null;
    }

    /** A minimal, total-PRESENT carrier expression that simply transports the type under test. */
    private static IRExpr carrier(RMetaAnnotatedType type) {
        return new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT, type,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
    }
}
