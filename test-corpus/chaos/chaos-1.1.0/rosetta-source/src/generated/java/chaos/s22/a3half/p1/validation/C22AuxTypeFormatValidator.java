package chaos.s22.a3half.p1.validation;

import chaos.s22.a3half.p1.C22Aux;
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

public class C22AuxTypeFormatValidator implements Validator<C22Aux> {

	private List<ComparisonResult> getComparisonResults(C22Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C22Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C22Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C22Aux", path, "", res.getError());
				}
				return success("C22Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C22Aux", path, "");
			})
			.collect(toList());
	}

}
