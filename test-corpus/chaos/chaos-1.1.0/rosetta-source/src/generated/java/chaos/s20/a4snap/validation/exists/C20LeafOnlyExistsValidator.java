package chaos.s20.a4snap.validation.exists;

import chaos.s20.a4snap.C20Leaf;
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

public class C20LeafOnlyExistsValidator implements ValidatorWithArg<C20Leaf, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C20Leaf> ValidationResult<C20Leaf> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("v", ExistenceChecker.isSet((BigDecimal) o.getV()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C20Leaf", ValidationResult.ValidationType.ONLY_EXISTS, "C20Leaf", path, "");
		}
		return failure("C20Leaf", ValidationResult.ValidationType.ONLY_EXISTS, "C20Leaf", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
