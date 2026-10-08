package chaos.s19.a1o2.validation.exists;

import chaos.s19.a1o2.C19Part;
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

public class C19PartOnlyExistsValidator implements ValidatorWithArg<C19Part, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C19Part> ValidationResult<C19Part> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pid", ExistenceChecker.isSet((String) o.getPid()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C19Part", ValidationResult.ValidationType.ONLY_EXISTS, "C19Part", path, "");
		}
		return failure("C19Part", ValidationResult.ValidationType.ONLY_EXISTS, "C19Part", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
