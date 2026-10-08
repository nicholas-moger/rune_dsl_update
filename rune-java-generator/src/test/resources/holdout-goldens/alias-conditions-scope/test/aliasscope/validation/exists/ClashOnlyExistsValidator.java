package test.aliasscope.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliasscope.Clash;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class ClashOnlyExistsValidator implements ValidatorWithArg<Clash, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Clash> ValidationResult<Clash> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("results", ExistenceChecker.isSet((List<Integer>) o.getResults()))
				.put("o", ExistenceChecker.isSet((List<Integer>) o.getO()))
				.put("i", ExistenceChecker.isSet((List<Integer>) o.getI()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Clash", ValidationResult.ValidationType.ONLY_EXISTS, "Clash", path, "");
		}
		return failure("Clash", ValidationResult.ValidationType.ONLY_EXISTS, "Clash", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
