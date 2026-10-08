package chaos.s18.a1o4.validation.exists;

import chaos.s18.a1o4.C18OptA;
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

public class C18OptAOnlyExistsValidator implements ValidatorWithArg<C18OptA, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C18OptA> ValidationResult<C18OptA> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("av", ExistenceChecker.isSet((String) o.getAv()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C18OptA", ValidationResult.ValidationType.ONLY_EXISTS, "C18OptA", path, "");
		}
		return failure("C18OptA", ValidationResult.ValidationType.ONLY_EXISTS, "C18OptA", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
