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
import com.hkmc.multimedia.streaming.genie.model.GenieFavoriteDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieFeeDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieLyricsDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieMusicSearchDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieMyPlaylistDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieMyPlaylistSongDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieNewSongDTO;
import com.hkmc.multimedia.streaming.genie.model.GeniePlayDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieProductDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieRealtimeChartDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieRecommendDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieRecommendDetailDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieSearchDetailDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieSearchVRDTO;
import com.hkmc.multimedia.streaming.genie.model.GenieStreamingLogDTO;
import com.hkmc.multimedia.streaming.genie.util.GenieConvertUtil;
import com.hkmc.multimedia.streaming.model.CommonRequestVO;
import com.hkmc.multimedia.streaming.model.FavoriteIsCheckedRequestVO;
import com.hkmc.multimedia.streaming.model.FavoriteMngmRequestVO;
import com.hkmc.multimedia.streaming.model.FeeMusicRequestVO;
import com.hkmc.multimedia.streaming.model.LyricsMusicRequestVO;
import com.hkmc.multimedia.streaming.model.MyPlaylistSongRequestVO;
import com.hkmc.multimedia.streaming.model.NewSongRequestVO;
import com.hkmc.multimedia.streaming.model.PlayRequestVO;
import com.hkmc.multimedia.streaming.model.RecommendDetailRequestVO;
import com.hkmc.multimedia.streaming.model.RecommendRequestVO;
import com.hkmc.multimedia.streaming.model.SearchDetailMusicRequestVO;
import com.hkmc.multimedia.streaming.model.SearchMusicRequestVO;
import com.hkmc.multimedia.streaming.model.StreamingLogRequestVO;
import com.hkmc.multimedia.streaming.model.VoiceSearchMusicRequestVO;
import com.hkmc.multimedia.streaming.service.StreamingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RefreshScope
public class GenieServiceImpl implements StreamingService {

	@Autowired
	StreamingServiceClient streamingServiceClient;

	@Autowired
	private Environment env;

	@Value("${api.brand1.url}")		// brand 1 = ginie
	String baseUrl;

	@Override
	public Map<String, Object> getServiceStatus(Map<String, Object> header) {
		return null;
	}

	@Override
	public Map<String, Object> getRealtimeChart(Map<String, Object> header, CommonRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> response = null;

		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
       
		String path = env.getProperty("api.brand" + body.getBrand() + ".realtimechart.path");
		try {
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieRealtimeChartDTO.class, header, body);
			
            StopWatch sw = new StopWatch("Genie getRealtimeChart Start");
            sw.start();
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			sw.stop();
			log.info("Genie getRealtimeChart Start Excute Time : {}",sw.getTotalTimeSeconds());			

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.realtimechart(response.getBody());
			}
			
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
			}else {
				throw new GlobalCCSException(500, e.getMessage());
			}
		}
		return resBody;
	}

	/*MSS-K (재생이력 등록)*/
	@Override
	public Map<String, Object> sendStreamingLog(Map<String, Object> header, StreamingLogRequestVO body) throws GlobalCCSException{
		Map<String, Object> resBody = new HashMap<String, Object>();
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		String path = env.getProperty("api.brand" + body.getBrand() + ".stmlog.path");

		try {
			//genie : POST
			//melon : 미지원
			GenieStreamingLogDTO dto = CommonUtil.makePostBody(GenieStreamingLogDTO.class, header, body);

			ResponseEntity<Map<String, Object>> response = streamingServiceClient.post(URI.create(baseUrl), header, dto, path);

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.streamingLog(response.getBody());
			}
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("itemSize", Const.ZERO);
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}catch(Exception ex) {
			resBody.put("resultCode", "E00004");
			resBody.put("resultMessage", ex.getMessage());
			resBody.put("itemSize", Const.ZERO);
			resBody.put("resultBrand", body.getBrand());
		}
		return resBody;
	}

	/*MSS-L (나의 재생목록)*/
	@Override
	public Map<String, Object> getPlaylist(Map<String, Object> header, CommonRequestVO body) throws GlobalCCSException {
		Map<String, Object> resBody = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> response = null;
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		String path = env.getProperty("api.brand" + body.getBrand() + ".playlist.path");

		try {
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieMyPlaylistDTO.class, header, body);
            
            StopWatch sw = new StopWatch("getPlaylist");
            sw.start();
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.playList(response.getBody());
			}
			sw.stop();
			log.info("Genie getPlaylist Start Excute Time : {}",sw.getTotalTimeSeconds());
			
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("itemSize", Const.ZERO);
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}

		}catch(Exception ex) {
			resBody.put("resultCode", "E00004");
			resBody.put("resultMessage", ex.getMessage());
			resBody.put("itemSize", Const.ZERO);
			resBody.put("resultBrand", body.getBrand());
		}
		return resBody;
	}

	/*MSS-M (나의 재생목록 곡 리스트)*/
	@Override
	public Map<String, Object> getPlaySonglist(Map<String, Object> header, MyPlaylistSongRequestVO body) throws GlobalCCSException {
		Map<String, Object> resBody = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> response = null;
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		String path = env.getProperty("api.brand" + body.getBrand() + ".playsonglist.path");


		try {
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieMyPlaylistSongDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.playSongList(response.getBody());
			}
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("itemSize", Const.ZERO);
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}catch(Exception ex) {
			ex.printStackTrace();

			resBody.put("resultCode", "E00004");
			resBody.put("resultMessage", ex.getMessage());
			resBody.put("itemSize", Const.ZERO);
			resBody.put("resultBrand", body.getBrand());
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getRecommend(Map<String, Object> header, RecommendRequestVO body) throws GlobalCCSException {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".recommend.path");
			String orgChartType = body.getChartType();
			String compareVal = "";
			
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieRecommendDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.recommend(response.getBody(), orgChartType);
			}
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getNewSongList(Map<String, Object> header, NewSongRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> response = null; 
				
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".newsonglist.path");
			
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieNewSongDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			
			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.newSongList(response.getBody());
			}
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getRecommendDetail(Map<String, Object> header, RecommendDetailRequestVO body) {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".recommenddetail.path");
            path = path.replace("{plm_seq}", String.valueOf(body.getRecommend_seq()));
            
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieRecommendDetailDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}            

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.recommendDetail(response.getBody());
			}

		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getProducts(Map<String, Object> header, CommonRequestVO body) {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".products.path");
            
			Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieProductDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}  

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.products(response.getBody());
			}

		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	// jh 작업 시작
	@Override
	public Map<String, Object> getFavorites(Map<String, Object> header, CommonRequestVO body) {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".favoritelist.path");

			Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieFavoriteDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}  
			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.favorites(response.getBody());
			}
		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E000f04");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getFavoriteMngm(Map<String, Object> header, FavoriteMngmRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());
		
		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".favoritemngm.path");
			path = path.replace("{song_id}", body.getSong_id());
						
			ResponseEntity<Map<String, Object>> response = streamingServiceClient.post(URI.create(baseUrl), header, path);

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.favoriteMngm(response.getBody());
			}

		}catch(GlobalCCSException e) {
			if(e.getCode() == -1) {
				resBody.put("resultCode", "E00004");
				resBody.put("resultMessage", e.getMessage());
				resBody.put("resultBrand", body.getBrand());
			}else {
				throw new GlobalCCSException(500);
			}
		}
		return resBody;
	}

	@Override
	public Map<String, Object> getFavoriteIsChecked(Map<String, Object> header, FavoriteIsCheckedRequestVO body) {
		// 미지원
		Map<String, Object> resBody = new HashMap<String, Object>();
		return resBody;
	}

	/**
	 * 뮤직검색/곡/앨범/아티스트/playlist
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getSearchMusic(Map<String, Object> header, SearchMusicRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		try {
			String path = env.getProperty("api.brand" + Integer.toString(body.getBrand()) + ".searchitem.path");
			GenieMusicSearchDTO dto = CommonUtil.makePostBody(GenieMusicSearchDTO.class, header, body);

			ResponseEntity<Map<String, Object>> response = streamingServiceClient.post(URI.create(baseUrl), header, dto, path);

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody.put("item", GenieConvertUtil.searchMusic(response.getBody()));
			}
		} catch (Exception e) {
			throw new GlobalCCSException(500);
		}
		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

	/**
	 * 재생요청
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getPlayMusic(Map<String, Object> header, PlayRequestVO body) {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".streaming.path");
			path = path.replace("{song_id}", body.getSong_id());

			Map<String, Object> paramMap = CommonUtil.makeGetParam(GeniePlayDTO.class, header, body);
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.playMusic(response.getBody(), header.getOrDefault(Const.TELECOM_STATUS, "").toString());
			}

		} catch (Exception e) {
			throw new GlobalCCSException(500);
		}
		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

	/**
	 * 정산로그요청
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getFeeMusic(Map<String, Object> header, FeeMusicRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		String path = env.getProperty("api.brand" + body.getBrand() + ".logging.path");


		try {

			GenieFeeDTO dto = CommonUtil.makePostBody(GenieFeeDTO.class, header, body);
			ResponseEntity<Map<String, Object>> response = streamingServiceClient.post(URI.create(baseUrl), header, dto, path);

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.feeMusic(response.getBody());
			}

		} catch (Exception e) {

			throw new GlobalCCSException(500);
		}

		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

	/**
	 * 가사조회요청
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getLyricsMusic(Map<String, Object> header, LyricsMusicRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		String path = env.getProperty("api.brand" + Integer.toString(body.getBrand()) + ".lyrics.path");
		ResponseEntity<Map<String, Object>> response = null; 

		try {
			path = path.replace("{song_id}", body.getSong_id());
			//path = CommonUtil.makeGetPath(GenieLyricsDTO.class, header, body, path);
			
            Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieLyricsDTO.class, header, body);
			StopWatch sw = new StopWatch("Genie getLyricsMusic Start");
			sw.start();
			
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}
			sw.stop();
			log.info("Genie getLyricsMusic Start Excute Time : {}",sw.getTotalTimeSeconds());
			
			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.lyricsMusic(response.getBody());
			}

		} catch (Exception e) {
			throw new GlobalCCSException(500,e.getMessage());
		}

		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

	/**
	 * 음성검색/곡/앨범/아티스트/tag/keyword
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getVoiceSearchMusic(Map<String, Object> header, VoiceSearchMusicRequestVO body) {
		ResponseEntity<Map<String, Object>> response = null;
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());


		try {
			String path = env.getProperty("api.brand" +Integer.toString(body.getBrand()) + ".search.path");
			Map<String, Object> paramMap = CommonUtil.makeGetParam(GenieSearchVRDTO.class, header, body);
			
			if(paramMap.isEmpty()){
				response = streamingServiceClient.get(URI.create(baseUrl), header, path);	
			}else {
				response = streamingServiceClient.getParam(URI.create(baseUrl), header, path, paramMap);				
			}  

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {

				resBody = GenieConvertUtil.voiceSearchMusic(response.getBody());
			}
		} catch (Exception e) {
			throw new GlobalCCSException(500);
		}
		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

	/**
	 * 상세검색
	 *
	 * @param header,body
	 * @return Map<String, Object>
	 * @throws GlobalCCSException
	 */
	@Override
	public Map<String, Object> getSearchDetailMusic(Map<String, Object> header,  SearchDetailMusicRequestVO body) {
		Map<String, Object> resBody = new HashMap<String, Object>();
		
		resBody.put("resultCode", Const.GENIE_ERR_CODE);
		resBody.put("resultMessage", Const.GENIE_ERR_MESSAGE);
		resBody.put("itemSize", 0);
		resBody.put("resultBrand", body.getBrand());

		ResponseEntity<Map<String, Object>> response = null;

		try {
			String path = env.getProperty("api.brand" + body.getBrand() + ".searchdetail.path");
			GenieSearchDetailDTO dto = CommonUtil.makePostBody(GenieSearchDetailDTO.class, header, body);
			
			response = streamingServiceClient.post(URI.create(baseUrl), header, dto, path);

			if(StringUtils.equalsAny(String.valueOf(response.getStatusCodeValue()), String.valueOf(HttpStatus.OK.value()), String.valueOf(HttpStatus.UNAUTHORIZED.value()))) {
				resBody = GenieConvertUtil.searchDetailMusic(response.getBody());
			}
		} catch (Exception e) {
			throw new GlobalCCSException(500);
		}
		log.info(">>>>>>>>>>>> resBody : {}", resBody.toString());
		return resBody;
	}

}
