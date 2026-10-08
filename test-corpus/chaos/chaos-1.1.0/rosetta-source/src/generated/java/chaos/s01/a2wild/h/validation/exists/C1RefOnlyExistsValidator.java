package chaos.s01.a2wild.h.validation.exists;

import chaos.s01.a2wild.h.C1Ref;
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

public class C1RefOnlyExistsValidator implements ValidatorWithArg<C1Ref, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Ref> ValidationResult<C1Ref> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("refCode", ExistenceChecker.isSet((String) o.getRefCode()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C1Ref", path, "");
		}
		return failure("C1Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C1Ref", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
