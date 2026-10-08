package chaos.s04.a8pkg.functions.validation.exists;

import chaos.s04.a8pkg.functions.C4Add;
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

public class C4AddOnlyExistsValidator implements ValidatorWithArg<C4Add, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C4Add> ValidationResult<C4Add> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pathMark", ExistenceChecker.isSet((String) o.getPathMark()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C4Add", ValidationResult.ValidationType.ONLY_EXISTS, "C4Add", path, "");
		}
		return failure("C4Add", ValidationResult.ValidationType.ONLY_EXISTS, "C4Add", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
