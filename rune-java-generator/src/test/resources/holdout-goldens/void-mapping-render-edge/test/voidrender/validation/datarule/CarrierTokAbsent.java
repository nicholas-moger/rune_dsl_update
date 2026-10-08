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
@RosettaDataRule("CarrierTokAbsent")
@ImplementedBy(CarrierTokAbsent.Default.class)
public interface CarrierTokAbsent extends Validator<Carrier> {
	
	String NAME = "CarrierTokAbsent";
	String DEFINITION = "if flag = True then tok is absent";
	
	class Default implements CarrierTokAbsent {
	
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
				if (areEqual(MapperS.of(carrier).<Boolean>map("getFlag", _carrier -> _carrier.getFlag()), MapperS.of(true), CardinalityOperator.All).getOrDefault(false)) {
					return notExists(MapperS.<Void>ofNull());
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements CarrierTokAbsent {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Carrier carrier) {
			return Collections.emptyList();
		}
	}
}
