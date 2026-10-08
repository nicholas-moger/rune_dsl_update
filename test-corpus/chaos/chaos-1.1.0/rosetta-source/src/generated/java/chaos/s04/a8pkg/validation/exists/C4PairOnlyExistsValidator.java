package chaos.s04.a8pkg.validation.exists;

import chaos.s04.a8pkg.C4Pair;
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

public class C4PairOnlyExistsValidator implements ValidatorWithArg<C4Pair, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C4Pair> ValidationResult<C4Pair> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("left", ExistenceChecker.isSet((BigDecimal) o.getLeft()))
				.put("right", ExistenceChecker.isSet((BigDecimal) o.getRight()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C4Pair", ValidationResult.ValidationType.ONLY_EXISTS, "C4Pair", path, "");
		}
		return failure("C4Pair", ValidationResult.ValidationType.ONLY_EXISTS, "C4Pair", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
