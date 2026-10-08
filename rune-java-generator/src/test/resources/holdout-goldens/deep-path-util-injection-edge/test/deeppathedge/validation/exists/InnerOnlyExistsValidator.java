package test.deeppathedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.deeppathedge.CashLeg;
import test.deeppathedge.Inner;
import test.deeppathedge.StockLeg;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class InnerOnlyExistsValidator implements ValidatorWithArg<Inner, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Inner> ValidationResult<Inner> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("CashLeg", ExistenceChecker.isSet((CashLeg) o.getCashLeg()))
				.put("StockLeg", ExistenceChecker.isSet((StockLeg) o.getStockLeg()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Inner", ValidationResult.ValidationType.ONLY_EXISTS, "Inner", path, "");
		}
		return failure("Inner", ValidationResult.ValidationType.ONLY_EXISTS, "Inner", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
