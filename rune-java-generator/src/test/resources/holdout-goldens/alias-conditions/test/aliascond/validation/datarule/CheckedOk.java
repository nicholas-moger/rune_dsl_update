package test.aliascond.validation.datarule;

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
import test.aliascond.functions.IsOk;


/**
 * @version 0.0.0
 */
@RosettaDataRule("CheckedOk")
@ImplementedBy(CheckedOk.Default.class)
public interface CheckedOk extends Validator<BigDecimal> {
	
	String NAME = "CheckedOk";
	String DEFINITION = "IsOk(item)";
	
	class Default implements CheckedOk {
	
		@Inject protected IsOk isOk;
		
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, BigDecimal checked) {
			ComparisonResult result = executeDataRule(checked);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Checked", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Checked", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(BigDecimal checked) {
			try {
				return ComparisonResult.ofNullSafe(MapperS.of(isOk.evaluate(checked)));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements CheckedOk {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, BigDecimal checked) {
			return Collections.emptyList();
		}
	}
}
