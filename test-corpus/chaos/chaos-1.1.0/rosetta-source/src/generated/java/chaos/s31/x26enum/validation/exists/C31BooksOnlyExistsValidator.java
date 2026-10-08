package chaos.s31.x26enum.validation.exists;

import chaos.s31.x26enum.C31Books;
import chaos.s31.x26enum.C31Held;
import chaos.s31.x26enum.C31MoveEnum;
import chaos.s31.x26enum.C31SideEnum;
import chaos.s31.x26enum.C31TopEnum;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C31BooksOnlyExistsValidator implements ValidatorWithArg<C31Books, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C31Books> ValidationResult<C31Books> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("side", ExistenceChecker.isSet((C31SideEnum) o.getSide()))
				.put("move", ExistenceChecker.isSet((C31MoveEnum) o.getMove()))
				.put("coded", ExistenceChecker.isSet((FieldWithMetaString) o.getCoded()))
				.put("sides", ExistenceChecker.isSet((List<C31SideEnum>) o.getSides()))
				.put("top", ExistenceChecker.isSet((C31TopEnum) o.getTop()))
				.put("held", ExistenceChecker.isSet((C31Held) o.getHeld()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C31Books", ValidationResult.ValidationType.ONLY_EXISTS, "C31Books", path, "");
		}
		return failure("C31Books", ValidationResult.ValidationType.ONLY_EXISTS, "C31Books", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
