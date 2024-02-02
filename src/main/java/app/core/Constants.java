package app.core;

public final class Constants {

  private Constants() {}

  public static final int ITERATIONS = 210000;                                // Number of iterations
  public static final int DERIVED_KEY_LENGTH = 512;                           // Master key length

  public static final int MIN_PASSWORD_LENGTH = 12;                           // Minimum password length
  public static final int MAX_PASSWORD_LENGTH = 64;                           // Maximum password length
  public static final int SALT_LENGTH = 128 / 8;                              // Salt length

  public static final String ALG_WRAP_KEYS  = "AESWrap";
  public static final String PROV_WRAP_KEYS = "SunJCE";

  public static final int    MAX_TOKEN_SIZE = 500;
  public static final String ALG_HMAC_TOK = "HmacSHA512";
  public static final char   PERIOD = '.';

  public static final String CHKSUM_FILE_EXT = ".mac";
  public static final String CONF_FILE_EXT   = ".vault";
  public static final String VAULT_NAME_RGX = "^[a-zA-Z0-9_ ]+$";

  public static final String[] PSW_EXCEPTION = { "(Short)", "(Long)", "(Special)", "(Upper)", "(Lower)", "(Digit)" };

  // File
  public static final int CHUNK_SIZE = 65536; // bytes 2^16
  public static final int FILENAME_MAX_SIZE = 256; // bytes
  public static final int IVLEN = 12; // bytes
  public static final String KEY_GEN_ALGO = "AES";
  public static final int KEY_SIZE_BITS = 256; // bits
  public static final int KEY_SIZE = KEY_SIZE_BITS / 8; // bytes
  public static final int TAG_LEN_BITS = 128; // bits
  public static final int TAG_LEN = TAG_LEN_BITS / 8; // bytes
}
