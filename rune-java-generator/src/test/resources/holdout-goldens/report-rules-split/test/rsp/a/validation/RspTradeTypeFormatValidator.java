package test.rsp.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rsp.a.RspTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RspTradeTypeFormatValidator implements Validator<RspTrade> {

	private List<ComparisonResult> getComparisonResults(RspTrade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RspTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RspTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RspTrade", path, "", res.getError());
				}
				return success("RspTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RspTrade", path, "");
			})
			.collect(toList());
	}

}
