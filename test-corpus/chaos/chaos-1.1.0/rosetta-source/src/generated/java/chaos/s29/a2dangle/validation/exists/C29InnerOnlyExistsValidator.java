package chaos.s29.a2dangle.validation.exists;

import chaos.s29.a2dangle.C29In1;
import chaos.s29.a2dangle.C29In2;
import chaos.s29.a2dangle.C29Inner;
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

public class C29InnerOnlyExistsValidator implements ValidatorWithArg<C29Inner, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29Inner> ValidationResult<C29Inner> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C29In1", ExistenceChecker.isSet((C29In1) o.getC29In1()))
				.put("C29In2", ExistenceChecker.isSet((C29In2) o.getC29In2()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29Inner", ValidationResult.ValidationType.ONLY_EXISTS, "C29Inner", path, "");
		}
		return failure("C29Inner", ValidationResult.ValidationType.ONLY_EXISTS, "C29Inner", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
