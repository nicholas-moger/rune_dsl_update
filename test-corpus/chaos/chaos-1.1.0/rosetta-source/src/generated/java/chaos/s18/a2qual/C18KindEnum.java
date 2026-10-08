package chaos.s18.a2qual;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Enum-guard subject.
 * @version 1.0.0
 */
@RosettaEnum("C18KindEnum")
public enum C18KindEnum {

	@RosettaEnumValue(value = "Red") 
	RED("Red", null),
	
	@RosettaEnumValue(value = "Green") 
	GREEN("Green", null),
	
	@RosettaEnumValue(value = "Blue") 
	BLUE("Blue", null)
;
	private static Map<String, C18KindEnum> values;
	static {
        Map<String, C18KindEnum> map = new ConcurrentHashMap<>();
		for (C18KindEnum instance : C18KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C18KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C18KindEnum fromDisplayName(String name) {
		C18KindEnum value = values.get(name);
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
