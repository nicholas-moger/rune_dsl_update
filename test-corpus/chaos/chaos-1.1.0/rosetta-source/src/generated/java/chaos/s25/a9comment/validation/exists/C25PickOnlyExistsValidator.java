package chaos.s25.a9comment.validation.exists;

import chaos.s25.a9comment.C25OptA;
import chaos.s25.a9comment.C25OptB;
import chaos.s25.a9comment.C25Pick;
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

public class C25PickOnlyExistsValidator implements ValidatorWithArg<C25Pick, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C25Pick> ValidationResult<C25Pick> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C25OptA", ExistenceChecker.isSet((C25OptA) o.getC25OptA()))
				.put("C25OptB", ExistenceChecker.isSet((C25OptB) o.getC25OptB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C25Pick", ValidationResult.ValidationType.ONLY_EXISTS, "C25Pick", path, "");
		}
		return failure("C25Pick", ValidationResult.ValidationType.ONLY_EXISTS, "C25Pick", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
