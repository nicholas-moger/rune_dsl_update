package com.regnosys.rosetta.generator.java.types;

/**
 * Operation types for POJO property getter/setter naming.
 * Used by {@link JavaPojoProperty#getOperationName} to compute
 * method names like {@code getFoo}, {@code setFoo}, {@code addFoo}, etc.
 */
public enum JavaPojoPropertyOperationType {
    GET("get"),
    GET_OR_CREATE("getOrCreate"),

    SET("set"),
    SET_VALUE("set", "Value"),

    ADD("add"),
    ADD_VALUE("add", "Value");

    private final String prefix;
    private final String postfix;

    JavaPojoPropertyOperationType(String prefix) {
        this(prefix, "");
    }

    JavaPojoPropertyOperationType(String prefix, String postfix) {
        this.prefix = prefix;
        this.postfix = postfix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPostfix() {
        return postfix;
    }
}
