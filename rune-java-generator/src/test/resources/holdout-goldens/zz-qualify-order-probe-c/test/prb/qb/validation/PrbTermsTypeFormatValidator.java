package test.prb.qb.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.prb.qb.PrbTerms;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PrbTermsTypeFormatValidator implements Validator<PrbTerms> {

	private List<ComparisonResult> getComparisonResults(PrbTerms o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, PrbTerms o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("PrbTerms", ValidationResult.ValidationType.TYPE_FORMAT, "PrbTerms", path, "", res.getError());
				}
				return success("PrbTerms", ValidationResult.ValidationType.TYPE_FORMAT, "PrbTerms", path, "");
			})
			.collect(toList());
	}

}
