package holdout.voidexistsclean.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.voidexistsclean.Pair;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class PairOnlyExistsValidator implements ValidatorWithArg<Pair, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Pair> ValidationResult<Pair> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("present", ExistenceChecker.isSet((Boolean) o.getPresent()))
				.put("s", ExistenceChecker.isSet((String) o.getS()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Pair", ValidationResult.ValidationType.ONLY_EXISTS, "Pair", path, "");
		}
		return failure("Pair", ValidationResult.ValidationType.ONLY_EXISTS, "Pair", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
