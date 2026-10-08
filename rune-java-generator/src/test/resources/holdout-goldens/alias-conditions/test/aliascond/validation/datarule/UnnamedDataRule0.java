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
@RosettaDataRule("UnnamedDataRule0")
@ImplementedBy(UnnamedDataRule0.Default.class)
public interface UnnamedDataRule0 extends Validator<Integer> {
	
	String NAME = "UnnamedDataRule0";
	String DEFINITION = "item > 0";
	
	class Default implements UnnamedDataRule0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer unnamed) {
			ComparisonResult result = executeDataRule(unnamed);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Unnamed", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Unnamed", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer unnamed) {
			try {
				return greaterThan(MapperS.of(unnamed), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements UnnamedDataRule0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer unnamed) {
			return Collections.emptyList();
		}
	}
}
