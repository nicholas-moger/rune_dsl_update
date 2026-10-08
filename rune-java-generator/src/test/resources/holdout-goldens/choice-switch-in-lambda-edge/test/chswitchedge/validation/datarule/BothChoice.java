package test.chswitchedge.validation.datarule;

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
import test.chswitchedge.Both;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("BothChoice")
@ImplementedBy(BothChoice.Default.class)
public interface BothChoice extends Validator<Both> {
	
	String NAME = "BothChoice";
	String DEFINITION = "";
	
	class Default implements BothChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Both both) {
			ComparisonResult result = executeDataRule(both);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Both", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Both", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Both both) {
			try {
				return choice(MapperS.of(both), Arrays.asList("Either", "OptC"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements BothChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Both both) {
			return Collections.emptyList();
		}
	}
}
