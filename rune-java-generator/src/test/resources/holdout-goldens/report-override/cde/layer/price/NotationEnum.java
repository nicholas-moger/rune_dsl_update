package cde.layer.price;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version 0.0.0
 */
@RosettaEnum("NotationEnum")
public enum NotationEnum {

	@RosettaEnumValue(value = "W") 
	W("W", null),
	
	@RosettaEnumValue(value = "X") 
	X("X", null),
	
	@RosettaEnumValue(value = "Y") 
	Y("Y", null),
	
	@RosettaEnumValue(value = "Z") 
	Z("Z", null)
;
	private static Map<String, NotationEnum> values;
	static {
        Map<String, NotationEnum> map = new ConcurrentHashMap<>();
		for (NotationEnum instance : NotationEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	NotationEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static NotationEnum fromDisplayName(String name) {
		NotationEnum value = values.get(name);
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
