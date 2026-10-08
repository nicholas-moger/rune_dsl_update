package holdout.argcoercionbarelocal.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.argcoercionbarelocal.functions.Check;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


/**
 * @version 0.0.0
 */
@RosettaDataRule("NatCalled")
@ImplementedBy(NatCalled.Default.class)
public interface NatCalled extends Validator<Integer> {
	
	String NAME = "NatCalled";
	String DEFINITION = "Check(item)";
	
	class Default implements NatCalled {
	
		@Inject protected Check check;
		
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer nat) {
			ComparisonResult result = executeDataRule(nat);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Nat", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Nat", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Integer nat) {
			try {
				return ComparisonResult.ofNullSafe(MapperS.of(check.evaluate((nat == null ? null : BigDecimal.valueOf(nat)))));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements NatCalled {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer nat) {
			return Collections.emptyList();
		}
	}
}
