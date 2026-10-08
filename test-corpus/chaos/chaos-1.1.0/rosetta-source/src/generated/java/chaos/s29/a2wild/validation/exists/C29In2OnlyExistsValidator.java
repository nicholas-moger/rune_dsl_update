package chaos.s29.a2wild.validation.exists;

import chaos.s29.a2wild.C29In2;
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

public class C29In2OnlyExistsValidator implements ValidatorWithArg<C29In2, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29In2> ValidationResult<C29In2> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("deep", ExistenceChecker.isSet((String) o.getDeep()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29In2", ValidationResult.ValidationType.ONLY_EXISTS, "C29In2", path, "");
		}
		return failure("C29In2", ValidationResult.ValidationType.ONLY_EXISTS, "C29In2", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
