package com.odysseusinc.arachne.execution_engine_common.api.v1.dto;

public enum KerberosAuthMechanism {
    PASSWORD, KEYTAB, DEFAULT;

    public static KerberosAuthMechanism getByName(String name) {

        for (KerberosAuthMechanism auth : values()) {
            if (auth.toString().equals(name.toUpperCase())) {
                return auth;
            }
        }
        return DEFAULT;
    }
}
