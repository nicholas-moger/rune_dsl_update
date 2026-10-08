package chaos.s26.base.validation.exists;

import chaos.s26.base.C26OptB;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C26OptBOnlyExistsValidator implements ValidatorWithArg<C26OptB, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C26OptB> ValidationResult<C26OptB> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("bv", ExistenceChecker.isSet((BigDecimal) o.getBv()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C26OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C26OptB", path, "");
		}
		return failure("C26OptB", ValidationResult.ValidationType.ONLY_EXISTS, "C26OptB", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
