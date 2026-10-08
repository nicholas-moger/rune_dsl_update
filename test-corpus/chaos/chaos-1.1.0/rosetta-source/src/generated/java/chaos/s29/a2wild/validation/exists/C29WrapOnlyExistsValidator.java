package chaos.s29.a2wild.validation.exists;

import chaos.s29.a2wild.C29Inner;
import chaos.s29.a2wild.C29Wrap;
import chaos.s29.a2wild.h.C29Leaf;
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

public class C29WrapOnlyExistsValidator implements ValidatorWithArg<C29Wrap, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C29Wrap> ValidationResult<C29Wrap> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("text", ExistenceChecker.isSet((String) o.getText()))
				.put("leaves", ExistenceChecker.isSet((List<? extends C29Leaf>) o.getLeaves()))
				.put("code", ExistenceChecker.isSet((FieldWithMetaString) o.getCode()))
				.put("codes", ExistenceChecker.isSet((List<? extends FieldWithMetaString>) o.getCodes()))
				.put("inners", ExistenceChecker.isSet((List<? extends C29Inner>) o.getInners()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C29Wrap", ValidationResult.ValidationType.ONLY_EXISTS, "C29Wrap", path, "");
		}
		return failure("C29Wrap", ValidationResult.ValidationType.ONLY_EXISTS, "C29Wrap", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
