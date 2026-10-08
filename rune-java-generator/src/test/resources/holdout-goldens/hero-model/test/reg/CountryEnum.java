package test.reg;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version test
 */
@RosettaEnum("CountryEnum")
public enum CountryEnum {

	@RosettaEnumValue(value = "UnitedStatesOfAmerica") 
	UNITED_STATES_OF_AMERICA("UnitedStatesOfAmerica", null)
;
	private static Map<String, CountryEnum> values;
	static {
        Map<String, CountryEnum> map = new ConcurrentHashMap<>();
		for (CountryEnum instance : CountryEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	CountryEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static CountryEnum fromDisplayName(String name) {
		CountryEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
