package com.github.com.nettyrpc.powersocket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.com.nettyrpc.IHandler;
import com.github.com.nettyrpc.RpcRequest;
import com.github.com.nettyrpc.exception.InternalException;
import com.github.com.nettyrpc.powersocket.dao.pojo.ApiResponse;
import com.github.com.nettyrpc.util.CookieUtil;
import com.github.com.nettyrpc.util.HttpUtil;

public class WebsocketInfoHandler implements IHandler {
	private static final Logger logger = LoggerFactory.getLogger(WebsocketInfoHandler.class);

	public Object rpc(RpcRequest req) throws InternalException {
		String cookie = HttpUtil.getPostValue(req.getParams(), "cookie");
		WebsocketInfoHandlerResponse resp = new WebsocketInfoHandlerResponse();
		resp.setStatusMsg("");
		resp.setUrlOrigin(req.getUrlOrigin());
		// 1. 校验cookie信息
		String[] infos = null;
		try {
			infos = CookieUtil.decode(cookie);
			String userId = infos[0];
			String heartBeat = CookieUtil.EXPIRE_SEC;
			String proxyKey = CookieUtil.generateKey(userId, String.valueOf(System.currentTimeMillis()/1000), CookieUtil.EXPIRE_SEC);
			
			resp.setProxyAddr(CookieUtil.WEBSOCKET_ADDR);
			resp.setHeartBeat(heartBeat);
			resp.setProxyKey(proxyKey);
			resp.setStatus(0);
		} catch (Exception e) {
			logger.error("System error occurs",e);
			resp.setStatus(-1);
			return resp;
		}

		return resp;
	}

	private class WebsocketInfoHandlerResponse extends ApiResponse {
		private String proxyAddr;
		private String proxyKey;
		private String heartBeat;
		public String getProxyAddr() {
			return proxyAddr;
		}
		public void setProxyAddr(String proxyAddr) {
			this.proxyAddr = proxyAddr;
		}
		public String getProxyKey() {
			return proxyKey;
		}
		public void setProxyKey(String proxyKey) {
			this.proxyKey = proxyKey;
		}
		public String getHeartBeat() {
			return heartBeat;
		}
		public void setHeartBeat(String heartBeat) {
			this.heartBeat = heartBeat;
		}
		
	}
}