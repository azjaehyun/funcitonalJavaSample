package com.hkmc.multimedia.streaming.genie.service;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.hkmc.multimedia.common.Const;
import com.hkmc.multimedia.common.client.StreamingServiceClient;
import com.hkmc.multimedia.common.exception.GlobalCCSException;
import com.hkmc.multimedia.common.util.CommonUtil;
import com.hkmc.multimedia.common.util.SteamingUtil;
import com.hkmc.multimedia.streaming.genie.model.GenieRealtimeChartDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieStreamingLogDTO;
import com.hkmc.multimedia.streaming.genie.model.RunSample;
import com.hkmc.multimedia.streaming.genie.util.GenieConvertUtil;
import com.hkmc.multimedia.streaming.model.CommonRequestVO;
import com.hkmc.multimedia.streaming.model.StreamingLogRequestVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RefreshScope
public class GenieServiceImpl2{

	@Autowired
	StreamingServiceClient streamingServiceClient;

	@Autowired
	private Environment env;

	@Value("${api.brand1.url}")		// brand 1 = ginie
	String baseUrl;

	
	public <T> Map<String, Object> run(RunSample<T> sample) {
		Map<String, Object> resBody = null;
		try {
			resBody = sample.getResult();
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("itemSize", Const.ZERO);
				resBody.put("resultBrand", sample.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}catch(Exception ex) {
			resBody.put("resultCode", "E00004");
			resBody.put("resultMessage", ex.getMessage());
			resBody.put("itemSize", Const.ZERO);
			resBody.put("resultBrand", sample.getBrand());
		}
		return resBody;
	}
	
	public Map<String, Object> getRealtimeChart(Map<String, Object> header, CommonRequestVO body) {
		RunSample<CommonRequestVO> runSample = RunSample.create(header, body, body.getBrand());
		
		runSample.setFeginClientResponseBody((fHeader, fBody) -> { 
			String path = env.getProperty("api.brand" + body.getBrand() + ".realtimechart.path");
			Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieRealtimeChartDTO.class, header, body);
			
			ResponseEntity<Map<String, Object>> response = null;	
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			return response;
		});
		
		runSample.setGetResponseBody((brand, callResponse) -> { 
			Map<String, Object> resBody;
			if(StringUtils.equalsAny(String.valueOf(callResponse.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.realtimechart(callResponse.getBody());
			}else {
				resBody = SteamingUtil.defaultErrorCode(brand);
			}
			return resBody;
		});
		
		return run(runSample);
	}

	
	
	public Map<String, Object> sendStreamingLog(Map<String, Object> header, StreamingLogRequestVO body) throws GlobalCCSException{
		RunSample<StreamingLogRequestVO> runSample = RunSample.create(header, body, body.getBrand());
		
		runSample.setFeginClientResponseBody((fHeader, fBody) -> { 
			String path = env.getProperty("api.brand" + fBody.getBrand() + ".playlist.path");
			GenieStreamingLogDTO dto = CommonUtil.makePostBody(GenieStreamingLogDTO.class, fHeader, fBody);
			ResponseEntity<Map<String, Object>> response = streamingServiceClient.post(URI.create(baseUrl), fHeader, dto, path);
			return response;
		});
		
		runSample.setGetResponseBody((brand, callResponse) -> { 
			Map<String, Object> resBody;
			if(StringUtils.equalsAny(String.valueOf(callResponse.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.playList(callResponse.getBody());
			}else {
				resBody = SteamingUtil.defaultErrorCode(brand);
			}
			return resBody;
		});
		
		return run(runSample);
	}	

}
