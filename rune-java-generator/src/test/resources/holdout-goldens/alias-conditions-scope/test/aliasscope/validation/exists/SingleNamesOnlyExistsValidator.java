package test.aliasscope.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliasscope.SingleNames;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class SingleNamesOnlyExistsValidator implements ValidatorWithArg<SingleNames, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends SingleNames> ValidationResult<SingleNames> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("results", ExistenceChecker.isSet((Integer) o.getResults()))
				.put("o", ExistenceChecker.isSet((Integer) o.getO()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("SingleNames", ValidationResult.ValidationType.ONLY_EXISTS, "SingleNames", path, "");
		}
		return failure("SingleNames", ValidationResult.ValidationType.ONLY_EXISTS, "SingleNames", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
