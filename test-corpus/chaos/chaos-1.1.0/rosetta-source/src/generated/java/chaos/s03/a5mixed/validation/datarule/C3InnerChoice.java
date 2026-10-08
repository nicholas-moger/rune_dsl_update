package chaos.s03.a5mixed.validation.datarule;

import chaos.s03.a5mixed.C3Inner;
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
@RosettaDataRule("C3InnerChoice")
@ImplementedBy(C3InnerChoice.Default.class)
public interface C3InnerChoice extends Validator<C3Inner> {
	
	String NAME = "C3InnerChoice";
	String DEFINITION = "";
	
	class Default implements C3InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Inner c3Inner) {
			ComparisonResult result = executeDataRule(c3Inner);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Inner", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C3Inner", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C3Inner c3Inner) {
			try {
				return choice(MapperS.of(c3Inner), Arrays.asList("C3CashLeg", "C3StockLeg"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C3InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Inner c3Inner) {
			return Collections.emptyList();
		}
	}
}
