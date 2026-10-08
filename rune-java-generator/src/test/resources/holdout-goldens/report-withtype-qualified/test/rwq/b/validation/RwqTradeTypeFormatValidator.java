package test.rwq.b.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rwq.b.RwqTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RwqTradeTypeFormatValidator implements Validator<RwqTrade> {

	private List<ComparisonResult> getComparisonResults(RwqTrade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RwqTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RwqTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RwqTrade", path, "", res.getError());
				}
				return success("RwqTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RwqTrade", path, "");
			})
			.collect(toList());
	}

}
