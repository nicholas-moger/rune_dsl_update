package test.aliasreserved.validation.datarule;

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
@RosettaDataRule("Results")
@ImplementedBy(Results.Default.class)
public interface Results extends Validator<Integer> {
	
	String NAME = "Results";
	String DEFINITION = "item >= 0";
	
	class Default implements Results {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer re) {
			ComparisonResult result = executeDataRule(re);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Re", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Re", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer re) {
			try {
				return greaterThanEquals(MapperS.of(re), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Results {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer re) {
			return Collections.emptyList();
		}
	}
}
