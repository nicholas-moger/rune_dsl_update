package test.enumuni;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Every value carries a display name.
 * @version 1.0.0
 */
@RosettaEnum("AllDisplayedEnum")
public enum AllDisplayedEnum {

	@RosettaEnumValue(value = "One", displayName = "eins") 
	ONE("One", "eins"),
	
	@RosettaEnumValue(value = "Two", displayName = "zwei – ü") 
	TWO("Two", "zwei \u2013 \u00FC"),
	
	@RosettaEnumValue(value = "Three", displayName = "drei") 
	THREE("Three", "drei")
;
	private static Map<String, AllDisplayedEnum> values;
	static {
        Map<String, AllDisplayedEnum> map = new ConcurrentHashMap<>();
		for (AllDisplayedEnum instance : AllDisplayedEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	AllDisplayedEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static AllDisplayedEnum fromDisplayName(String name) {
		AllDisplayedEnum value = values.get(name);
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
