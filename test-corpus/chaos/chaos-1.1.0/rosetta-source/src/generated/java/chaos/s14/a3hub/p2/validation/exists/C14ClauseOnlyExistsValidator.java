package chaos.s14.a3hub.p2.validation.exists;

import chaos.s14.a3hub.p1.C14Aux;
import chaos.s14.a3hub.p2.C14Clause;
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

public class C14ClauseOnlyExistsValidator implements ValidatorWithArg<C14Clause, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C14Clause> ValidationResult<C14Clause> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("cid", ExistenceChecker.isSet((String) o.getCid()))
				.put("weight", ExistenceChecker.isSet((BigDecimal) o.getWeight()))
				.put("aux", ExistenceChecker.isSet((C14Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C14Clause", ValidationResult.ValidationType.ONLY_EXISTS, "C14Clause", path, "");
		}
		return failure("C14Clause", ValidationResult.ValidationType.ONLY_EXISTS, "C14Clause", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
