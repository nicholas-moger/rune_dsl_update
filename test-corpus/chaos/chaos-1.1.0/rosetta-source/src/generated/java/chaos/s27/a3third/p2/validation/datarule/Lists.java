package chaos.s27.a3third.p2.validation.datarule;

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
@RosettaDataRule("Lists")
@ImplementedBy(Lists.Default.class)
public interface Lists extends Validator<Integer> {
	
	String NAME = "Lists";
	String DEFINITION = "item >= 0";
	
	class Default implements Lists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer l) {
			ComparisonResult result = executeDataRule(l);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "L", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "L", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer l) {
			try {
				return greaterThanEquals(MapperS.of(l), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements Lists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer l) {
			return Collections.emptyList();
		}
	}
}
