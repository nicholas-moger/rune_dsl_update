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
import test.deeppathedge.Inner;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("InnerChoice")
@ImplementedBy(InnerChoice.Default.class)
public interface InnerChoice extends Validator<Inner> {
	
	String NAME = "InnerChoice";
	String DEFINITION = "";
	
	class Default implements InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Inner inner) {
			ComparisonResult result = executeDataRule(inner);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Inner", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Inner", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Inner inner) {
			try {
				return choice(MapperS.of(inner), Arrays.asList("CashLeg", "StockLeg"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Inner inner) {
			return Collections.emptyList();
		}
	}
}
