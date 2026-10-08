package chaos.s01.a3half.p2.validation.datarule;

import chaos.s01.a3half.p2.C1Cond;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
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
@RosettaDataRule("C1CondDataRule1")
@ImplementedBy(C1CondDataRule1.Default.class)
public interface C1CondDataRule1 extends Validator<C1Cond> {
	
	String NAME = "C1CondDataRule1";
	String DEFINITION = "lo exists or hi exists";
	
	class Default implements C1CondDataRule1 {
	
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
				return exists(MapperS.of(c1Cond).<BigDecimal>map("getLo", _c1Cond -> _c1Cond.getLo())).orNullSafe(exists(MapperS.of(c1Cond).<BigDecimal>map("getHi", _c1Cond -> _c1Cond.getHi())));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C1CondDataRule1 {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Cond c1Cond) {
			return Collections.emptyList();
		}
	}
}
