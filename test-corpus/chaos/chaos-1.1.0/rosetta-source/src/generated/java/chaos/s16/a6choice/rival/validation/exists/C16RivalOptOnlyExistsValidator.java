package chaos.s16.a6choice.rival.validation.exists;

import chaos.s16.a6choice.rival.C16RivalOpt;
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

public class C16RivalOptOnlyExistsValidator implements ValidatorWithArg<C16RivalOpt, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C16RivalOpt> ValidationResult<C16RivalOpt> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("r", ExistenceChecker.isSet((String) o.getR()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C16RivalOpt", ValidationResult.ValidationType.ONLY_EXISTS, "C16RivalOpt", path, "");
		}
		return failure("C16RivalOpt", ValidationResult.ValidationType.ONLY_EXISTS, "C16RivalOpt", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
