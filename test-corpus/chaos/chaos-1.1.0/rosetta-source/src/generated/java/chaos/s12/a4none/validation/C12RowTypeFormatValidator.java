package chaos.s12.a4none.validation;

import chaos.s12.a4none.C12Row;
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

public class C12RowTypeFormatValidator implements Validator<C12Row> {

	private List<ComparisonResult> getComparisonResults(C12Row o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C12Row o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C12Row", ValidationResult.ValidationType.TYPE_FORMAT, "C12Row", path, "", res.getError());
				}
				return success("C12Row", ValidationResult.ValidationType.TYPE_FORMAT, "C12Row", path, "");
			})
			.collect(toList());
	}

}
