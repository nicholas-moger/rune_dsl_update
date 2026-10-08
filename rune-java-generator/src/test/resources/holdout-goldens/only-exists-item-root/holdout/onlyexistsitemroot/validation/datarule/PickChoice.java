package holdout.onlyexistsitemroot.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Pick;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PickChoice")
@ImplementedBy(PickChoice.Default.class)
public interface PickChoice extends Validator<Pick> {
	
	String NAME = "PickChoice";
	String DEFINITION = "";
	
	class Default implements PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pick pick) {
			ComparisonResult result = executeDataRule(pick);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Pick", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Pick", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Pick pick) {
			try {
				return choice(MapperS.of(pick), Arrays.asList("OptA", "OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pick pick) {
			return Collections.emptyList();
		}
	}
}
