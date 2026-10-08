package chaos.s26.a2wild.validation.exists;

import chaos.s26.a2wild.C26Either;
import chaos.s26.a2wild.C26OptA;
import chaos.s26.a2wild.C26OptB;
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

public class C26EitherOnlyExistsValidator implements ValidatorWithArg<C26Either, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C26Either> ValidationResult<C26Either> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C26OptA", ExistenceChecker.isSet((C26OptA) o.getC26OptA()))
				.put("C26OptB", ExistenceChecker.isSet((C26OptB) o.getC26OptB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C26Either", ValidationResult.ValidationType.ONLY_EXISTS, "C26Either", path, "");
		}
		return failure("C26Either", ValidationResult.ValidationType.ONLY_EXISTS, "C26Either", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
