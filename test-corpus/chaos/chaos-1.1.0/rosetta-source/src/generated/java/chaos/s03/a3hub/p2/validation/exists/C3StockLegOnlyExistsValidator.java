package chaos.s03.a3hub.p2.validation.exists;

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

public class C3StockLegOnlyExistsValidator implements ValidatorWithArg<C3StockLeg, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C3StockLeg> ValidationResult<C3StockLeg> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ticker", ExistenceChecker.isSet((String) o.getTicker()))
				.put("common", ExistenceChecker.isSet((String) o.getCommon()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C3StockLeg", ValidationResult.ValidationType.ONLY_EXISTS, "C3StockLeg", path, "");
		}
		return failure("C3StockLeg", ValidationResult.ValidationType.ONLY_EXISTS, "C3StockLeg", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
