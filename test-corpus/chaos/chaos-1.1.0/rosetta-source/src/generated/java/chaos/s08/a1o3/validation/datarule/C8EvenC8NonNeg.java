package chaos.s08.a1o3.validation.datarule;

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
@RosettaDataRule("C8EvenC8NonNeg")
@ImplementedBy(C8EvenC8NonNeg.Default.class)
public interface C8EvenC8NonNeg extends Validator<Integer> {
	
	String NAME = "C8EvenC8NonNeg";
	String DEFINITION = "item >= 0";
	
	class Default implements C8EvenC8NonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c8Even) {
			ComparisonResult result = executeDataRule(c8Even);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C8Even", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C8Even", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer c8Even) {
			try {
				return greaterThanEquals(MapperS.of(c8Even), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C8EvenC8NonNeg {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c8Even) {
			return Collections.emptyList();
		}
	}
}
