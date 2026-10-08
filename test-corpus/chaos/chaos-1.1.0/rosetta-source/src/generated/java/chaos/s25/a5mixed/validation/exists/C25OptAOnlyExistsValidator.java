package chaos.s25.a5mixed.validation.exists;

import chaos.s25.a5mixed.C25OptA;
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

public class C25OptAOnlyExistsValidator implements ValidatorWithArg<C25OptA, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C25OptA> ValidationResult<C25OptA> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pa", ExistenceChecker.isSet((String) o.getPa()))
				.put("shared", ExistenceChecker.isSet((String) o.getShared()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C25OptA", ValidationResult.ValidationType.ONLY_EXISTS, "C25OptA", path, "");
		}
		return failure("C25OptA", ValidationResult.ValidationType.ONLY_EXISTS, "C25OptA", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
