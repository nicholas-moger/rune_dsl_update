package chaos.s12.a5uni.validation;

import chaos.s12.a5uni.C12Aux;
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

public class C12AuxTypeFormatValidator implements Validator<C12Aux> {

	private List<ComparisonResult> getComparisonResults(C12Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C12Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C12Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C12Aux", path, "", res.getError());
				}
				return success("C12Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C12Aux", path, "");
			})
			.collect(toList());
	}

}
