package test.aliascond.validation.datarule;

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
import test.aliascond.Holder;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("HolderHolderOk")
@ImplementedBy(HolderHolderOk.Default.class)
public interface HolderHolderOk extends Validator<Holder> {
	
	String NAME = "HolderHolderOk";
	String DEFINITION = "one > 0";
	
	class Default implements HolderHolderOk {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder holder) {
			ComparisonResult result = executeDataRule(holder);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Holder", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Holder", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Holder holder) {
			try {
				return greaterThan(MapperS.of(holder).<Integer>map("getOne", _holder -> _holder.getOne()), MapperS.of(0), CardinalityOperator.All);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements HolderHolderOk {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder holder) {
			return Collections.emptyList();
		}
	}
}
