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
@RosettaDataRule("Code3NotPlaceholder")
@ImplementedBy(Code3NotPlaceholder.Default.class)
public interface Code3NotPlaceholder extends Validator<String> {
	
	String NAME = "Code3NotPlaceholder";
	String DEFINITION = "item <> \"XXX\"";
	
	class Default implements Code3NotPlaceholder {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, String code3) {
			ComparisonResult result = executeDataRule(code3);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Code3", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Code3", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(String code3) {
			try {
				return notEqual(MapperS.of(code3), MapperS.of("XXX"), CardinalityOperator.Any);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Code3NotPlaceholder {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, String code3) {
			return Collections.emptyList();
		}
	}
}
