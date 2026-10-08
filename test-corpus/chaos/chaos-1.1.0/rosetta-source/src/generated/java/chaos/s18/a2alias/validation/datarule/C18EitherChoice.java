package chaos.s18.a2alias.validation.datarule;

import chaos.s18.a2alias.C18Either;
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
@RosettaDataRule("C18EitherChoice")
@ImplementedBy(C18EitherChoice.Default.class)
public interface C18EitherChoice extends Validator<C18Either> {
	
	String NAME = "C18EitherChoice";
	String DEFINITION = "";
	
	class Default implements C18EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Either c18Either) {
			ComparisonResult result = executeDataRule(c18Either);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C18Either", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C18Either", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C18Either c18Either) {
			try {
				return choice(MapperS.of(c18Either), Arrays.asList("C18OptA", "C18OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C18EitherChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Either c18Either) {
			return Collections.emptyList();
		}
	}
}
