package chaos.s12.a2dangle.unused.validation.exists;

import chaos.s12.a2dangle.unused.C12AuxUnusedT;
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

public class C12AuxUnusedTOnlyExistsValidator implements ValidatorWithArg<C12AuxUnusedT, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C12AuxUnusedT> ValidationResult<C12AuxUnusedT> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("stub", ExistenceChecker.isSet((String) o.getStub()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C12AuxUnusedT", ValidationResult.ValidationType.ONLY_EXISTS, "C12AuxUnusedT", path, "");
		}
		return failure("C12AuxUnusedT", ValidationResult.ValidationType.ONLY_EXISTS, "C12AuxUnusedT", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
