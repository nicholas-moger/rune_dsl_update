package chaos.s01.a2wild.validation.exists;

import chaos.s01.a2wild.C1Base;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C1BaseOnlyExistsValidator implements ValidatorWithArg<C1Base, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Base> ValidationResult<C1Base> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("baseId", ExistenceChecker.isSet((String) o.getBaseId()))
				.put("note", ExistenceChecker.isSet((String) o.getNote()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Base", ValidationResult.ValidationType.ONLY_EXISTS, "C1Base", path, "");
		}
		return failure("C1Base", ValidationResult.ValidationType.ONLY_EXISTS, "C1Base", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
