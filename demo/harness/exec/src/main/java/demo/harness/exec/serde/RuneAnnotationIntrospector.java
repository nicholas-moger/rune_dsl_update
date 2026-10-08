package demo.harness.exec.serde;

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaIgnore;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneIgnore;

/**
 * Teaches a plain Jackson {@link com.fasterxml.jackson.databind.ObjectMapper} to read the
 * RUNTIME'S OWN annotations, so the fallback serialiser emits Rune property names rather
 * than bean names.
 *
 * <p>Why this exists at all: the generated POJOs carry no Jackson annotations whatsoever -
 * they carry {@code @RuneAttribute} / {@code @RosettaAttribute} (and the deprecated-but-still
 * emitted pair) on the implementation class's getters. Without this introspector Jackson
 * would name the ReferenceWithMeta properties {@code value} / {@code globalReference}; with
 * it they come out as the real Rune names {@code @data} / {@code @ref}, which is what a
 * reader of the demo's sample JSON expects to see.
 *
 * <p>Scope is deliberately narrow - names and ignore markers only. It is paired with the
 * standard {@code JacksonAnnotationIntrospector} (see {@link RuneJson}), which keeps every
 * other Jackson behaviour at its default; returning null here simply defers.
 *
 * <p>This is the FALLBACK path. When rosetta-common's canonical {@code RosettaObjectMapper}
 * is on the classpath it is used instead, and which one ran is reported in a ##DEMO## metric
 * and in the receipt notes - never inferred by the reader.
 */
@SuppressWarnings("deprecation") // @RosettaAttribute is deprecated upstream but still emitted
public final class RuneAnnotationIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        RuneAttribute rune = a.getAnnotation(RuneAttribute.class);
        if (rune != null && !rune.value().isEmpty()) {
            return PropertyName.construct(rune.value());
        }
        RosettaAttribute rosetta = a.getAnnotation(RosettaAttribute.class);
        if (rosetta != null && !rosetta.value().isEmpty()) {
            return PropertyName.construct(rosetta.value());
        }
        return null; // defer to the paired Jackson introspector
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        // Symmetric with serialization so a tree written here reads back under the same
        // names, should the integrator ever wire a full model round-trip.
        return findNameForSerialization(a);
    }

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return m.getAnnotation(RuneIgnore.class) != null
                || m.getAnnotation(RosettaIgnore.class) != null;
    }
}
