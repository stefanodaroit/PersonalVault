package app.core;

public final class Constants {

  private Constants() {}

  public static final int ITERATIONS = 210000;                                // Number of iterations
  public static final int DERIVED_KEY_LENGTH = 512;                           // Master key length

  public static final int MIN_PASSWORD_LENGTH = 12;                           // Minimum password length
  public static final int MAX_PASSWORD_LENGTH = 64;                           // Maximum password length
  public static final int SALT_LENGTH = 128;                                  // Salt length

  public static final String ALG_WRAP_KEYS  = "AESWrap";
  public static final String PROV_WRAP_KEYS = "SunJCE";
}