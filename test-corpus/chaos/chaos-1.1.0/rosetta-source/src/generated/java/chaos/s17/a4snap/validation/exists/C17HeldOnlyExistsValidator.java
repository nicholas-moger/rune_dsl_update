package chaos.s17.a4snap.validation.exists;

import chaos.s17.a4snap.C17Held;
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

public class C17HeldOnlyExistsValidator implements ValidatorWithArg<C17Held, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C17Held> ValidationResult<C17Held> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("h", ExistenceChecker.isSet((String) o.getH()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C17Held", ValidationResult.ValidationType.ONLY_EXISTS, "C17Held", path, "");
		}
		return failure("C17Held", ValidationResult.ValidationType.ONLY_EXISTS, "C17Held", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
