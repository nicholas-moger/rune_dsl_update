package chaos.s02.base;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Extends across a block boundary.
 * @version 1.0.0
 */
@RosettaEnum("C2ExtEnum")
public enum C2ExtEnum {

	@RosettaEnumValue(value = "Alpha") 
	ALPHA("Alpha", null),
	
	@RosettaEnumValue(value = "Beta", displayName = "B") 
	BETA("Beta", "B"),
	
	/**
	 * Third letter.
	 */
	@RosettaEnumValue(value = "Gamma", displayName = "G3") 
	GAMMA("Gamma", "G3")
;
	private static Map<String, C2ExtEnum> values;
	static {
        Map<String, C2ExtEnum> map = new ConcurrentHashMap<>();
		for (C2ExtEnum instance : C2ExtEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C2ExtEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C2ExtEnum fromDisplayName(String name) {
		C2ExtEnum value = values.get(name);
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
