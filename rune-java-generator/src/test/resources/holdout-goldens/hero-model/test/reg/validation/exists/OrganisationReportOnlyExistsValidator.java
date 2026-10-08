package test.reg.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.reg.CountryEnum;
import test.reg.OrganisationReport;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class OrganisationReportOnlyExistsValidator implements ValidatorWithArg<OrganisationReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends OrganisationReport> ValidationResult<OrganisationReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("isGovernmentAgency", ExistenceChecker.isSet((Boolean) o.getIsGovernmentAgency()))
				.put("country", ExistenceChecker.isSet((CountryEnum) o.getCountry()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("OrganisationReport", ValidationResult.ValidationType.ONLY_EXISTS, "OrganisationReport", path, "");
		}
		return failure("OrganisationReport", ValidationResult.ValidationType.ONLY_EXISTS, "OrganisationReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
