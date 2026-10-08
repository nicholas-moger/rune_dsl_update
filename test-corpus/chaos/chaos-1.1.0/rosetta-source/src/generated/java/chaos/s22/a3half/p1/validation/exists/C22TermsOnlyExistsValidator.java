package chaos.s22.a3half.p1.validation.exists;

import chaos.s22.a3half.p1.C22Aux;
import chaos.s22.a3half.p1.C22Terms;
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

public class C22TermsOnlyExistsValidator implements ValidatorWithArg<C22Terms, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C22Terms> ValidationResult<C22Terms> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("kind", ExistenceChecker.isSet((String) o.getKind()))
				.put("notional", ExistenceChecker.isSet((BigDecimal) o.getNotional()))
				.put("aux", ExistenceChecker.isSet((C22Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C22Terms", ValidationResult.ValidationType.ONLY_EXISTS, "C22Terms", path, "");
		}
		return failure("C22Terms", ValidationResult.ValidationType.ONLY_EXISTS, "C22Terms", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
