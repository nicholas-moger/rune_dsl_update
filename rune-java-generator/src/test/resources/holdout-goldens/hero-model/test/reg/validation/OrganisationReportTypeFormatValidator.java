package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.OrganisationReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OrganisationReportTypeFormatValidator implements Validator<OrganisationReport> {

	private List<ComparisonResult> getComparisonResults(OrganisationReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, OrganisationReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("OrganisationReport", ValidationResult.ValidationType.TYPE_FORMAT, "OrganisationReport", path, "", res.getError());
				}
				return success("OrganisationReport", ValidationResult.ValidationType.TYPE_FORMAT, "OrganisationReport", path, "");
			})
			.collect(toList());
	}

}
