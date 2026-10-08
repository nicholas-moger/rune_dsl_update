package chaos.s02.a1o3.validation.datarule;

import chaos.s02.a1o3.C2DirEnum;
import chaos.s02.a1o3.C2Use;
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

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C2UseC2NotHold")
@ImplementedBy(C2UseC2NotHold.Default.class)
public interface C2UseC2NotHold extends Validator<C2Use> {
	
	String NAME = "C2UseC2NotHold";
	String DEFINITION = "dir <> C2DirEnum -> Hold";
	
	class Default implements C2UseC2NotHold {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C2Use c2Use) {
			ComparisonResult result = executeDataRule(c2Use);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C2Use", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C2Use", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C2Use c2Use) {
			try {
				return notEqual(MapperS.of(c2Use).<C2DirEnum>map("getDir", _c2Use -> _c2Use.getDir()), MapperS.of(C2DirEnum.HOLD), CardinalityOperator.Any);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C2UseC2NotHold {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C2Use c2Use) {
			return Collections.emptyList();
		}
	}
}
