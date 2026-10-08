package test.rsr.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rsr.a.RsrTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RsrTradeTypeFormatValidator implements Validator<RsrTrade> {

	private List<ComparisonResult> getComparisonResults(RsrTrade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RsrTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RsrTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RsrTrade", path, "", res.getError());
				}
				return success("RsrTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RsrTrade", path, "");
			})
			.collect(toList());
	}

}
