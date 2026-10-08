package chaos.s33.a5mixed.validation.exists;

import chaos.s33.a5mixed.C33Extra;
import chaos.s33.a5mixed.C33Trade;
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

public class C33TradeOnlyExistsValidator implements ValidatorWithArg<C33Trade, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C33Trade> ValidationResult<C33Trade> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utid", ExistenceChecker.isSet((String) o.getUtid()))
				.put("notional", ExistenceChecker.isSet((BigDecimal) o.getNotional()))
				.put("extra", ExistenceChecker.isSet((C33Extra) o.getExtra()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C33Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C33Trade", path, "");
		}
		return failure("C33Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C33Trade", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
