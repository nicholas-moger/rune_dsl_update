package chaos.s25.base.validation.datarule;

import chaos.s25.base.C25Paths;
import chaos.s25.base.C25Pick;
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
@RosettaDataRule("C25PathsC25Option")
@ImplementedBy(C25PathsC25Option.Default.class)
public interface C25PathsC25Option extends Validator<C25Paths> {
	
	String NAME = "C25PathsC25Option";
	String DEFINITION = "if pick exists then pick -> C25OptA only exists or pick -> C25OptB only exists";
	
	class Default implements C25PathsC25Option {
	
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
				if (exists(MapperS.of(c25Paths).<C25Pick>map("getPick", _c25Paths -> _c25Paths.getPick())).getOrDefault(false)) {
					return onlyExists(MapperS.of(c25Paths).<C25Pick>map("getPick", _c25Paths -> _c25Paths.getPick()), Arrays.asList("C25OptA", "C25OptB"), Arrays.asList("C25OptA")).orNullSafe(onlyExists(MapperS.of(c25Paths).<C25Pick>map("getPick", _c25Paths -> _c25Paths.getPick()), Arrays.asList("C25OptA", "C25OptB"), Arrays.asList("C25OptB")));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements C25PathsC25Option {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Paths c25Paths) {
			return Collections.emptyList();
		}
	}
}
