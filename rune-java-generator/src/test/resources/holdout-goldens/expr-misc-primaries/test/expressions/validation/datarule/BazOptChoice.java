package test.expressions.validation.datarule;

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
import test.expressions.Baz;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("BazOptChoice")
@ImplementedBy(BazOptChoice.Default.class)
public interface BazOptChoice extends Validator<Baz> {
	
	String NAME = "BazOptChoice";
	String DEFINITION = "optional choice x, y";
	
	class Default implements BazOptChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Baz baz) {
			ComparisonResult result = executeDataRule(baz);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Baz", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Baz", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Baz baz) {
			try {
				return choice(MapperS.of(baz), Arrays.asList("x", "y"), ChoiceRuleValidationMethod.OPTIONAL);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements BazOptChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Baz baz) {
			return Collections.emptyList();
		}
	}
}
