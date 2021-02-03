package com.hkmc.multimedia.streaming.genie.model;

import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.http.ResponseEntity;

import lombok.Data;

@Data
public class RunSample<T> {
	
	private BiFunction<Map<String, Object>, T, ResponseEntity<Map<String, Object>>> feginClientResponseBody;
	private BiFunction<Integer, ResponseEntity<Map<String, Object>>, Map<String, Object>> getResponseBody;
	

	private Map<String, Object> header;
	
	private T body;
	
	private int brand;
	
	public static <T> RunSample<T> create(Map<String, Object> header, T body, int brand) {
		return new RunSample<T> (header, body, brand);
	}

	public RunSample(Map<String, Object> header, T body, int brand) {
		this.header = header;
		this.body = body;
		this.brand = brand;
	}
	
	public Map<String, Object> getResult(){
		
		ResponseEntity<Map<String, Object>> response = getFeginClientResponseBody().apply(getHeader(), getBody());
		return getGetResponseBody().apply(getBrand(), response);
	}

}
