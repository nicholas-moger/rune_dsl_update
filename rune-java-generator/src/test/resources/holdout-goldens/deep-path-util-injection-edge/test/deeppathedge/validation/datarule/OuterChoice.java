package test.deeppathedge.validation.datarule;

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
import test.deeppathedge.Outer;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("OuterChoice")
@ImplementedBy(OuterChoice.Default.class)
public interface OuterChoice extends Validator<Outer> {
	
	String NAME = "OuterChoice";
	String DEFINITION = "";
	
	class Default implements OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer outer) {
			ComparisonResult result = executeDataRule(outer);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Outer", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Outer", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Outer outer) {
			try {
				return choice(MapperS.of(outer), Arrays.asList("Wrap", "Note"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements OuterChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer outer) {
			return Collections.emptyList();
		}
	}
}
