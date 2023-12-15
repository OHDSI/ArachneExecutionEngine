package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

public enum AuthMethod {
    DEFAULT(0), KERBEROS(1), USERNAME(2), LDAP(3);

    private final int type;

    AuthMethod(int type) {
        this.type = type;
    }

    public static AuthMethod getByAuthType(Integer type) {

        for (AuthMethod auth : values()) {
            if (auth.getType() == type) {
                return auth;
            }
        }
        return DEFAULT;
    }

    public int getType() {
        return type;
    }
}
