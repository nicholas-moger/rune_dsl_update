package chaos.s27.a3half.p1.validation.datarule;

import chaos.s27.a3half.p1.functions.C27Check;
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
import javax.inject.Inject;


/**
 * @version 1.0.0
 */
@RosettaDataRule("C27NatCalled")
@ImplementedBy(C27NatCalled.Default.class)
public interface C27NatCalled extends Validator<Integer> {
	
	String NAME = "C27NatCalled";
	String DEFINITION = "C27Check(item)";
	
	class Default implements C27NatCalled {
	
		@Inject protected C27Check c27Check;
		
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
				return ComparisonResult.ofNullSafe(MapperS.of(c27Check.evaluate((c27Nat == null ? null : BigDecimal.valueOf(c27Nat)))));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C27NatCalled {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Integer c27Nat) {
			return Collections.emptyList();
		}
	}
}
