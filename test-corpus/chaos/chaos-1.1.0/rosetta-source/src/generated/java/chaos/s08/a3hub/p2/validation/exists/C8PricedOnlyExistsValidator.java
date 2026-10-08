package chaos.s08.a3hub.p2.validation.exists;

import chaos.s08.a3hub.p1.C8Box;
import chaos.s08.a3hub.p2.C8Priced;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C8PricedOnlyExistsValidator implements ValidatorWithArg<C8Priced, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C8Priced> ValidationResult<C8Priced> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("qty", ExistenceChecker.isSet((Integer) o.getQty()))
				.put("ccy", ExistenceChecker.isSet((String) o.getCcy()))
				.put("weights", ExistenceChecker.isSet((List<BigDecimal>) o.getWeights()))
				.put("evens", ExistenceChecker.isSet((List<Integer>) o.getEvens()))
				.put("box", ExistenceChecker.isSet((C8Box) o.getBox()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C8Priced", ValidationResult.ValidationType.ONLY_EXISTS, "C8Priced", path, "");
		}
		return failure("C8Priced", ValidationResult.ValidationType.ONLY_EXISTS, "C8Priced", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
