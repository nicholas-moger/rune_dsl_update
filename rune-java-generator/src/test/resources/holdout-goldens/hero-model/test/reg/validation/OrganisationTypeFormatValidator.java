package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.Organisation;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OrganisationTypeFormatValidator implements Validator<Organisation> {

	private List<ComparisonResult> getComparisonResults(Organisation o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Organisation o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Organisation", ValidationResult.ValidationType.TYPE_FORMAT, "Organisation", path, "", res.getError());
				}
				return success("Organisation", ValidationResult.ValidationType.TYPE_FORMAT, "Organisation", path, "");
			})
			.collect(toList());
	}

}
