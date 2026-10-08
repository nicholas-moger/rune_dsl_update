package chaos.s27.a2qual.validation.datarule;

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
@RosettaDataRule("C27Holder")
@ImplementedBy(C27Holder.Default.class)
public interface C27Holder extends Validator<Integer> {
	
	String NAME = "C27Holder";
	String DEFINITION = "item >= 0";
	
	class Default implements C27Holder {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Hol) {
			ComparisonResult result = executeDataRule(c27Hol);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Hol", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C27Hol", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer c27Hol) {
			try {
				return greaterThanEquals(MapperS.of(c27Hol), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C27Holder {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Hol) {
			return Collections.emptyList();
		}
	}
}
