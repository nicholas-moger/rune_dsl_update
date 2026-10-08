package test.rws.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rws.a.RwsTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RwsTradeTypeFormatValidator implements Validator<RwsTrade> {

	private List<ComparisonResult> getComparisonResults(RwsTrade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RwsTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RwsTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RwsTrade", path, "", res.getError());
				}
				return success("RwsTrade", ValidationResult.ValidationType.TYPE_FORMAT, "RwsTrade", path, "");
			})
			.collect(toList());
	}

}
