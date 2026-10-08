package chaos.s11.a5crlf.validation;

import chaos.s11.a5crlf.C11Aux;
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

public class C11AuxTypeFormatValidator implements Validator<C11Aux> {

	private List<ComparisonResult> getComparisonResults(C11Aux o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C11Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C11Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C11Aux", path, "", res.getError());
				}
				return success("C11Aux", ValidationResult.ValidationType.TYPE_FORMAT, "C11Aux", path, "");
			})
			.collect(toList());
	}

}
