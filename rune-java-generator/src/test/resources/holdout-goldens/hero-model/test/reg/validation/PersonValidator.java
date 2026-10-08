package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.Attribute;
import test.reg.CountryEnum;
import test.reg.Person;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PersonValidator implements Validator<Person> {

	private List<ComparisonResult> getComparisonResults(Person o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1), 
				checkCardinality("dateOfBirth", (Date) o.getDateOfBirth() != null ? 1 : 0, 1, 1), 
				checkCardinality("nationality", (CountryEnum) o.getNationality() != null ? 1 : 0, 1, 1), 
				checkCardinality("hasSpecialAbilities", (Boolean) o.getHasSpecialAbilities() != null ? 1 : 0, 1, 1), 
				checkCardinality("attribute", (Attribute) o.getAttribute() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Person o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Person", ValidationResult.ValidationType.CARDINALITY, "Person", path, "", res.getError());
				}
				return success("Person", ValidationResult.ValidationType.CARDINALITY, "Person", path, "");
			})
			.collect(toList());
	}

}
