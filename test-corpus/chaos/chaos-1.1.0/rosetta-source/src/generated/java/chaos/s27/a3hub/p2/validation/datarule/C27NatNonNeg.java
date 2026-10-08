package chaos.s27.a3hub.p2.validation.datarule;

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
@RosettaDataRule("C27NatNonNeg")
@ImplementedBy(C27NatNonNeg.Default.class)
public interface C27NatNonNeg extends Validator<Integer> {
	
	String NAME = "C27NatNonNeg";
	String DEFINITION = "item >= 0";
	
	class Default implements C27NatNonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Nat) {
			ComparisonResult result = executeDataRule(c27Nat);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Nat", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Nat", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer c27Nat) {
			try {
				return greaterThanEquals(MapperS.of(c27Nat), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C27NatNonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Nat) {
			return Collections.emptyList();
		}
	}
}
