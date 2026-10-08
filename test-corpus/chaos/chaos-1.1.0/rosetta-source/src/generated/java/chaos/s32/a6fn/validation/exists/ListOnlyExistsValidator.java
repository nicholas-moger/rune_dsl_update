package chaos.s32.a6fn.validation.exists;

import chaos.s32.a6fn.List;
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

public class ListOnlyExistsValidator implements ValidatorWithArg<List, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends List> ValidationResult<List> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("items", ExistenceChecker.isSet((java.util.List<String>) o.getItems()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("List", ValidationResult.ValidationType.ONLY_EXISTS, "List", path, "");
		}
		return failure("List", ValidationResult.ValidationType.ONLY_EXISTS, "List", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
