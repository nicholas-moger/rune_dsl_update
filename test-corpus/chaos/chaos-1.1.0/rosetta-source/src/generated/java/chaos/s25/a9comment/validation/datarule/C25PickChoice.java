package chaos.s25.a9comment.validation.datarule;

import chaos.s25.a9comment.C25Pick;
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
@RosettaDataRule("C25PickChoice")
@ImplementedBy(C25PickChoice.Default.class)
public interface C25PickChoice extends Validator<C25Pick> {
	
	String NAME = "C25PickChoice";
	String DEFINITION = "";
	
	class Default implements C25PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Pick c25Pick) {
			ComparisonResult result = executeDataRule(c25Pick);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C25Pick", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C25Pick", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C25Pick c25Pick) {
			try {
				return choice(MapperS.of(c25Pick), Arrays.asList("C25OptA", "C25OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C25PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Pick c25Pick) {
			return Collections.emptyList();
		}
	}
}
