package test.fmeta026.validation.datarule;

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
import test.fmeta026.A;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("AChoice")
@ImplementedBy(AChoice.Default.class)
public interface AChoice extends Validator<A> {
	
	String NAME = "AChoice";
	String DEFINITION = "";
	
	class Default implements AChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, A a) {
			ComparisonResult result = executeDataRule(a);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "A", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "A", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(A a) {
			try {
				return choice(MapperS.of(a), Arrays.asList("B", "C"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements AChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, A a) {
			return Collections.emptyList();
		}
	}
}
