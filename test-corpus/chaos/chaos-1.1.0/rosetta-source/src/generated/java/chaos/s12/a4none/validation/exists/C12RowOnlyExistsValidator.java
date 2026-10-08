package chaos.s12.a4none.validation.exists;

import chaos.s12.a4none.C12Aux;
import chaos.s12.a4none.C12Row;
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

public class C12RowOnlyExistsValidator implements ValidatorWithArg<C12Row, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C12Row> ValidationResult<C12Row> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("key", ExistenceChecker.isSet((String) o.getKey()))
				.put("val", ExistenceChecker.isSet((BigDecimal) o.getVal()))
				.put("aux", ExistenceChecker.isSet((C12Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C12Row", ValidationResult.ValidationType.ONLY_EXISTS, "C12Row", path, "");
		}
		return failure("C12Row", ValidationResult.ValidationType.ONLY_EXISTS, "C12Row", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
