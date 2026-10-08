package chaos.s34.a2dangle.unused.validation.exists;

import chaos.s34.a2dangle.unused.C34AuxUnusedT;
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

public class C34AuxUnusedTOnlyExistsValidator implements ValidatorWithArg<C34AuxUnusedT, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C34AuxUnusedT> ValidationResult<C34AuxUnusedT> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("stub", ExistenceChecker.isSet((String) o.getStub()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C34AuxUnusedT", ValidationResult.ValidationType.ONLY_EXISTS, "C34AuxUnusedT", path, "");
		}
		return failure("C34AuxUnusedT", ValidationResult.ValidationType.ONLY_EXISTS, "C34AuxUnusedT", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
