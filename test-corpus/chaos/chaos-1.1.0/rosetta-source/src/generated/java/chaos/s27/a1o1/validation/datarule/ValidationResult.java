package chaos.s27.a1o1.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("ValidationResult")
@ImplementedBy(ValidationResult.Default.class)
public interface ValidationResult extends Validator<Integer> {
	
	String NAME = "ValidationResult";
	String DEFINITION = "item <= 1000000";
	
	class Default implements ValidationResult {
	
		@Override
		public List<com.rosetta.model.lib.validation.ValidationResult<?>> getValidationResults(RosettaPath path, Integer validation) {
			ComparisonResult result = executeDataRule(validation);
			if (result.getOrDefault(true)) {
				return Arrays.asList(com.rosetta.model.lib.validation.ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Validation", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(com.rosetta.model.lib.validation.ValidationResult.failure(NAME, com.rosetta.model.lib.validation.ValidationResult.ValidationType.DATA_RULE, "Validation", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer validation) {
			try {
				return lessThanEquals(MapperS.of(validation), MapperS.of(1000000), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements ValidationResult {
	
		@Override
		public List<com.rosetta.model.lib.validation.ValidationResult<?>> getValidationResults(RosettaPath path, Integer validation) {
			return Collections.emptyList();
		}
	}
}
