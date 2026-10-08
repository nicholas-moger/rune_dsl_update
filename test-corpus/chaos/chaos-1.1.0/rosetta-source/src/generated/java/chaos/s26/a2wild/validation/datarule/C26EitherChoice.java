package chaos.s26.a2wild.validation.datarule;

import chaos.s26.a2wild.C26Either;
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

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C26EitherChoice")
@ImplementedBy(C26EitherChoice.Default.class)
public interface C26EitherChoice extends Validator<C26Either> {
	
	String NAME = "C26EitherChoice";
	String DEFINITION = "";
	
	class Default implements C26EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Either c26Either) {
			ComparisonResult result = executeDataRule(c26Either);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C26Either", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C26Either", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C26Either c26Either) {
			try {
				return choice(MapperS.of(c26Either), Arrays.asList("C26OptA", "C26OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C26EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Either c26Either) {
			return Collections.emptyList();
		}
	}
}
