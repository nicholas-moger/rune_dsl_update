package chaos.s34.a3third.p1.validation;

import chaos.s34.a3third.p1.C34Aux;
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

public class C34AuxTypeFormatValidator implements Validator<C34Aux> {

	private List<ComparisonResult> getComparisonResults(C34Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C34Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C34Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C34Aux", path, "", res.getError());
				}
				return success("C34Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C34Aux", path, "");
			})
			.collect(toList());
	}

}
