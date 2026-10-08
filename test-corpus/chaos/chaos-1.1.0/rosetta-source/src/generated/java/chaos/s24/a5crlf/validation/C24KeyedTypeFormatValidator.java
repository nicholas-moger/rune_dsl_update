package chaos.s24.a5crlf.validation;

import chaos.s24.a5crlf.C24Keyed;
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

public class C24KeyedTypeFormatValidator implements Validator<C24Keyed> {

	private List<ComparisonResult> getComparisonResults(C24Keyed o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Keyed o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Keyed", ValidationResult.ValidationType.TYPE_FORMAT, "C24Keyed", path, "", res.getError());
				}
				return success("C24Keyed", ValidationResult.ValidationType.TYPE_FORMAT, "C24Keyed", path, "");
			})
			.collect(toList());
	}

}
