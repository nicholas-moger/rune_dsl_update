package chaos.s26.a1o4;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Enum-guard subject.
 * @version 1.0.0
 */
@RosettaEnum("C26KindEnum")
public enum C26KindEnum {

	@RosettaEnumValue(value = "Red") 
	RED("Red", null),
	
	@RosettaEnumValue(value = "Green") 
	GREEN("Green", null),
	
	@RosettaEnumValue(value = "Blue") 
	BLUE("Blue", null)
;
	private static Map<String, C26KindEnum> values;
	static {
        Map<String, C26KindEnum> map = new ConcurrentHashMap<>();
		for (C26KindEnum instance : C26KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C26KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C26KindEnum fromDisplayName(String name) {
		C26KindEnum value = values.get(name);
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
