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
import route.fixture.RouteChoice;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("RouteChoiceChoice")
@ImplementedBy(RouteChoiceChoice.Default.class)
public interface RouteChoiceChoice extends Validator<RouteChoice> {
	
	String NAME = "RouteChoiceChoice";
	String DEFINITION = "";
	
	class Default implements RouteChoiceChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteChoice routeChoice) {
			ComparisonResult result = executeDataRule(routeChoice);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteChoice", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteChoice", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(RouteChoice routeChoice) {
			try {
				return choice(MapperS.of(routeChoice), Arrays.asList("RouteChild", "RouteDeep"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements RouteChoiceChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteChoice routeChoice) {
			return Collections.emptyList();
		}
	}
}
