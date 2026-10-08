package chaos.s24.x13half.p1.validation.exists;

import chaos.s24.x13half.p1.C24Ref;
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

public class C24RefOnlyExistsValidator implements ValidatorWithArg<C24Ref, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C24Ref> ValidationResult<C24Ref> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("mark", ExistenceChecker.isSet((String) o.getMark()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C24Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C24Ref", path, "");
		}
		return failure("C24Ref", ValidationResult.ValidationType.ONLY_EXISTS, "C24Ref", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
