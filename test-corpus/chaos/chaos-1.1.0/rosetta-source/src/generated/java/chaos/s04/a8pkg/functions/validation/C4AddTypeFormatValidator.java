package chaos.s04.a8pkg.functions.validation;

import chaos.s04.a8pkg.functions.C4Add;
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

public class C4AddTypeFormatValidator implements Validator<C4Add> {

	private List<ComparisonResult> getComparisonResults(C4Add o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C4Add o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C4Add", ValidationResult.ValidationType.TYPE_FORMAT, "C4Add", path, "", res.getError());
				}
				return success("C4Add", ValidationResult.ValidationType.TYPE_FORMAT, "C4Add", path, "");
			})
			.collect(toList());
	}

}
