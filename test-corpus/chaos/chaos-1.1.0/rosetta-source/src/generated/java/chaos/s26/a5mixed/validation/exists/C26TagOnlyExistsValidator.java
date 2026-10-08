package chaos.s26.a5mixed.validation.exists;

import chaos.s26.a5mixed.C26Tag;
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

public class C26TagOnlyExistsValidator implements ValidatorWithArg<C26Tag, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C26Tag> ValidationResult<C26Tag> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("code", ExistenceChecker.isSet((String) o.getCode()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C26Tag", ValidationResult.ValidationType.ONLY_EXISTS, "C26Tag", path, "");
		}
		return failure("C26Tag", ValidationResult.ValidationType.ONLY_EXISTS, "C26Tag", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
