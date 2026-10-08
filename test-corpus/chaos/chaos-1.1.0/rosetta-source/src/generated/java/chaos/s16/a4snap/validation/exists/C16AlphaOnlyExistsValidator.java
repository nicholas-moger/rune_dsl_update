package chaos.s16.a4snap.validation.exists;

import chaos.s16.a4snap.C16Alpha;
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

public class C16AlphaOnlyExistsValidator implements ValidatorWithArg<C16Alpha, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C16Alpha> ValidationResult<C16Alpha> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("a", ExistenceChecker.isSet((String) o.getA()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C16Alpha", ValidationResult.ValidationType.ONLY_EXISTS, "C16Alpha", path, "");
		}
		return failure("C16Alpha", ValidationResult.ValidationType.ONLY_EXISTS, "C16Alpha", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
