package chaos.s01.a2alias.validation.datarule;

import chaos.s01.a2alias.C1Cond;
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
@RosettaDataRule("C1CondC1Bounds")
@ImplementedBy(C1CondC1Bounds.Default.class)
public interface C1CondC1Bounds extends Validator<C1Cond> {
	
	String NAME = "C1CondC1Bounds";
	String DEFINITION = "if lo exists and hi exists then lo <= hi";
	
	class Default implements C1CondC1Bounds {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Cond c1Cond) {
			ComparisonResult result = executeDataRule(c1Cond);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C1Cond", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C1Cond", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C1Cond c1Cond) {
			try {
				if (exists(MapperS.of(c1Cond).<BigDecimal>map("getLo", _c1Cond -> _c1Cond.getLo())).andNullSafe(exists(MapperS.of(c1Cond).<BigDecimal>map("getHi", _c1Cond -> _c1Cond.getHi()))).getOrDefault(false)) {
					return lessThanEquals(MapperS.of(c1Cond).<BigDecimal>map("getLo", _c1Cond -> _c1Cond.getLo()), MapperS.of(c1Cond).<BigDecimal>map("getHi", _c1Cond -> _c1Cond.getHi()), CardinalityOperator.All);
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C1CondC1Bounds {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Cond c1Cond) {
			return Collections.emptyList();
		}
	}
}
