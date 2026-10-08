package chaos.s21.a1o4.validation.datarule;

import chaos.s21.a1o4.C21Exactly;
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
@RosettaDataRule("C21ExactlyOneOf0")
@ImplementedBy(C21ExactlyOneOf0.Default.class)
public interface C21ExactlyOneOf0 extends Validator<C21Exactly> {
	
	String NAME = "C21ExactlyOneOf0";
	String DEFINITION = "one-of";
	
	class Default implements C21ExactlyOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Exactly c21Exactly) {
			ComparisonResult result = executeDataRule(c21Exactly);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C21Exactly", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C21Exactly", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C21Exactly c21Exactly) {
			try {
				return choice(MapperS.of(c21Exactly), Arrays.asList("a", "b", "c"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C21ExactlyOneOf0 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Exactly c21Exactly) {
			return Collections.emptyList();
		}
	}
}
