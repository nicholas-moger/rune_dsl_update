package test.reg.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.reg.AttributeReport;
import test.reg.CountryEnum;
import test.reg.OrganisationReport;
import test.reg.PowerEnum;
import test.reg.SokoviaAccordsReport;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class SokoviaAccordsReportOnlyExistsValidator implements ValidatorWithArg<SokoviaAccordsReport, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends SokoviaAccordsReport> ValidationResult<SokoviaAccordsReport> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("heroName", ExistenceChecker.isSet((String) o.getHeroName()))
				.put("dateOfBirth", ExistenceChecker.isSet((Date) o.getDateOfBirth()))
				.put("nationality", ExistenceChecker.isSet((CountryEnum) o.getNationality()))
				.put("hasSpecialAbilities", ExistenceChecker.isSet((Boolean) o.getHasSpecialAbilities()))
				.put("powers", ExistenceChecker.isSet((List<PowerEnum>) o.getPowers()))
				.put("attribute", ExistenceChecker.isSet((AttributeReport) o.getAttribute()))
				.put("organisations", ExistenceChecker.isSet((List<? extends OrganisationReport>) o.getOrganisations()))
				.put("notModelled", ExistenceChecker.isSet((String) o.getNotModelled()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("SokoviaAccordsReport", ValidationResult.ValidationType.ONLY_EXISTS, "SokoviaAccordsReport", path, "");
		}
		return failure("SokoviaAccordsReport", ValidationResult.ValidationType.ONLY_EXISTS, "SokoviaAccordsReport", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
