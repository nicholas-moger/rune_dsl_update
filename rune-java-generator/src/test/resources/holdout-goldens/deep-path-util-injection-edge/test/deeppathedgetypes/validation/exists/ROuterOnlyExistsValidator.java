package test.deeppathedgetypes.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.deeppathedgetypes.RNote;
import test.deeppathedgetypes.ROuter;
import test.deeppathedgetypes.RWrap;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class ROuterOnlyExistsValidator implements ValidatorWithArg<ROuter, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends ROuter> ValidationResult<ROuter> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("RWrap", ExistenceChecker.isSet((RWrap) o.getRWrap()))
				.put("RNote", ExistenceChecker.isSet((RNote) o.getRNote()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("ROuter", ValidationResult.ValidationType.ONLY_EXISTS, "ROuter", path, "");
		}
		return failure("ROuter", ValidationResult.ValidationType.ONLY_EXISTS, "ROuter", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
