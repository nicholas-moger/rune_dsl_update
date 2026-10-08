package chaos.s99.base.validation.exists;

import chaos.s99.base.C99Holder;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C99HolderOnlyExistsValidator implements ValidatorWithArg<C99Holder, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C99Holder> ValidationResult<C99Holder> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ig", ExistenceChecker.isSet((List<Integer>) o.getIg()))
				.put("n", ExistenceChecker.isSet((Integer) o.getN()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C99Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C99Holder", path, "");
		}
		return failure("C99Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C99Holder", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
