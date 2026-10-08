package chaos.s02.a2dangle.validation.datarule;

import chaos.s02.a2dangle.C2ExtEnum;
import chaos.s02.a2dangle.C2Use;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
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
@RosettaDataRule("C2UseC2HasAlpha")
@ImplementedBy(C2UseC2HasAlpha.Default.class)
public interface C2UseC2HasAlpha extends Validator<C2Use> {
	
	String NAME = "C2UseC2HasAlpha";
	String DEFINITION = "if exts exists then exts contains C2ExtEnum -> Alpha or exts contains C2ExtEnum -> Gamma";
	
	class Default implements C2UseC2HasAlpha {
	
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
				if (exists(MapperS.of(c2Use).<C2ExtEnum>mapC("getExts", _c2Use -> _c2Use.getExts())).getOrDefault(false)) {
					return contains(MapperS.of(c2Use).<C2ExtEnum>mapC("getExts", _c2Use -> _c2Use.getExts()), MapperS.of(C2ExtEnum.ALPHA)).orNullSafe(contains(MapperS.of(c2Use).<C2ExtEnum>mapC("getExts", _c2Use -> _c2Use.getExts()), MapperS.of(C2ExtEnum.GAMMA)));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C2UseC2HasAlpha {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C2Use c2Use) {
			return Collections.emptyList();
		}
	}
}
