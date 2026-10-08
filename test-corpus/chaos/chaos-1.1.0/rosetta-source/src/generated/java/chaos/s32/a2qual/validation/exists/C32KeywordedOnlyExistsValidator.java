package chaos.s32.a2qual.validation.exists;

import chaos.s32.a2qual.C32Keyworded;
import chaos.s32.a2qual.h.C32Aux;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C32KeywordedOnlyExistsValidator implements ValidatorWithArg<C32Keyworded, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C32Keyworded> ValidationResult<C32Keyworded> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("class", ExistenceChecker.isSet((String) o._getClass()))
				.put("long", ExistenceChecker.isSet((BigDecimal) o.getLong()))
				.put("char", ExistenceChecker.isSet((String) o.getChar()))
				.put("this", ExistenceChecker.isSet((String) o.getThis()))
				.put("new", ExistenceChecker.isSet((List<String>) o.getNew()))
				.put("private", ExistenceChecker.isSet((String) o.getPrivate()))
				.put("return", ExistenceChecker.isSet((BigDecimal) o.getReturn()))
				.put("package", ExistenceChecker.isSet((String) o.getPackage()))
				.put("null", ExistenceChecker.isSet((String) o.getNull()))
				.put("var", ExistenceChecker.isSet((List<String>) o.getVar()))
				.put("abstract", ExistenceChecker.isSet((Boolean) o.getAbstract()))
				.put("throws", ExistenceChecker.isSet((String) o.getThrows()))
				.put("aux", ExistenceChecker.isSet((C32Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C32Keyworded", ValidationResult.ValidationType.ONLY_EXISTS, "C32Keyworded", path, "");
		}
		return failure("C32Keyworded", ValidationResult.ValidationType.ONLY_EXISTS, "C32Keyworded", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
