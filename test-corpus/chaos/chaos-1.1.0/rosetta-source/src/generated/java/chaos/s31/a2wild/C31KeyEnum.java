package chaos.s31.a2wild;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Enum values named like Java keywords.
 * @version 1.0.0
 */
@RosettaEnum("C31KeyEnum")
public enum C31KeyEnum {

	@RosettaEnumValue(value = "class") 
	CLASS("class", null),
	
	@RosettaEnumValue(value = "long") 
	LONG("long", null),
	
	@RosettaEnumValue(value = "new") 
	NEW("new", null),
	
	@RosettaEnumValue(value = "null") 
	NULL("null", null)
;
	private static Map<String, C31KeyEnum> values;
	static {
        Map<String, C31KeyEnum> map = new ConcurrentHashMap<>();
		for (C31KeyEnum instance : C31KeyEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31KeyEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31KeyEnum fromDisplayName(String name) {
		C31KeyEnum value = values.get(name);
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
