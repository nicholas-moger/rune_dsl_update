package test.chswitchedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.chswitchedge.OptA;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class OptAOnlyExistsValidator implements ValidatorWithArg<OptA, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends OptA> ValidationResult<OptA> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("av", ExistenceChecker.isSet((String) o.getAv()))
				.put("tags", ExistenceChecker.isSet((List<String>) o.getTags()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("OptA", ValidationResult.ValidationType.ONLY_EXISTS, "OptA", path, "");
		}
		return failure("OptA", ValidationResult.ValidationType.ONLY_EXISTS, "OptA", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
