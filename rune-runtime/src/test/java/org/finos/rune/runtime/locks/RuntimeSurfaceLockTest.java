package org.finos.rune.runtime.locks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.mapper.MapperS;

/**
 * Locks for the fork-owned runtime's drop-in contract (U018, Leg R / R2 of the
 * drop-in parity program).
 *
 * <p>The module's source basis is the RELEASED
 * {@code org.finos.rune:rune-runtime:9.83.0} sources jar taken verbatim; the
 * full ABI receipt against the released binary jar (class-entry set 227/227,
 * javap -p declaration surface 2,454/2,454 lines identical both directions)
 * is the banked {@code target-459-r2-abi-javap.log}. These locks pin the
 * pieces of that contract that must survive WITHOUT the released jar present:
 * the byte-identity of the three classpath resources (the maven plugin's
 * builtin-model channel reads {@code model/*.rosetta} from this jar), the
 * de-xtend deviation in {@link MapperMaths} (no {@code org.eclipse} references
 * may reappear), and the released message bytes on the error paths that the
 * de-xtend edit rebuilt with {@link StringBuilder}.
 */
class RuntimeSurfaceLockTest {

    /** SHA-256 of the released 9.83.0 binary jar's copies (byte-for-byte). */
    private static final String ANNOTATIONS_SHA256 =
            "7e9f1bdf8e22d585f67665ddc16fee1e444ff930f98541f696c587a74e1cfcf1";
    private static final String BASICTYPES_SHA256 =
            "7c608118f1e594187a2e891de73aed5a2e5e898c86ec4980cdf6749224d4ccf4";
    private static final String FORMATTING_OPTIONS_SHA256 =
            "66b741f8f2b33eeea356dee662770c57cd05f333622b47863bca693043307f7d";

    @Test
    void annotationsRosettaIsByteIdenticalToReleased() throws Exception {
        assertEquals(ANNOTATIONS_SHA256, sha256OfResource("model/annotations.rosetta"));
    }

    @Test
    void basictypesRosettaIsByteIdenticalToReleased() throws Exception {
        assertEquals(BASICTYPES_SHA256, sha256OfResource("model/basictypes.rosetta"));
    }

    @Test
    void formattingOptionsJsonIsByteIdenticalToReleased() throws Exception {
        assertEquals(FORMATTING_OPTIONS_SHA256, sha256OfResource("default-formatting-options.json"));
    }

    /**
     * Contract pin (not a differential witness): the de-xtend edit removed the
     * runtime's only {@code org.eclipse} references; this scans the compiled
     * {@code MapperMaths.class} bytes so an accidental reintroduction of
     * xtend/xbase types fails loudly.
     */
    @Test
    void mapperMathsCarriesNoEclipseReferences() throws Exception {
        byte[] classBytes = bytesOfResource("com/rosetta/model/lib/expression/MapperMaths.class");
        String haystack = new String(classBytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(!haystack.contains("org/eclipse"),
                "MapperMaths.class must not reference org.eclipse types after the de-xtend edit");
    }

    @Test
    void mapperMathsAddKeepsReleasedMessageBytes() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> MapperMaths.add(MapperS.of("a"), MapperS.of(BigDecimal.ONE)).get());
        assertEquals("Cant add two random (String, BigDecimal) together", e.getMessage());
    }

    @Test
    void mapperMathsSubtractKeepsReleasedMessageBytes() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> MapperMaths.subtract(MapperS.of("a"), MapperS.of("b")).get());
        assertEquals("Cant subtract two strings together", e.getMessage());
    }

    @Test
    void mapperMathsDivideKeepsReleasedMessageBytes() {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> MapperMaths.divide(MapperS.of("a"), MapperS.of("b")).get());
        assertEquals("Cant divide two strings", e.getMessage());
    }

    @Test
    void mapperMathsAddStillComputes() {
        assertEquals(Integer.valueOf(3), MapperMaths.<Integer, Integer, Integer>add(
                MapperS.of(Integer.valueOf(1)), MapperS.of(Integer.valueOf(2))).get());
    }

    /**
     * The outer-class census: every package of the released jar is present with
     * a representative class resolvable from this module's own output. The full
     * 148-class inventory is proven by the banked javap receipt; this spot
     * check keeps a per-package floor without needing the released jar.
     */
    @Test
    void everyReleasedPackageIsPresent() {
        List<String> representatives = Arrays.asList(
                "com.regnosys.rosetta.lib.labelprovider.GraphBasedLabelProvider",
                "com.rosetta.lib.postprocess.PostProcessorReport",
                "com.rosetta.model.lib.RosettaModelObject",
                "com.rosetta.model.lib.annotations.RosettaDataType",
                "com.rosetta.model.lib.expression.ComparisonResult",
                "com.rosetta.model.lib.flatten.ModelObjectFlattener",
                "com.rosetta.model.lib.functions.RosettaFunction",
                "com.rosetta.model.lib.mapper.MapperS",
                "com.rosetta.model.lib.meta.RosettaMetaData",
                "com.rosetta.model.lib.path.RosettaPath",
                "com.rosetta.model.lib.process.PostProcessStep",
                "com.rosetta.model.lib.qualify.QualifyResult",
                "com.rosetta.model.lib.records.Date",
                "com.rosetta.model.lib.reports.Tabulator",
                "com.rosetta.model.lib.validation.ValidationResult",
                "com.rosetta.model.metafields.MetaFields",
                "com.rosetta.util.DottedPath",
                "com.rosetta.util.types.JavaType",
                "com.rosetta.util.types.generated.GeneratedJavaClass");
        for (String fqn : representatives) {
            try {
                assertNotNull(Class.forName(fqn));
            } catch (ClassNotFoundException e) {
                throw new AssertionError("released-package representative missing: " + fqn, e);
            }
        }
    }

    private static String sha256OfResource(String name) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytesOfResource(name));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] bytesOfResource(String name) throws IOException {
        try (InputStream in = RuntimeSurfaceLockTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "resource missing from the module classpath: " + name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
