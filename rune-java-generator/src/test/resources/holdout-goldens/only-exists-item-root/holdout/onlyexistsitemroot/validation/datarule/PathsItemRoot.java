package holdout.onlyexistsitemroot.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PathsItemRoot")
@ImplementedBy(PathsItemRoot.Default.class)
public interface PathsItemRoot extends Validator<Paths> {
	
	String NAME = "PathsItemRoot";
	String DEFINITION = "item -> p only exists or (item -> p, item -> q) only exists";
	
	class Default implements PathsItemRoot {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths paths) {
			ComparisonResult result = executeDataRule(paths);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Paths", path, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Paths", path, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Paths paths) {
			try {
				return onlyExists(MapperS.of(paths), Arrays.asList("p", "q", "sub", "pick"), Arrays.asList("p")).orNullSafe(onlyExists(MapperS.of(paths), Arrays.asList("p", "q", "sub", "pick"), Arrays.asList("p", "q")));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PathsItemRoot {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths paths) {
			return Collections.emptyList();
		}
	}
}
