package chaos.s99.base.validation.datarule;

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
@RosettaDataRule("Integer")
@ImplementedBy(Integer.Default.class)
public interface Integer extends Validator<java.lang.Integer> {
	
	String NAME = "Integer";
	String DEFINITION = "item <> 8";
	
	class Default implements Integer {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, java.lang.Integer _int) {
			ComparisonResult result = executeDataRule(_int);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Int", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Int", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(java.lang.Integer _int) {
			try {
				return notEqual(MapperS.of(_int), MapperS.of(8), CardinalityOperator.Any);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Integer {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, java.lang.Integer _int) {
			return Collections.emptyList();
		}
	}
}
