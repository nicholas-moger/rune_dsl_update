package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.CountryEnum;
import test.reg.Organisation;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OrganisationValidator implements Validator<Organisation> {

	private List<ComparisonResult> getComparisonResults(Organisation o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1), 
				checkCardinality("isGovernmentAgency", (Boolean) o.getIsGovernmentAgency() != null ? 1 : 0, 1, 1), 
				checkCardinality("country", (CountryEnum) o.getCountry() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Organisation o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Organisation", ValidationResult.ValidationType.CARDINALITY, "Organisation", path, "", res.getError());
				}
				return success("Organisation", ValidationResult.ValidationType.CARDINALITY, "Organisation", path, "");
			})
			.collect(toList());
	}

}
