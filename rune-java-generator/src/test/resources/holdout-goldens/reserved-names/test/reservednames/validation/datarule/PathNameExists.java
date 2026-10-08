package test.reservednames.validation.datarule;

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
import test.reservednames.Path;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

/**
 * @version 0.0.0
 */
@RosettaDataRule("PathNameExists")
@ImplementedBy(PathNameExists.Default.class)
public interface PathNameExists extends Validator<Path> {
	
	String NAME = "PathNameExists";
	String DEFINITION = "name exists";
	
	class Default implements PathNameExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path0, Path path) {
			ComparisonResult result = executeDataRule(path);
			if (result.getOrDefault(true)) {
				return Arrays.asList(ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, "Path", path0, DEFINITION));
			}
			
			String failureMessage = result.getError();
			if (failureMessage == null || failureMessage.contains("Null") || failureMessage == "") {
				failureMessage = "Condition has failed.";
			}
			return Arrays.asList(ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, "Path", path0, DEFINITION, failureMessage));
		}
		
		private ComparisonResult executeDataRule(Path path) {
			try {
				return exists(MapperS.of(path).<String>map("getName", _path -> _path.getName()));
			}
			catch (Exception ex) {
				return ComparisonResult.failure(ex.getMessage());
			}
		}
	}
	
	@SuppressWarnings("unused")
	class NoOp implements PathNameExists {
	
		@Override
		public List<ValidationResult<?>> getValidationResults(RosettaPath path0, Path path1) {
			return Collections.emptyList();
		}
	}
}
