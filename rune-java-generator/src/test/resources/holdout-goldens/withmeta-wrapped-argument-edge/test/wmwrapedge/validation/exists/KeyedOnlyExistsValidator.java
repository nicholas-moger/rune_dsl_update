package test.wmwrapedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.wmwrapedge.Keyed;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class KeyedOnlyExistsValidator implements ValidatorWithArg<Keyed, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Keyed> ValidationResult<Keyed> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("kid", ExistenceChecker.isSet((String) o.getKid()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Keyed", ValidationResult.ValidationType.ONLY_EXISTS, "Keyed", path, "");
		}
		return failure("Keyed", ValidationResult.ValidationType.ONLY_EXISTS, "Keyed", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
