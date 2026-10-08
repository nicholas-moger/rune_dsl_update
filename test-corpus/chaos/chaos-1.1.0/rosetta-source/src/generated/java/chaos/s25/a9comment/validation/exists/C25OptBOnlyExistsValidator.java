package chaos.s25.a9comment.validation.exists;

import chaos.s25.a9comment.C25OptB;
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

public class C25OptBOnlyExistsValidator implements ValidatorWithArg<C25OptB, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C25OptB> ValidationResult<C25OptB> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pb", ExistenceChecker.isSet((String) o.getPb()))
				.put("shared", ExistenceChecker.isSet((String) o.getShared()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C25OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C25OptB", path, "");
		}
		return failure("C25OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C25OptB", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
