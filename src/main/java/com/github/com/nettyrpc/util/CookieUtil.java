package com.github.com.nettyrpc.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @author j
 * 
 */
@SuppressWarnings("restriction")
public class CookieUtil {

	/**
	 * cookie过期时间
	 */
	public static final String EXPIRE_SEC = Constants.getProperty("expire", "300");

	/**
	 * 用户认证
	 */
	public static final String USER_SALT = Constants.getProperty("USER_SALT", "black_crystal");

	/**
	 * 设备cookie生成的盐
	 */
	public static final String DEVICE_SALT = Constants.getProperty("DEVICE_SALT", "BlackCrystalDevice14529");
	/**
	 * WEBSOCKET的盐
	 */
	public static final String WEBSOCKET_SALT = Constants.getProperty("WEBSOCKET_SALT", "BlackCrystalWb14527");

	/**
	 * 生成cookie
	 * 
	 * @param userId
	 *            用户Id
	 * @return cookie
	 * @throws NoSuchAlgorithmException
	 */
	public static String encode(String userId, String expire) throws NoSuchAlgorithmException {
		String cookie = "";

		String sha1Value = encodeSha1(userId, expire);

		String beforeAes = String.format("%s|%s|%s", userId, expire, sha1Value);

		BASE64Encoder encoder = new BASE64Encoder();

		cookie = encoder.encode(beforeAes.getBytes());

		cookie = cookie.replace("+", "%2B");

		return cookie;
	}

	public static void main(String[] args) {
		String s = "gD BOoD";

	}

	/**
	 * 解析用户Id
	 * 
	 * @param cookie
	 *            cookie
	 * @return 用户Id
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String[] decode(String cookie) throws NoSuchAlgorithmException, IOException,Exception {

		cookie = cookie.replace("%2B", "+");

		BASE64Decoder decoder = new BASE64Decoder();

		byte[] cookieStrByte = decoder.decodeBuffer(cookie);

		String beforeAes = new String(cookieStrByte);

		String[] parts = beforeAes.split("\\|");

		if(parts.length<3){
			throw new ArrayIndexOutOfBoundsException("parts's length less than three.");
		}
		String userId = "";
		String expire = "";
		String sha1Value = "";

		userId = parts[0];
		expire = parts[1];
		sha1Value = parts[2];

		if (!sha1Value.equals(encodeSha1(userId, expire))) {
			return null;
		}

		return new String[] { userId, expire };
	}

	public static String generateDeviceKey(String mac, String id) throws NoSuchAlgorithmException, InvalidKeyException {
		String cookie = "";

		Mac hmac = Mac.getInstance("HmacSHA256");
		SecretKey secret = new SecretKeySpec(DEVICE_SALT.getBytes(), "HMACSHA256");
		hmac.init(secret);

		byte[] doFinal = hmac.doFinal(mac.getBytes());

		BASE64Encoder encoder = new BASE64Encoder();
		String macEncode = encoder.encode(doFinal);

		cookie = String.format("%s|%s", id, macEncode);

		cookie = cookie.replace("+", "%2B");

		return cookie;
	}

	public static String extractDeviceId(String cookie) throws IOException {
		cookie = cookie.replace("%2B", "+");

		String[] parts = cookie.split("\\|");

		if (null == parts || parts.length < 2) {
			return null;
		}

		String deviceId = "";
		deviceId = parts[0];

		return deviceId;
	}

	public static String generateKey(String id, String timestamp, String expire) throws NoSuchAlgorithmException {
		String key = "";
		String buf = String.format("%s|%s|%s", id, timestamp, expire);

		String md5_buf = String.format("%s|%s|%s|%s", id, timestamp, expire, WEBSOCKET_SALT);
		byte[] bytes = MessageDigest.getInstance("MD5").digest(md5_buf.getBytes());

		BASE64Encoder encoder = new BASE64Encoder();
		String md5Str = encoder.encode(bytes);

		key = String.format("%s|%s", buf, md5Str);
		return key;
	}

	public static boolean verifyDeviceKey(String mac, String cookie) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		boolean result = false;

		String id = extractDeviceId(cookie);
		if (null != cookie) {
			cookie = cookie.replace("+", "%2B");
			if (cookie.equals(generateDeviceKey(mac, id))) {
				result = true;
			}
		}

		return result;
	}

	private static String encodeSha1(String id, String expire) throws NoSuchAlgorithmException {
		String result = "";

		String beforeSha1 = String.format("%s|%s|%s", id, expire, USER_SALT);
		result = sha1(beforeSha1);

		return result;
	}

	private static String sha1(String str) throws NoSuchAlgorithmException {
		String result = "";

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(str.getBytes());
		result = bytetoString(digest);

		return result;
	}

	private static String bytetoString(byte[] digest) {
		String str = "";
		String tempStr = "";

		for (int i = 1; i < digest.length; i++) {
			tempStr = (Integer.toHexString(digest[i] & 0xff));
			if (tempStr.length() == 1) {
				str = str + "0" + tempStr;
			} else {
				str = str + tempStr;
			}
		}
		return str.toLowerCase();
	}

}
