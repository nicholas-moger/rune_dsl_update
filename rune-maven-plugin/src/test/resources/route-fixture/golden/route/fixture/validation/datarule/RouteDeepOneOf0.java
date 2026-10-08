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
import route.fixture.RouteDeep;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("RouteDeepOneOf0")
@ImplementedBy(RouteDeepOneOf0.Default.class)
public interface RouteDeepOneOf0 extends Validator<RouteDeep> {
	
	String NAME = "RouteDeepOneOf0";
	String DEFINITION = "one-of";
	
	class Default implements RouteDeepOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteDeep routeDeep) {
			ComparisonResult result = executeDataRule(routeDeep);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteDeep", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteDeep", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(RouteDeep routeDeep) {
			try {
				return choice(MapperS.of(routeDeep), Arrays.asList("a", "b"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements RouteDeepOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteDeep routeDeep) {
			return Collections.emptyList();
		}
	}
}
