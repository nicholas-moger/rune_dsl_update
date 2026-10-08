package test.aliascond.validation.datarule;

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
 * @version 0.0.0
 */
@RosettaDataRule("NestedSmall")
@ImplementedBy(NestedSmall.Default.class)
public interface NestedSmall extends Validator<Integer> {
	
	String NAME = "NestedSmall";
	String DEFINITION = "item < 1000";
	
	class Default implements NestedSmall {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer nested) {
			ComparisonResult result = executeDataRule(nested);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Nested", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Nested", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer nested) {
			try {
				return lessThan(MapperS.of(nested), MapperS.of(1000), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements NestedSmall {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer nested) {
			return Collections.emptyList();
		}
	}
}
