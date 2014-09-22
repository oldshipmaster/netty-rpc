package com.blackcrystalinfo.platform.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.IHandler;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.exception.InternalException;
import com.blackcrystalinfo.platform.powersocket.dao.DataHelper;
import com.blackcrystalinfo.platform.powersocket.dao.pojo.user.UserRegisterResponse;
import com.blackcrystalinfo.platform.util.CometScanner;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.HttpUtil;
import com.blackcrystalinfo.platform.util.PBKDF2;

public class UserRegisterHandler extends HandlerAdapter  {

	private static final Logger logger = LoggerFactory.getLogger(UserRegisterHandler.class);

	@Override
	public Object rpc(RpcRequest req) throws InternalException {

		UserRegisterResponse result = new UserRegisterResponse();
		result.setStatus(-1);
		result.setStatusMsg("");
		result.setUrlOrigin(req.getUrlOrigin());

		String email = HttpUtil.getPostValue(req.getParams(), "email");
		String phone = HttpUtil.getPostValue(req.getParams(), "phone");
		String pwd = HttpUtil.getPostValue(req.getParams(), "passwd");

		logger.info("UserRegisterHandler begin email:{}|phone:{}|passwd:{}", email,phone,pwd);
		
		
		if (StringUtils.isBlank(email)) {
			result.setStatus(1);
			logger.info("email is null. email:{}|phone:{}|passwd:{}|status:{}", email,phone,pwd,result.getStatus());
			return result;
		}
		// if(StringUtils.isBlank(phone)){
		// result.setStatus(2);
		// return result;
		// }
		if (StringUtils.isBlank(pwd)) {
			result.setStatus(3);
			logger.info("pwd is null. email:{}|phone:{}|passwd:{}|status:{}", email,phone,pwd,result.getStatus());
			return result;
		}

		Jedis jedis = null;
		try {
			jedis = DataHelper.getJedis();
			email=email.toLowerCase();
			// 1. 邮箱是否注册
			String existId = jedis.hget("user:mailtoid", email);
			if (null != existId) {
				result.setStatus(4);
				logger.info("user has been existed. email:{}|phone:{}|passwd:{}|status:{}", email,phone,pwd,result.getStatus());
				return result;
			}

			// 2. 生成用户ID

			String userId = String.valueOf(jedis.incrBy("user:nextid", 16));
			
			long intUserId = Long.parseLong(userId);
			
			if(intUserId%16>0){
				logger.info("userId can not modulo 16",userId);
				userId = String.valueOf(intUserId - intUserId%16);
			}

			Transaction tx = jedis.multi();

			// 3. 记录<邮箱，用户Id>
			tx.hset("user:mailtoid", email, userId);

			// 4. 记录<用户Id，邮箱>
			tx.hset("user:email", userId, email);

			// 5. 记录<用户Id，电话号码>
			tx.hset("user:phone", userId, phone);

			// 6. 记录<用户Id，密码>
			String shadow = PBKDF2.encode(pwd);
			tx.hset("user:shadow", userId, shadow);

			result.setStatus(0);
			result.setUrlOrigin(req.getUrlOrigin());

//			String cookie = CookieUtil.encode(userId, CookieUtil.EXPIRE_SEC);
//			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis() / 1000), CookieUtil.EXPIRE_SEC);
//			String proxyAddr = CometScanner.take();
//			logger.info("proxykey:{} | size:{} | proxyAddr:{} ", proxyKey, proxyKey.getBytes().length, proxyAddr);
//			result.setUserId(userId);
//			result.setHeartBeat(CookieUtil.EXPIRE_SEC);
//			result.setCookie(cookie);
//			result.setProxyKey(proxyKey);
//			result.setProxyAddr(proxyAddr);

			tx.exec();
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(jedis);
			logger.error("User regist error, email:{}|phone:{}|passwd:{}|status:{}", email,phone,pwd,result.getStatus(),e);
			
		} finally {
			DataHelper.returnJedis(jedis);
		}

		logger.info("response: email:{}|phone:{}|passwd:{}|status:{}", email,phone,pwd,result.getStatus());
		return result;

	}

}
