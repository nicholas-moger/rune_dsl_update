package test.voidrender.validation.datarule;

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
import test.voidrender.Carrier;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("CarrierToksExist")
@ImplementedBy(CarrierToksExist.Default.class)
public interface CarrierToksExist extends Validator<Carrier> {
	
	String NAME = "CarrierToksExist";
	String DEFINITION = "if flag = False then toks exists";
	
	class Default implements CarrierToksExist {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Carrier carrier) {
			ComparisonResult result = executeDataRule(carrier);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Carrier", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Carrier", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Carrier carrier) {
			try {
				if (areEqual(MapperS.of(carrier).<Boolean>map("getFlag", _carrier -> _carrier.getFlag()), MapperS.of(false), CardinalityOperator.All).getOrDefault(false)) {
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
	class NoOp implements CarrierToksExist {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Carrier carrier) {
			return Collections.emptyList();
		}
	}
}
