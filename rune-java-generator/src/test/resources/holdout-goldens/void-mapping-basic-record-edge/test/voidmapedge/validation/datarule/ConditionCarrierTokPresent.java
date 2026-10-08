package test.voidmapedge.validation.datarule;

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
import test.voidmapedge.ConditionCarrier;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("ConditionCarrierTokPresent")
@ImplementedBy(ConditionCarrierTokPresent.Default.class)
public interface ConditionCarrierTokPresent extends Validator<ConditionCarrier> {
	
	String NAME = "ConditionCarrierTokPresent";
	String DEFINITION = "if flag = True then tok exists";
	
	class Default implements ConditionCarrierTokPresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, ConditionCarrier conditionCarrier) {
			ComparisonResult result = executeDataRule(conditionCarrier);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "ConditionCarrier", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "ConditionCarrier", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(ConditionCarrier conditionCarrier) {
			try {
				if (areEqual(MapperS.of(conditionCarrier).<Boolean>map("getFlag", _conditionCarrier -> _conditionCarrier.getFlag()), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
					return exists(MapperS.<Void>ofNull());
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements ConditionCarrierTokPresent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, ConditionCarrier conditionCarrier) {
			return Collections.emptyList();
		}
	}
}
