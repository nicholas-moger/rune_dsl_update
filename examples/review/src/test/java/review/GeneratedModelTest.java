package review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rosetta.model.lib.path.RosettaPath;
import org.junit.jupiter.api.Test;
import route.fixture.RouteBase;
import route.fixture.functions.RouteEcho;
import route.fixture.validation.RouteBaseTypeFormatValidator;

/**
 * Exercises Java generated from the plugin's route fixture on the route selected by the Maven command line
 * (docs/MODES.md): a generated function, and a generated type-format validator with a valid and an invalid value.
 * The generated sources are produced by this module's own {@code generate-sources} phase, never checked in.
 */
final class GeneratedModelTest {
    @Test
    void generatedFunctionReturnsItsInput() {
        var function = new RouteEcho.RouteEchoDefault();
        assertEquals("review", function.evaluate("review"));
        assertEquals("", function.evaluate(""));
    }

    @Test
    void generatedValidatorAcceptsValidName() {
        var results = new RouteBaseTypeFormatValidator().getValidationResults(
                RosettaPath.valueOf("review"), RouteBase.builder().setName("AB").build());
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(result -> result.isSuccess()));
    }

    @Test
    void generatedValidatorRejectsNameOverTheDeclaredLimit() {
        var results = new RouteBaseTypeFormatValidator().getValidationResults(
                RosettaPath.valueOf("review"), RouteBase.builder().setName("ABCD").build());
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result -> !result.isSuccess()));
    }
}
