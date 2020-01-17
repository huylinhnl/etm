package com.jecstar.etm.server.core.domain.configuration;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.converter.JsonEntityConverter;
import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.converter.custom.EnumConverter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

public class License {

    private static final String PUBLIC_KEY = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAtBCraSZNjqfqnDK/ESEqwZWZiDY6YRe72N8id//B5LHv7eO41cgRrKzAIn+WH10C3jOjGpJjF1RITKTJg1FM4CK+L66hYP3HQVX8ghtQT99TkHuTkTGxbbBMZd4VF77TR5mTa0LjMTGz7+r9q0PAQEGPol/WqaOTxGHiizh7/qmA0hvAA4Ff39T0CsFyWFpI4hmfS5JG/sLsG8WKd125A1VJFk76ZH7kWP1ysrzGzbR1vSQznQpzz7GPpbzFgjDWJpvQzLREv7qSn1z7MGD4YKlLpgaYxoPUsF2kg4N3YzvZw+RfMTFS2v689VmLccZbySXSoqXyssSq6oMlXIwDSus5qFaB1TeYFJWHZh/t6QHHYeyI0RW6pzIAAG/yGF9uX13uiIb9J9+Qu02XAPstl0ZsVfAVdbzV1AKFMPVOCzMHk6T8YcLsFKedigeH4K2vzdQyHC4L0oZ+2xYiDp904Y7A20HfTyBhVJmz7OIKLjJbnuCh8wP1g9VAR9NC468/nhEdCBxT2nHvvJMLzw2xUBYNIoSw5rWd5+nO9kiCWD7OoNpL5nTRRlX3jBpuqEJmszQo3wF0jZEqAi/pYn3c60iEljtx8m8K8EgjylS/C49qBDUfCQnwfNQxGjxzEzeFc9+mJRox87kxYMUsCyT5u46f8P1wfHOWxzubRgcr0hECAwEAAQ==";

    public static final String OWNER = "owner";
    public static final String START_DATE = "start_date";
    public static final String EXPIRY_DATE = "expiry_date";
    public static final String MAX_REQUEST_UNITS_PER_SECOND = "max_request_units_per_second";
    public static final String LICENSE_TYPE = "license_type";
    public static final Long UNLIMITED = -1L;
    /**
     * The number of bytes that make up 1 RU.
     */
    public static final long BYTES_PER_RU = 1024;
    /**
     * The number of request units per second you may consume when no license is installed.
     */
    public static final Long UNLICENSED_REQUEST_UNITS_PER_SECOND = 10L;
    /**
     * The number of bytes storage based on the license. For every request unit 10 MiB is assigned.
     */
    private static final long STORAGE_BYTES_PER_RU = 1024 * 1024 * 10;

    public enum LicenseType {
        CLOUD, ON_PREM;

        public static LicenseType safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return LicenseType.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

    }

    @JsonField(OWNER)
    private String owner;
    @JsonField(START_DATE)
    private Instant startDate;
    @JsonField(EXPIRY_DATE)
    private Instant expiryDate;
    @JsonField(MAX_REQUEST_UNITS_PER_SECOND)
    private Long maxRequestUnitsPerSecond;
    @JsonField(value = LICENSE_TYPE, converterClass = EnumConverter.class)
    private LicenseType licenseType = LicenseType.ON_PREM;

    public License(String licenseKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY));
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            Cipher decrpyptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decrpyptCipher.init(Cipher.DECRYPT_MODE, publicKey);

            byte[] decryptedBytes = decrpyptCipher.doFinal(Base64.getDecoder().decode(licenseKey.getBytes()));
            String license = new String(decryptedBytes);
            JsonEntityConverter<License> converter = new JsonEntityConverter<>(f -> this) {
            };
            converter.read(license);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | IllegalArgumentException e) {
            throw new EtmException(EtmException.INVALID_LICENSE_KEY, e);
        }
    }

    public String getOwner() {
        return this.owner;
    }

    public Instant getStartDate() {
        return this.startDate;
    }

    public Instant getExpiryDate() {
        return this.expiryDate;
    }

    public Long getMaxRequestUnitsPerSecond() {
        return this.maxRequestUnitsPerSecond;
    }

    public Long getMaxDatabaseSize() {
        if (getMaxRequestUnitsPerSecond().equals(UNLIMITED)) {
            return UNLIMITED;
        }
        return this.maxRequestUnitsPerSecond * STORAGE_BYTES_PER_RU;
    }

    public LicenseType getLicenseType() {
        return this.licenseType;
    }

    public boolean isExpiredAt(Instant moment) {
        return !moment.isAfter(getExpiryDate());
    }

    public boolean isValidAt(Instant moment) {
        var valid = !moment.isAfter(getExpiryDate());
        if (getStartDate() != null) {
            valid = valid && !moment.isBefore(getStartDate());
        }
        return valid;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof License) {
            var other = (License) obj;
            return Objects.equals(other.owner, this.owner)
                    && Objects.equals(other.startDate, this.startDate)
                    && Objects.equals(other.expiryDate, this.expiryDate)
                    && Objects.equals(other.maxRequestUnitsPerSecond, this.maxRequestUnitsPerSecond);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.owner, this.startDate, this.expiryDate, this.maxRequestUnitsPerSecond);
    }
}


