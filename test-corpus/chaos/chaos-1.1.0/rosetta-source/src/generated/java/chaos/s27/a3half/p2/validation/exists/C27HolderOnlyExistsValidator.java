package chaos.s27.a3half.p2.validation.exists;

import chaos.s27.a3half.p1.C27Ref;
import chaos.s27.a3half.p2.C27Holder;
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

public class C27HolderOnlyExistsValidator implements ValidatorWithArg<C27Holder, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C27Holder> ValidationResult<C27Holder> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("l", ExistenceChecker.isSet((List<Integer>) o.getL()))
				.put("vr", ExistenceChecker.isSet((Integer) o.getVr()))
				.put("ar", ExistenceChecker.isSet((List<Integer>) o.getAr()))
				.put("h", ExistenceChecker.isSet((Integer) o.getH()))
				.put("inj", ExistenceChecker.isSet((List<Integer>) o.getInj()))
				.put("i", ExistenceChecker.isSet((List<Integer>) o.getI()))
				.put("o", ExistenceChecker.isSet((List<Integer>) o.getO()))
				.put("results", ExistenceChecker.isSet((List<Integer>) o.getResults()))
				.put("ref", ExistenceChecker.isSet((C27Ref) o.getRef()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C27Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C27Holder", path, "");
		}
		return failure("C27Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C27Holder", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
