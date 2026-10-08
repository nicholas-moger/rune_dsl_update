package chaos.s03.a2qual.validation.datarule;

import chaos.s03.a2qual.C3Outer;
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
@RosettaDataRule("C3OuterChoice")
@ImplementedBy(C3OuterChoice.Default.class)
public interface C3OuterChoice extends Validator<C3Outer> {
	
	String NAME = "C3OuterChoice";
	String DEFINITION = "";
	
	class Default implements C3OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Outer c3Outer) {
			ComparisonResult result = executeDataRule(c3Outer);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Outer", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Outer", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C3Outer c3Outer) {
			try {
				return choice(MapperS.of(c3Outer), Arrays.asList("C3Wrap", "C3Note"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C3OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Outer c3Outer) {
			return Collections.emptyList();
		}
	}
}
