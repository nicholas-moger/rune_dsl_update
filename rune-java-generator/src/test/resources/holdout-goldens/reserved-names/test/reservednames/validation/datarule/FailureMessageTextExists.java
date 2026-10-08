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
import test.reservednames.FailureMessage;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("FailureMessageTextExists")
@ImplementedBy(FailureMessageTextExists.Default.class)
public interface FailureMessageTextExists extends Validator<FailureMessage> {
	
	String NAME = "FailureMessageTextExists";
	String DEFINITION = "text exists";
	
	class Default implements FailureMessageTextExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, FailureMessage failureMessage) {
			ComparisonResult result = executeDataRule(failureMessage);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "FailureMessage", path, DEFINITION));
			}
			
			String _failureMessage = result.getError();
			if (_failureMessage == null || _failureMessage.contains("Null") || _failureMessage == "") {
				_failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "FailureMessage", path, DEFINITION, _failureMessage));
		}
		
		private ComparisonResult executeDataRule(FailureMessage failureMessage) {
			try {
				return exists(MapperS.of(failureMessage).<String>map("getText", _failureMessage -> _failureMessage.getText()));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements FailureMessageTextExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, FailureMessage failureMessage) {
			return Collections.emptyList();
		}
	}
}
