package chaos.s07.a3half.p1.validation.exists;

import chaos.s07.a3half.p1.C7Extra;
import chaos.s07.a3half.p1.C7Trade;
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

public class C7TradeOnlyExistsValidator implements ValidatorWithArg<C7Trade, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C7Trade> ValidationResult<C7Trade> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("utid", ExistenceChecker.isSet((String) o.getUtid()))
				.put("notional", ExistenceChecker.isSet((BigDecimal) o.getNotional()))
				.put("extra", ExistenceChecker.isSet((C7Extra) o.getExtra()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C7Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C7Trade", path, "");
		}
		return failure("C7Trade", ValidationResult.ValidationType.ONLY_EXISTS, "C7Trade", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
