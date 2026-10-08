package test.reservednames.validation.datarule;

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
import test.reservednames.Result;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("ResultValueExists")
@ImplementedBy(ResultValueExists.Default.class)
public interface ResultValueExists extends Validator<Result> {
	
	String NAME = "ResultValueExists";
	String DEFINITION = "value exists";
	
	class Default implements ResultValueExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Result result) {
			ComparisonResult _result = executeDataRule(result);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Result", path, DEFINITION));
			}
			
			String failureMessage = _result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Result", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Result result) {
			try {
				return exists(MapperS.of(result).<String>map("getValue", _result -> _result.getValue()));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements ResultValueExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Result result) {
			return Collections.emptyList();
		}
	}
}
