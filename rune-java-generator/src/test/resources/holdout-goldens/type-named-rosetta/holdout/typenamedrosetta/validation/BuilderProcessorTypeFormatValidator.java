package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.BuilderProcessor;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BuilderProcessorTypeFormatValidator implements Validator<BuilderProcessor> {

	private List<ComparisonResult> getComparisonResults(BuilderProcessor o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BuilderProcessor o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BuilderProcessor", ValidationResult.ValidationType.TYPE_FORMAT, "BuilderProcessor", path, "", res.getError());
				}
				return success("BuilderProcessor", ValidationResult.ValidationType.TYPE_FORMAT, "BuilderProcessor", path, "");
			})
			.collect(toList());
	}

}
