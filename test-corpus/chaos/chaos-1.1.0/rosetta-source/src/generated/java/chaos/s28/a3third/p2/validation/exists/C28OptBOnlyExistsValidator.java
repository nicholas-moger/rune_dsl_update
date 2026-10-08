package chaos.s28.a3third.p2.validation.exists;

import chaos.s28.a3third.p2.C28OptB;
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

public class C28OptBOnlyExistsValidator implements ValidatorWithArg<C28OptB, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C28OptB> ValidationResult<C28OptB> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("bv", ExistenceChecker.isSet((String) o.getBv()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C28OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C28OptB", path, "");
		}
		return failure("C28OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C28OptB", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
