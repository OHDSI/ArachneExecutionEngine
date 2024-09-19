package com.odysseusinc.datasourcemanager.encryption;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.core.env.Environment;

import java.util.Objects;

@Slf4j
public class EncryptorUtils {

	public static final String PREFIX = "ENC(";
	public static final String SUFFIX = ")";

	private EncryptorUtils(){}

	public static PBEStringEncryptor buildStringEncryptor(Environment env) {

		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
		encryptor.setProvider(new BouncyCastleProvider());
		encryptor.setProviderName("BC");
		encryptor.setAlgorithm(env.getRequiredProperty("jasypt.encryptor.algorithm"));
		if (StringUtils.equals("PBEWithMD5AndDES", env.getRequiredProperty("jasypt.encryptor.algorithm"))) {
			log.warn("Warning:  encryption algorithm set to PBEWithMD5AndDES, which is not considered a strong encryption algorithm.  You may use PBEWITHSHA256AND128BITAES-CBC-BC, but will require special JVM configuration to support these stronger methods.");
		}
		encryptor.setKeyObtentionIterations(1000);
		String password = env.getRequiredProperty("jasypt.encryptor.password");
		if (StringUtils.isNotEmpty(password)) {
			encryptor.setPassword(password);
		}
		return encryptor;
	}

	public static String decrypt(StringEncryptor encryptor, String value) {

		if (Objects.isNull(encryptor) || encryptor instanceof NotEncrypted) {
			return value;
		}
		if (Objects.isNull(value) || !value.startsWith(PREFIX)) {
			return value;
		}
		String encryptedData = value.substring(PREFIX.length(), value.length() - SUFFIX.length());
		return encryptor.decrypt(encryptedData);
	}

	public static String encrypt(StringEncryptor encryptor, String value) {

		if (Objects.isNull(encryptor) || encryptor instanceof NotEncrypted) {
			return value;
		}
		if (Objects.isNull(value)) {
			return value;
		}
		return PREFIX + encryptor.encrypt(value) + SUFFIX;
	}
}
