package chaos.s01.a3third.p2.validation.exists;

import chaos.s01.a3third.p2.C1Cond;
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

public class C1CondOnlyExistsValidator implements ValidatorWithArg<C1Cond, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Cond> ValidationResult<C1Cond> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("lo", ExistenceChecker.isSet((BigDecimal) o.getLo()))
				.put("hi", ExistenceChecker.isSet((BigDecimal) o.getHi()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Cond", ValidationResult.ValidationType.ONLY_EXISTS, "C1Cond", path, "");
		}
		return failure("C1Cond", ValidationResult.ValidationType.ONLY_EXISTS, "C1Cond", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
