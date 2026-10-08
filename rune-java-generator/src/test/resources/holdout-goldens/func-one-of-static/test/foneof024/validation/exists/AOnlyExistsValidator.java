package test.foneof024.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.foneof024.A;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class AOnlyExistsValidator implements ValidatorWithArg<A, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends A> ValidationResult<A> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("a1", ExistenceChecker.isSet((String) o.getA1()))
				.put("a2", ExistenceChecker.isSet((String) o.getA2()))
				.put("a3", ExistenceChecker.isSet((Boolean) o.getA3()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("A", ValidationResult.ValidationType.ONLY_EXISTS, "A", path, "");
		}
		return failure("A", ValidationResult.ValidationType.ONLY_EXISTS, "A", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
