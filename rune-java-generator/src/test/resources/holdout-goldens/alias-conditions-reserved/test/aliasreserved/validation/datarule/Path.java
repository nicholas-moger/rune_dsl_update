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
@RosettaDataRule("Path")
@ImplementedBy(Path.Default.class)
public interface Path extends Validator<Integer> {
	
	String NAME = "Path";
	String DEFINITION = "item >= 0";
	
	class Default implements Path {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer pa) {
			ComparisonResult result = executeDataRule(pa);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Pa", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Pa", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer pa) {
			try {
				return greaterThanEquals(MapperS.of(pa), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Path {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer pa) {
			return Collections.emptyList();
		}
	}
}
