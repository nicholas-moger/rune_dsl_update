package chaos.s15.a2dangle.h.validation;

import chaos.s15.a2dangle.h.C15Aux;
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

public class C15AuxTypeFormatValidator implements Validator<C15Aux> {

	private List<ComparisonResult> getComparisonResults(C15Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C15Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C15Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C15Aux", path, "", res.getError());
				}
				return success("C15Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C15Aux", path, "");
			})
			.collect(toList());
	}

}
