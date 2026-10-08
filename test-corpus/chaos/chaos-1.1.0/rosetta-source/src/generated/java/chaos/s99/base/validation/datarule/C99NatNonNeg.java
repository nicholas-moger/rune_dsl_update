package chaos.s99.base.validation.datarule;

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
@RosettaDataRule("C99NatNonNeg")
@ImplementedBy(C99NatNonNeg.Default.class)
public interface C99NatNonNeg extends Validator<Integer> {
	
	String NAME = "C99NatNonNeg";
	String DEFINITION = "item >= 0";
	
	class Default implements C99NatNonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c99Nat) {
			ComparisonResult result = executeDataRule(c99Nat);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C99Nat", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C99Nat", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer c99Nat) {
			try {
				return greaterThanEquals(MapperS.of(c99Nat), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C99NatNonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c99Nat) {
			return Collections.emptyList();
		}
	}
}
