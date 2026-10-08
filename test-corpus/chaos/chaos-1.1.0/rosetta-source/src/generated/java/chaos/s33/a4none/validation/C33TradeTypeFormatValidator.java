package chaos.s33.a4none.validation;

import chaos.s33.a4none.C33Trade;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C33TradeTypeFormatValidator implements Validator<C33Trade> {

	private List<ComparisonResult> getComparisonResults(C33Trade o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C33Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C33Trade", ValidationResult.ValidationType.TYPE_FORMAT, "C33Trade", path, "", res.getError());
				}
				return success("C33Trade", ValidationResult.ValidationType.TYPE_FORMAT, "C33Trade", path, "");
			})
			.collect(toList());
	}

}
