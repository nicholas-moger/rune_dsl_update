package test.chswitchbare.validation.datarule;

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
import test.chswitchbare.Outer2;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("Outer2Choice")
@ImplementedBy(Outer2Choice.Default.class)
public interface Outer2Choice extends Validator<Outer2> {
	
	String NAME = "Outer2Choice";
	String DEFINITION = "";
	
	class Default implements Outer2Choice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer2 outer2) {
			ComparisonResult result = executeDataRule(outer2);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Outer2", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Outer2", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Outer2 outer2) {
			try {
				return choice(MapperS.of(outer2), Arrays.asList("OptA", "OptB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Outer2Choice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer2 outer2) {
			return Collections.emptyList();
		}
	}
}
