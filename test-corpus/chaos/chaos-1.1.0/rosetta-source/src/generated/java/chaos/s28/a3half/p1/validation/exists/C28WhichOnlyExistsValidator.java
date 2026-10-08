package chaos.s28.a3half.p1.validation.exists;

import chaos.s28.a3half.p1.C28OptA;
import chaos.s28.a3half.p1.C28OptB;
import chaos.s28.a3half.p1.C28Which;
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

public class C28WhichOnlyExistsValidator implements ValidatorWithArg<C28Which, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C28Which> ValidationResult<C28Which> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("C28OptA", ExistenceChecker.isSet((C28OptA) o.getC28OptA()))
				.put("C28OptB", ExistenceChecker.isSet((C28OptB) o.getC28OptB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C28Which", ValidationResult.ValidationType.ONLY_EXISTS, "C28Which", path, "");
		}
		return failure("C28Which", ValidationResult.ValidationType.ONLY_EXISTS, "C28Which", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
