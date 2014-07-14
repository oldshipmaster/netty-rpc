package com.github.com.nettyrpc.powersocket.handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.DataHelper;
import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class DeviceNameModifying implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(DeviceNameModifying.class);
	@Override
	public Object rpc(RpcRequest req) throws InternalException {
		DeviceNameModifyingResponse resp = new DeviceNameModifyingResponse();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		
		String mac = HttpUtil.getPostValue(req.getParams(), "mac");
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		String newDeviceName = HttpUtil.getPostValue(req.getParams(), "deviceName");
		
		if(StringUtils.isBlank(mac)||StringUtils.isBlank(cookie)||StringUtils.isBlank(newDeviceName)){
			resp.setStatus(1);
			return resp;
		}
		Jedis jedis = null;
		try{
			jedis = DataHelper.getJedis();
			String deviceId = jedis.hget("device:mactoid", mac);
			if(deviceId==null){
				resp.setStatus(1);
				return resp;
			}
			String[] cookies = CookieUtil.decode(cookie);
			String userId = cookies[0];
				
			jedis.hset("user:device:name", userId+"_"+deviceId, newDeviceName);
			resp.setStatus(0);
		}catch(Exception e){
			resp.setStatus(-1);
			DataHelper.returnBrokenJedis(jedis);
			logger.info("",e);
			return resp;
		}finally{
			DataHelper.returnJedis(jedis);
		}
		return resp;
	}
	private class DeviceNameModifyingResponse extends ApiResponse{
		
	}
}
