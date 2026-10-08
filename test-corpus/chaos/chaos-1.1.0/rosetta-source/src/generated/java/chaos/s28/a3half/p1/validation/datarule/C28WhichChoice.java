package chaos.s28.a3half.p1.validation.datarule;

import chaos.s28.a3half.p1.C28Which;
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
@RosettaDataRule("C28WhichChoice")
@ImplementedBy(C28WhichChoice.Default.class)
public interface C28WhichChoice extends Validator<C28Which> {
	
	String NAME = "C28WhichChoice";
	String DEFINITION = "";
	
	class Default implements C28WhichChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Which c28Which) {
			ComparisonResult result = executeDataRule(c28Which);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C28Which", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C28Which", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C28Which c28Which) {
			try {
				return choice(MapperS.of(c28Which), Arrays.asList("C28OptA", "C28OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C28WhichChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Which c28Which) {
			return Collections.emptyList();
		}
	}
}
