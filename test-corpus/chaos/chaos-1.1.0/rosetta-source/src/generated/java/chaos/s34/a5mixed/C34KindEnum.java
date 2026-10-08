package chaos.s34.a5mixed;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The annotation attribute&#39;s enum.
 * @version 1.0.0
 */
@RosettaEnum("C34KindEnum")
public enum C34KindEnum {

	@RosettaEnumValue(value = "Alpha") 
	ALPHA("Alpha", null),
	
	@RosettaEnumValue(value = "Beta") 
	BETA("Beta", null)
;
	private static Map<String, C34KindEnum> values;
	static {
        Map<String, C34KindEnum> map = new ConcurrentHashMap<>();
		for (C34KindEnum instance : C34KindEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C34KindEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C34KindEnum fromDisplayName(String name) {
		C34KindEnum value = values.get(name);
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
