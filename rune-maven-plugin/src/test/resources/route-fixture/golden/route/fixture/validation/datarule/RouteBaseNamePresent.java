package route.fixture.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import route.fixture.RouteBase;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("RouteBaseNamePresent")
@ImplementedBy(RouteBaseNamePresent.Default.class)
public interface RouteBaseNamePresent extends Validator<RouteBase> {
	
	String NAME = "RouteBaseNamePresent";
	String DEFINITION = "name exists";
	
	class Default implements RouteBaseNamePresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteBase routeBase) {
			ComparisonResult result = executeDataRule(routeBase);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteBase", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "RouteBase", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(RouteBase routeBase) {
			try {
				return exists(MapperS.of(routeBase).<String>map("getName", _routeBase -> _routeBase.getName()));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements RouteBaseNamePresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteBase routeBase) {
			return Collections.emptyList();
		}
	}
}
