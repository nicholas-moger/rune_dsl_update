package chaos.s21.a1o4.validation.datarule;

import chaos.s21.a1o4.C21Prefer;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 1.0.0
 */
@RosettaDataRule("C21PreferC21Opt")
@ImplementedBy(C21PreferC21Opt.Default.class)
public interface C21PreferC21Opt extends Validator<C21Prefer> {
	
	String NAME = "C21PreferC21Opt";
	String DEFINITION = "optional choice y, z";
	
	class Default implements C21PreferC21Opt {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Prefer c21Prefer) {
			ComparisonResult result = executeDataRule(c21Prefer);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C21Prefer", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C21Prefer", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C21Prefer c21Prefer) {
			try {
				return choice(MapperS.of(c21Prefer), Arrays.asList("y", "z"), ChoiceRuleValidationMethod.OPTIONAL);
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C21PreferC21Opt {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Prefer c21Prefer) {
			return Collections.emptyList();
		}
	}
}
