package chaos.s27.a3hub.p2.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("Inject")
@ImplementedBy(Inject.Default.class)
public interface Inject extends Validator<Integer> {
	
	String NAME = "Inject";
	String DEFINITION = "item >= 0";
	
	class Default implements Inject {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer in) {
			ComparisonResult result = executeDataRule(in);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "In", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "In", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer in) {
			try {
				return greaterThanEquals(MapperS.of(in), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Inject {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer in) {
			return Collections.emptyList();
		}
	}
}
