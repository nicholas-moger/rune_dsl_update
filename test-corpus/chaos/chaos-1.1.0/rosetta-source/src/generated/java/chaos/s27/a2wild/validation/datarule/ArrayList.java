package chaos.s27.a2wild.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("ArrayList")
@ImplementedBy(ArrayList.Default.class)
public interface ArrayList extends Validator<Integer> {
	
	String NAME = "ArrayList";
	String DEFINITION = "item <> 7";
	
	class Default implements ArrayList {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer array) {
			ComparisonResult result = executeDataRule(array);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Array", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Array", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer array) {
			try {
				return notEqual(MapperS.of(array), MapperS.of(7), CardinalityOperator.Any);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements ArrayList {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer array) {
			return Collections.emptyList();
		}
	}
}
