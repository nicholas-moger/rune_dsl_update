package test.qcn.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.qcn.a.QcnTerms;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class QcnTermsTypeFormatValidator implements Validator<QcnTerms> {

	private List<ComparisonResult> getComparisonResults(QcnTerms o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, QcnTerms o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("QcnTerms", ValidationResult.ValidationType.TYPE_FORMAT, "QcnTerms", path, "", res.getError());
				}
				return success("QcnTerms", ValidationResult.ValidationType.TYPE_FORMAT, "QcnTerms", path, "");
			})
			.collect(toList());
	}

}
