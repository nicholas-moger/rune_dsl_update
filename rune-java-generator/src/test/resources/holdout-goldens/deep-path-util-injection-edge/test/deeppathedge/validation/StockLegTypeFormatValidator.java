package test.deeppathedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedge.StockLeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class StockLegTypeFormatValidator implements Validator<StockLeg> {

	private List<ComparisonResult> getComparisonResults(StockLeg o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, StockLeg o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("StockLeg", ValidationResult.ValidationType.TYPE_FORMAT, "StockLeg", path, "", res.getError());
				}
				return success("StockLeg", ValidationResult.ValidationType.TYPE_FORMAT, "StockLeg", path, "");
			})
			.collect(toList());
	}

}
