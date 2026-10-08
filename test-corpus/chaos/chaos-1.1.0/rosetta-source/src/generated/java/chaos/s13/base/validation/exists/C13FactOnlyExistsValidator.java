package chaos.s13.base.validation.exists;

import chaos.s13.base.C13Aux;
import chaos.s13.base.C13Fact;
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

public class C13FactOnlyExistsValidator implements ValidatorWithArg<C13Fact, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C13Fact> ValidationResult<C13Fact> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("fid", ExistenceChecker.isSet((String) o.getFid()))
				.put("amt", ExistenceChecker.isSet((BigDecimal) o.getAmt()))
				.put("aux", ExistenceChecker.isSet((C13Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C13Fact", ValidationResult.ValidationType.ONLY_EXISTS, "C13Fact", path, "");
		}
		return failure("C13Fact", ValidationResult.ValidationType.ONLY_EXISTS, "C13Fact", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
