package chaos.s16.a5uni.validation.datarule;

import chaos.s16.a5uni.C16Pick;
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
@RosettaDataRule("C16PickChoice")
@ImplementedBy(C16PickChoice.Default.class)
public interface C16PickChoice extends Validator<C16Pick> {
	
	String NAME = "C16PickChoice";
	String DEFINITION = "";
	
	class Default implements C16PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Pick c16Pick) {
			ComparisonResult result = executeDataRule(c16Pick);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C16Pick", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C16Pick", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C16Pick c16Pick) {
			try {
				return choice(MapperS.of(c16Pick), Arrays.asList("C16Alpha", "C16Beta"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C16PickChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Pick c16Pick) {
			return Collections.emptyList();
		}
	}
}
