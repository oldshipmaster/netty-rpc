package com.blackcrystalinfo.platform.powersocket.api;

import static com.blackcrystalinfo.platform.util.ErrorCode.SUCCESS;
import static com.blackcrystalinfo.platform.util.ErrorCode.SYSERROR;
import static com.blackcrystalinfo.platform.util.RespField.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.blackcrystalinfo.platform.HandlerAdapter;
import com.blackcrystalinfo.platform.RpcRequest;
import com.blackcrystalinfo.platform.annotation.Path;
import com.blackcrystalinfo.platform.util.CookieUtil;
import com.blackcrystalinfo.platform.util.DataHelper;


@Path(path="/mobile/devices")
public class DevicesApi extends HandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(DevicesApi.class);

	@Override
	public Object rpc(RpcRequest req) throws Exception {
		Map<Object,Object> r = new HashMap<Object,Object>();
		r.put(status, SYSERROR.toString());
		
		String userId = CookieUtil.gotUserIdFromCookie(req.getParameter( "cookie"));
		
		Jedis j = null;
		try {
			j = DataHelper.getJedis();
			List<Map<Object,Object>> bindedDevices = new ArrayList<Map<Object,Object>>();
			
			String family = j.hget("user:family", userId);
			if(StringUtils.isNotBlank(family)){
				Set<String>mems = j.smembers("family:"+family);
				for(String m : mems){
					Set<String>devices = j.smembers("u:"+m+":devices");
					for(String id : devices){
						Map<Object,Object> devData = new HashMap<Object,Object>();
						String mac = j.hget("device:mac", id);
						String name = j.hget("device:name", id);
						String pwd = j.hget("device:pwd:" + userId, id);
						String dv = j.hget("device:dv", id);

						name = (null == name ? "default" : name);

						devData.put("deviceId",id);
						devData.put("mac",mac);
						devData.put("deviceName",name);
						devData.put("pwd",pwd);
						devData.put("deviceType",dv);
//						devData.put("family",family);
						bindedDevices.add(devData);
					}
				}
			}else{
				Set<String>devices = j.smembers("u:"+userId+":devices");
				for(String id : devices){
					Map<Object,Object> devData = new HashMap<Object,Object>();
					String mac = j.hget("device:mac", id);
					String name = j.hget("device:name", id);
					String pwd = j.hget("device:pwd:" + userId, id);
					String dv = j.hget("device:dv", id);

					name = (null == name ? "default" : name);

					devData.put("deviceId",id);
					devData.put("mac",mac);
					devData.put("deviceName",name);
					devData.put("pwd",pwd);
					devData.put("deviceType",dv);
//					devData.put("family",family);
					bindedDevices.add(devData);
				}
			}
			
			r.put("bindedDevices",bindedDevices);
		} catch (Exception e) {
			DataHelper.returnBrokenJedis(j);
			logger.error("",e);
			return r;
		} finally {
			DataHelper.returnJedis(j);
		}

		r.put(status,SUCCESS.toString());
		return r;
	}
}
