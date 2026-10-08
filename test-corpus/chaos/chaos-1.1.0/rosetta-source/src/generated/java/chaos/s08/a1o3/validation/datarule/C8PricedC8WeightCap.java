package chaos.s08.a1o3.validation.datarule;

import chaos.s08.a1o3.C8Priced;
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
 * @version 1.0.0
 */
@RosettaDataRule("C8PricedC8WeightCap")
@ImplementedBy(C8PricedC8WeightCap.Default.class)
public interface C8PricedC8WeightCap extends Validator<C8Priced> {
	
	String NAME = "C8PricedC8WeightCap";
	String DEFINITION = "if weights exists then weights sum <= 300";
	
	class Default implements C8PricedC8WeightCap {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C8Priced c8Priced) {
			ComparisonResult result = executeDataRule(c8Priced);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C8Priced", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C8Priced", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C8Priced c8Priced) {
			try {
				if (exists(MapperS.of(c8Priced).<BigDecimal>mapC("getWeights", _c8Priced -> _c8Priced.getWeights())).getOrDefault(false)) {
					return lessThanEquals(MapperS.of(c8Priced).<BigDecimal>mapC("getWeights", _c8Priced -> _c8Priced.getWeights())
						.sumBigDecimal(), MapperS.of(BigDecimal.valueOf(300)), CardinalityOperator.All);
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C8PricedC8WeightCap {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C8Priced c8Priced) {
			return Collections.emptyList();
		}
	}
}
