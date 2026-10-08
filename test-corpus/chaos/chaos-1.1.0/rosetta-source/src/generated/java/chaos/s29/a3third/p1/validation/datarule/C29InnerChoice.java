package chaos.s29.a3third.p1.validation.datarule;

import chaos.s29.a3third.p1.C29Inner;
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
@RosettaDataRule("C29InnerChoice")
@ImplementedBy(C29InnerChoice.Default.class)
public interface C29InnerChoice extends Validator<C29Inner> {
	
	String NAME = "C29InnerChoice";
	String DEFINITION = "";
	
	class Default implements C29InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Inner c29Inner) {
			ComparisonResult result = executeDataRule(c29Inner);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Inner", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C29Inner", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C29Inner c29Inner) {
			try {
				return choice(MapperS.of(c29Inner), Arrays.asList("C29In1", "C29In2"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C29InnerChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Inner c29Inner) {
			return Collections.emptyList();
		}
	}
}
