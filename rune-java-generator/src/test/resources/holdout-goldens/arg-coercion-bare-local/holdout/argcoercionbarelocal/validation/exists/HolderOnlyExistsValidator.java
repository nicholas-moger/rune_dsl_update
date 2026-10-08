package holdout.argcoercionbarelocal.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.argcoercionbarelocal.Holder;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class HolderOnlyExistsValidator implements ValidatorWithArg<Holder, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Holder> ValidationResult<Holder> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("tally", ExistenceChecker.isSet((Integer) o.getTally()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Holder", ValidationResult.ValidationType.ONLY_EXISTS, "Holder", path, "");
		}
		return failure("Holder", ValidationResult.ValidationType.ONLY_EXISTS, "Holder", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
