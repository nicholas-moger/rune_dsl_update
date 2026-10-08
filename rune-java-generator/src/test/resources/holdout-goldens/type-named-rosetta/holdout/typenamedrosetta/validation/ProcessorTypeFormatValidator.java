package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.Processor;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ProcessorTypeFormatValidator implements Validator<Processor> {

	private List<ComparisonResult> getComparisonResults(Processor o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Processor o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Processor", ValidationResult.ValidationType.TYPE_FORMAT, "Processor", path, "", res.getError());
				}
				return success("Processor", ValidationResult.ValidationType.TYPE_FORMAT, "Processor", path, "");
			})
			.collect(toList());
	}

}
