package route.fixture.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import route.fixture.RouteAltB;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("RouteAltBOneOf0")
@ImplementedBy(RouteAltBOneOf0.Default.class)
public interface RouteAltBOneOf0 extends Validator<RouteAltB> {
	
	String NAME = "RouteAltBOneOf0";
	String DEFINITION = "one-of";
	
	class Default implements RouteAltBOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteAltB routeAltB) {
			ComparisonResult result = executeDataRule(routeAltB);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteAltB", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteAltB", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(RouteAltB routeAltB) {
			try {
				return choice(MapperS.of(routeAltB), Arrays.asList("shared", "q"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements RouteAltBOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteAltB routeAltB) {
			return Collections.emptyList();
		}
	}
}
