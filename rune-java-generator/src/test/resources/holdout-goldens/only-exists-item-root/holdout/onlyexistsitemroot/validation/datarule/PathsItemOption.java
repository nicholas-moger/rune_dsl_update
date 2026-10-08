package holdout.onlyexistsitemroot.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.Pick;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PathsItemOption")
@ImplementedBy(PathsItemOption.Default.class)
public interface PathsItemOption extends Validator<Paths> {
	
	String NAME = "PathsItemOption";
	String DEFINITION = "if pick exists then item -> pick -> OptA only exists or item -> pick -> OptB only exists";
	
	class Default implements PathsItemOption {
	
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
				if (exists(MapperS.of(paths).<Pick>map("getPick", _paths -> _paths.getPick())).getOrDefault(false)) {
					return onlyExists(MapperS.of(paths).<Pick>map("getPick", _paths -> _paths.getPick()), Arrays.asList("OptA", "OptB"), Arrays.asList("OptA")).orNullSafe(onlyExists(MapperS.of(paths).<Pick>map("getPick", _paths -> _paths.getPick()), Arrays.asList("OptA", "OptB"), Arrays.asList("OptB")));
				}
				return ComparisonResult.ofEmpty();
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PathsItemOption {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths paths) {
			return Collections.emptyList();
		}
	}
}
