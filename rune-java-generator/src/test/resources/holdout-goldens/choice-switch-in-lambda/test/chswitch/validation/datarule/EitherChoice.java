package test.chswitch.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import test.chswitch.Either;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("EitherChoice")
@ImplementedBy(EitherChoice.Default.class)
public interface EitherChoice extends Validator<Either> {
	
	String NAME = "EitherChoice";
	String DEFINITION = "";
	
	class Default implements EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Either either) {
			ComparisonResult result = executeDataRule(either);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Either", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Either", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Either either) {
			try {
				return choice(MapperS.of(either), Arrays.asList("OptA", "OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Either either) {
			return Collections.emptyList();
		}
	}
}
