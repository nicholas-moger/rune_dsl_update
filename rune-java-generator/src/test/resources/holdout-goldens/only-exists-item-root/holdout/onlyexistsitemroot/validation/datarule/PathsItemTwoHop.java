package holdout.onlyexistsitemroot.validation.datarule;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaDataRule;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.Sub;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PathsItemTwoHop")
@ImplementedBy(PathsItemTwoHop.Default.class)
public interface PathsItemTwoHop extends Validator<Paths> {
	
	String NAME = "PathsItemTwoHop";
	String DEFINITION = "item -> sub -> sname only exists or (item -> sub -> sname, item -> sub -> subs) only exists";
	
	class Default implements PathsItemTwoHop {
	
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
				return onlyExists(MapperS.of(paths).<Sub>map("getSub", _paths -> _paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname")).orNullSafe(onlyExists(MapperS.of(paths).<Sub>map("getSub", _paths -> _paths.getSub()), Arrays.asList("sname", "subs"), Arrays.asList("sname", "subs")));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PathsItemTwoHop {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths paths) {
			return Collections.emptyList();
		}
	}
}
