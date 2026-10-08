package test.rws.b.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.rws.b.RwsTrade;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RwsTradeOnlyExistsValidator implements ValidatorWithArg<RwsTrade, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RwsTrade> ValidationResult<RwsTrade> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utid", ExistenceChecker.isSet((String) o.getUtid()))
				.put("notional", ExistenceChecker.isSet((BigDecimal) o.getNotional()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RwsTrade", ValidationResult.ValidationType.ONLY_EXISTS, "RwsTrade", path, "");
		}
		return failure("RwsTrade", ValidationResult.ValidationType.ONLY_EXISTS, "RwsTrade", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
