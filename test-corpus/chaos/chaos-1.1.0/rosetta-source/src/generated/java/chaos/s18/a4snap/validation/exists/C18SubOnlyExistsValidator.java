package chaos.s18.a4snap.validation.exists;

import chaos.s18.a4snap.C18Sub;
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

public class C18SubOnlyExistsValidator implements ValidatorWithArg<C18Sub, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C18Sub> ValidationResult<C18Sub> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("s", ExistenceChecker.isSet((String) o.getS()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C18Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C18Sub", path, "");
		}
		return failure("C18Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C18Sub", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
