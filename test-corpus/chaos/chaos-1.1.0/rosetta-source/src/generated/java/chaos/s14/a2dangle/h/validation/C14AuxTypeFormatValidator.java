package chaos.s14.a2dangle.h.validation;

import chaos.s14.a2dangle.h.C14Aux;
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

public class C14AuxTypeFormatValidator implements Validator<C14Aux> {

	private List<ComparisonResult> getComparisonResults(C14Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C14Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C14Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C14Aux", path, "", res.getError());
				}
				return success("C14Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C14Aux", path, "");
			})
			.collect(toList());
	}

}
