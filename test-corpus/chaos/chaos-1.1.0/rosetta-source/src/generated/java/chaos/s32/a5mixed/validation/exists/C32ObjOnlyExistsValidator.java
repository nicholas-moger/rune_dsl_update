package chaos.s32.a5mixed.validation.exists;

import chaos.s32.a5mixed.C32Obj;
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

public class C32ObjOnlyExistsValidator implements ValidatorWithArg<C32Obj, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C32Obj> ValidationResult<C32Obj> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("o", ExistenceChecker.isSet((String) o.getO()))
				.put("Class", ExistenceChecker.isSet((String) o._getClass()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C32Obj", ValidationResult.ValidationType.ONLY_EXISTS, "C32Obj", path, "");
		}
		return failure("C32Obj", ValidationResult.ValidationType.ONLY_EXISTS, "C32Obj", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
