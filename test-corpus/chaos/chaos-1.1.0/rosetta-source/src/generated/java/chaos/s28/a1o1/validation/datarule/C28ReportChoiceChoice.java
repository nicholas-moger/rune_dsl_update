package chaos.s28.a1o1.validation.datarule;

import chaos.s28.a1o1.C28ReportChoice;
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
@RosettaDataRule("C28ReportChoiceChoice")
@ImplementedBy(C28ReportChoiceChoice.Default.class)
public interface C28ReportChoiceChoice extends Validator<C28ReportChoice> {
	
	String NAME = "C28ReportChoiceChoice";
	String DEFINITION = "";
	
	class Default implements C28ReportChoiceChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ReportChoice c28ReportChoice) {
			ComparisonResult result = executeDataRule(c28ReportChoice);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C28ReportChoice", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C28ReportChoice", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C28ReportChoice c28ReportChoice) {
			try {
				return choice(MapperS.of(c28ReportChoice), Arrays.asList("C28ChoiceReport", "C28Report"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C28ReportChoiceChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ReportChoice c28ReportChoice) {
			return Collections.emptyList();
		}
	}
}
