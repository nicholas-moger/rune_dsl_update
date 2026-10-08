package chaos.s27.a1o1.validation.exists;

import chaos.s27.a1o1.C27Ref;
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

public class C27RefOnlyExistsValidator implements ValidatorWithArg<C27Ref, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C27Ref> ValidationResult<C27Ref> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("caption", ExistenceChecker.isSet((String) o.getCaption()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C27Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C27Ref", path, "");
		}
		return failure("C27Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C27Ref", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
