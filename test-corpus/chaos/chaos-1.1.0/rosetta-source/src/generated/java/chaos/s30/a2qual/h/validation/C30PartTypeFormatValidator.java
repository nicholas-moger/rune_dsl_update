package chaos.s30.a2qual.h.validation;

import chaos.s30.a2qual.h.C30Part;
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

public class C30PartTypeFormatValidator implements Validator<C30Part> {

	private List<ComparisonResult> getComparisonResults(C30Part o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C30Part o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C30Part", ValidationResult.ValidationType.TYPE_FORMAT, "C30Part", path, "", res.getError());
				}
				return success("C30Part", ValidationResult.ValidationType.TYPE_FORMAT, "C30Part", path, "");
			})
			.collect(toList());
	}

}
