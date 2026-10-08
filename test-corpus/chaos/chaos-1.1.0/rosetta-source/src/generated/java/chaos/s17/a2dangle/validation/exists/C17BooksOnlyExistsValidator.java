package chaos.s17.a2dangle.validation.exists;

import chaos.s17.a2dangle.C17ActionEnum;
import chaos.s17.a2dangle.C17Books;
import chaos.s17.a2dangle.C17SideEnum;
import chaos.s17.a2dangle.h.C17Held;
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

public class C17BooksOnlyExistsValidator implements ValidatorWithArg<C17Books, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C17Books> ValidationResult<C17Books> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("side", ExistenceChecker.isSet((C17SideEnum) o.getSide()))
				.put("action", ExistenceChecker.isSet((C17ActionEnum) o.getAction()))
				.put("held", ExistenceChecker.isSet((C17Held) o.getHeld()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C17Books", ValidationResult.ValidationType.ONLY_EXISTS, "C17Books", path, "");
		}
		return failure("C17Books", ValidationResult.ValidationType.ONLY_EXISTS, "C17Books", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
