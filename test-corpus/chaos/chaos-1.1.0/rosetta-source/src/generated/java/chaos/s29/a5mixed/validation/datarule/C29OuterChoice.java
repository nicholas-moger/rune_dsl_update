package chaos.s29.a5mixed.validation.datarule;

import chaos.s29.a5mixed.C29Outer;
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
@RosettaDataRule("C29OuterChoice")
@ImplementedBy(C29OuterChoice.Default.class)
public interface C29OuterChoice extends Validator<C29Outer> {
	
	String NAME = "C29OuterChoice";
	String DEFINITION = "";
	
	class Default implements C29OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Outer c29Outer) {
			ComparisonResult result = executeDataRule(c29Outer);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Outer", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Outer", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C29Outer c29Outer) {
			try {
				return choice(MapperS.of(c29Outer), Arrays.asList("C29Note", "C29Wrap"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C29OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Outer c29Outer) {
			return Collections.emptyList();
		}
	}
}
