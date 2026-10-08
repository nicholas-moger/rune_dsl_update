package holdout.voiddeeptok.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.voiddeeptok.Holder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("HolderChoice")
@ImplementedBy(HolderChoice.Default.class)
public interface HolderChoice extends Validator<Holder> {
	
	String NAME = "HolderChoice";
	String DEFINITION = "";
	
	class Default implements HolderChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder holder) {
			ComparisonResult result = executeDataRule(holder);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Holder", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Holder", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Holder holder) {
			try {
				return choice(MapperS.of(holder), Arrays.asList("HolderA", "HolderB"), ChoiceRuleValidationMethod.REQUIRED);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements HolderChoice {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder holder) {
			return Collections.emptyList();
		}
	}
}
