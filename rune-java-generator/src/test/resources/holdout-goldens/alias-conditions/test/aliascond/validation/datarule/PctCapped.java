package test.aliascond.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PctCapped")
@ImplementedBy(PctCapped.Default.class)
public interface PctCapped extends Validator<BigDecimal> {
	
	String NAME = "PctCapped";
	String DEFINITION = "item <= 100";
	
	class Default implements PctCapped {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, BigDecimal pct) {
			ComparisonResult result = executeDataRule(pct);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Pct", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Pct", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(BigDecimal pct) {
			try {
				return lessThanEquals(MapperS.of(pct), MapperS.of(BigDecimal.valueOf(100)), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PctCapped {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, BigDecimal pct) {
			return Collections.emptyList();
		}
	}
}
