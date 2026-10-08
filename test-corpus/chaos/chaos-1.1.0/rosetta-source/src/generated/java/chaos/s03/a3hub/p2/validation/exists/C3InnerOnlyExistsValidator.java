package chaos.s03.a3hub.p2.validation.exists;

import chaos.s03.a3hub.p2.C3CashLeg;
import chaos.s03.a3hub.p2.C3Inner;
import chaos.s03.a3hub.p2.C3StockLeg;
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

public class C3InnerOnlyExistsValidator implements ValidatorWithArg<C3Inner, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C3Inner> ValidationResult<C3Inner> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C3CashLeg", ExistenceChecker.isSet((C3CashLeg) o.getC3CashLeg()))
				.put("C3StockLeg", ExistenceChecker.isSet((C3StockLeg) o.getC3StockLeg()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C3Inner", ValidationResult.ValidationType.ONLY_EXISTS, "C3Inner", path, "");
		}
		return failure("C3Inner", ValidationResult.ValidationType.ONLY_EXISTS, "C3Inner", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
