package chaos.s25.a4snap.validation.datarule;

import chaos.s25.a4snap.C25Paths;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataRule("C25PathsC25Bare")
@ImplementedBy(C25PathsC25Bare.Default.class)
public interface C25PathsC25Bare extends Validator<C25Paths> {
	
	String NAME = "C25PathsC25Bare";
	String DEFINITION = "p only exists or q only exists";
	
	class Default implements C25PathsC25Bare {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Paths c25Paths) {
			ComparisonResult result = executeDataRule(c25Paths);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "C25Paths", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "C25Paths", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(C25Paths c25Paths) {
			try {
				return onlyExists(MapperS.of(c25Paths), Arrays.asList("p", "q", "sub", "pick", "subs"), Arrays.asList("p")).orNullSafe(onlyExists(MapperS.of(c25Paths), Arrays.asList("p", "q", "sub", "pick", "subs"), Arrays.asList("q")));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C25PathsC25Bare {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Paths c25Paths) {
			return Collections.emptyList();
		}
	}
}
