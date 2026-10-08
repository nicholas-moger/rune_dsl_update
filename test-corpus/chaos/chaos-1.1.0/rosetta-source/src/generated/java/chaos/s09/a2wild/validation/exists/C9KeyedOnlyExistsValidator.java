package chaos.s09.a2wild.validation.exists;

import chaos.s09.a2wild.C9Keyed;
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

public class C9KeyedOnlyExistsValidator implements ValidatorWithArg<C9Keyed, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C9Keyed> ValidationResult<C9Keyed> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("kid", ExistenceChecker.isSet((String) o.getKid()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C9Keyed", ValidationResult.ValidationType.ONLY_EXISTS, "C9Keyed", path, "");
		}
		return failure("C9Keyed", ValidationResult.ValidationType.ONLY_EXISTS, "C9Keyed", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
