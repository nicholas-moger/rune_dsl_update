package chaos.s02.a2dangle.validation.exists;

import chaos.s02.a2dangle.C2DirEnum;
import chaos.s02.a2dangle.C2ExtEnum;
import chaos.s02.a2dangle.C2Use;
import chaos.s02.a2dangle.h.C2Tag;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C2UseOnlyExistsValidator implements ValidatorWithArg<C2Use, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C2Use> ValidationResult<C2Use> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("dir", ExistenceChecker.isSet((C2DirEnum) o.getDir()))
				.put("exts", ExistenceChecker.isSet((List<C2ExtEnum>) o.getExts()))
				.put("tags", ExistenceChecker.isSet((List<? extends C2Tag>) o.getTags()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C2Use", ValidationResult.ValidationType.ONLY_EXISTS, "C2Use", path, "");
		}
		return failure("C2Use", ValidationResult.ValidationType.ONLY_EXISTS, "C2Use", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
