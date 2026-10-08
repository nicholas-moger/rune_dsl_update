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
import test.reg.Attribute;
import test.reg.CountryEnum;
import test.reg.Organisation;
import test.reg.Person;
import test.reg.PowerEnum;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class PersonOnlyExistsValidator implements ValidatorWithArg<Person, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Person> ValidationResult<Person> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("dateOfBirth", ExistenceChecker.isSet((Date) o.getDateOfBirth()))
				.put("nationality", ExistenceChecker.isSet((CountryEnum) o.getNationality()))
				.put("hasSpecialAbilities", ExistenceChecker.isSet((Boolean) o.getHasSpecialAbilities()))
				.put("powers", ExistenceChecker.isSet((List<PowerEnum>) o.getPowers()))
				.put("attribute", ExistenceChecker.isSet((Attribute) o.getAttribute()))
				.put("organisations", ExistenceChecker.isSet((List<? extends Organisation>) o.getOrganisations()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Person", ValidationResult.ValidationType.ONLY_EXISTS, "Person", path, "");
		}
		return failure("Person", ValidationResult.ValidationType.ONLY_EXISTS, "Person", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
