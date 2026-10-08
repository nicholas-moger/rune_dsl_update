package test.qep.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.qep.a.QepEvent;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class QepEventTypeFormatValidator implements Validator<QepEvent> {

	private List<ComparisonResult> getComparisonResults(QepEvent o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, QepEvent o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("QepEvent", ValidationResult.ValidationType.TYPE_FORMAT, "QepEvent", path, "", res.getError());
				}
				return success("QepEvent", ValidationResult.ValidationType.TYPE_FORMAT, "QepEvent", path, "");
			})
			.collect(toList());
	}

}
