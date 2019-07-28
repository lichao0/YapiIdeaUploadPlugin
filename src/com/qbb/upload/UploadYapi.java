package com.qbb.upload;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qbb.constant.YapiConstant;
import com.qbb.dto.*;
import com.qbb.util.HttpClientUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 上传到yapi
 *
 * @author chengsheng@qbb6.com
 * @date 2019/1/31 11:41 AM
 */
public class UploadYapi {


    private Gson gson=new Gson();

    public static Map<String,Map<String,Integer>> catMap=new HashMap<>();

    /**
     * @description: 调用保存接口
     * @param: [yapiSaveParam, attachUpload, path]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */ 
    public YapiResponse  uploadSave(YapiSaveParam yapiSaveParam,String attachUpload,String path) throws IOException {
        if(Strings.isNullOrEmpty(yapiSaveParam.getTitle())){
            yapiSaveParam.setTitle(yapiSaveParam.getPath());
        }
        YapiHeaderDTO yapiHeaderDTO=new YapiHeaderDTO();
        if("form".equals(yapiSaveParam.getReq_body_type())){
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/x-www-form-urlencoded");
            yapiSaveParam.setReq_body_form(yapiSaveParam.getReq_body_form());
        }else{
            yapiHeaderDTO.setName("Content-Type");
            yapiHeaderDTO.setValue("application/json");
            yapiSaveParam.setReq_body_type("json");
        }
        if(Objects.isNull(yapiSaveParam.getReq_headers())){
            List list=new ArrayList();
            list.add(yapiHeaderDTO);
            yapiSaveParam.setReq_headers(list);
        }else{
            yapiSaveParam.getReq_headers().add(yapiHeaderDTO);
        }
        this.changeDesByPath(yapiSaveParam);
        YapiResponse yapiResponse= this.getCatIdOrCreate(yapiSaveParam);
        if(yapiResponse.getErrcode()==0){
            String response=HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpPost(yapiSaveParam.getYapiUrl()+YapiConstant.yapiSave,gson.toJson(yapiSaveParam))),"utf-8");
            return gson.fromJson(response,YapiResponse.class);
        }else{
            return yapiResponse;
        }
    }



    /**
     * 获得httpPost
     * @return
     */
    private HttpPost getHttpPost(String url, String body) {
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            httpPost.setHeader("Content-type", "application/json;charset=utf-8");
            HttpEntity reqEntity = new StringEntity(body == null ? "" : body, "UTF-8");
            httpPost.setEntity(reqEntity);
        } catch (Exception e) {
        }
        return httpPost;
    }

    /**
     * @description: 上传文件
     * @param: [url, filePath]
     * @return: java.lang.String
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */ 
    public String uploadFile(String url,String filePath){
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url);
            FileBody bin = new FileBody(new File(filePath));
            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", bin).build();
            httpPost.setEntity(reqEntity);
            return  HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(httpPost),"utf-8");
        } catch (Exception e) {
        }
        return "";
    }


    private HttpGet getHttpGet(String url){
        try {
            return HttpClientUtil.getHttpGet(url, "application/json", "application/json; charset=utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * @description: 获得描述
     * @param: [yapiSaveParam]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/7/28
     */ 
    public void changeDesByPath(YapiSaveParam yapiSaveParam){
        try{
            String response = HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpGet(yapiSaveParam.getYapiUrl()+ YapiConstant.yapiGetByPath+"?token="+yapiSaveParam.getToken()+"&path="+yapiSaveParam.getPath())),"utf-8");
            YapiResponse yapiResponse=gson.fromJson(response,YapiResponse.class);
            if(yapiResponse.getErrcode()==0) {
                YapiInterfaceResponse yapiInterfaceResponse=(YapiInterfaceResponse)yapiResponse.getData();
                if(!Strings.isNullOrEmpty(yapiInterfaceResponse.getDesc())){
                    //如果原来描述不为空，那么就将当前描述+上一个版本描述的自定义部分
                    yapiSaveParam.setDesc(yapiInterfaceResponse.getDesc().substring(0,yapiInterfaceResponse.getDesc().indexOf("<pre>"))+yapiSaveParam.getDesc()+yapiInterfaceResponse.getDesc().substring(yapiInterfaceResponse.getDesc().indexOf("</pre>"),yapiInterfaceResponse.getDesc().length()));
                }
                if(Objects.nonNull(yapiInterfaceResponse.getCatid())){
                    yapiSaveParam.setCatid(yapiInterfaceResponse.getCatid().toString());
                }
            }
        }catch (Exception e){

        }
    }

    /**
     * @description: 获得分类或者创建分类或者
     * @param: [yapiSaveParam]
     * @return: com.qbb.dto.YapiResponse
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/15
     */ 
    public YapiResponse getCatIdOrCreate(YapiSaveParam yapiSaveParam){
        Map<String,Integer> catMenuMap= catMap.get(yapiSaveParam.getProjectId().toString());
        if(catMenuMap!=null){
            if(!Strings.isNullOrEmpty(yapiSaveParam.getMenu())) {
                if(Objects.nonNull(catMenuMap.get(yapiSaveParam.getMenu()))){
                    return new YapiResponse(catMenuMap.get(yapiSaveParam.getMenu()));
                }
            }else{
                //如果默认菜单缓存不为空，并且没有已有菜单
                if(Objects.nonNull(catMenuMap.get(YapiConstant.menu)) && Objects.isNull(yapiSaveParam.getCatid())){
                    return new YapiResponse(catMenuMap.get(YapiConstant.menu));
                }
                if(Objects.nonNull(yapiSaveParam.getCatid())){
                    // 自定义菜单不为空
                    return new YapiResponse(yapiSaveParam.getCatid());
                }
                // 如果自定义菜单为空,并且默认菜单缓存不存在
                yapiSaveParam.setMenu(YapiConstant.menu);
            }
        }
        String response= null;
        try {
            response = HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpGet(yapiSaveParam.getYapiUrl()+ YapiConstant.yapiCatMenu+"?project_id="+yapiSaveParam.getProjectId()+"&token="+yapiSaveParam.getToken())),"utf-8");
            YapiResponse yapiResponse=gson.fromJson(response,YapiResponse.class);
            if(yapiResponse.getErrcode()==0) {
                List<YapiCatResponse> list = (List<YapiCatResponse>) yapiResponse.getData();
                list=gson.fromJson(gson.toJson(list),new TypeToken<List<YapiCatResponse>>() {
                }.getType());
                String[] menus=yapiSaveParam.getMenu().split("/");
                // 循环多级菜单，判断是否存在，如果不存在就创建
                //  解决多级菜单创建问题
                Integer parent_id=-1;
                Integer now_id=null;
                for(int i=0;i<menus.length;i++){
                    boolean needAdd=true;
                    now_id=null;
                    for (YapiCatResponse yapiCatResponse : list) {
                        if (yapiCatResponse.getName().equals(menus[i])) {
                            needAdd=false;
                            now_id=yapiCatResponse.get_id();
                            break;
                        }
                    }
                    if(needAdd){
                         now_id=this.addMenu(yapiSaveParam,parent_id);
                    }
                    if(i==(menus.length-1)) {
                        yapiSaveParam.setCatid(now_id.toString());
                    }else{
                        parent_id=now_id;
                    }
                }
                Map<String,Integer> catMenuMapSub=catMap.get(yapiSaveParam.getProjectId().toString());
                if(catMenuMapSub!=null){
                    catMenuMapSub.put(yapiSaveParam.getMenu(),now_id);
                }else{
                    catMenuMapSub=new HashMap<>();
                    catMenuMapSub.put(yapiSaveParam.getMenu(),now_id);
                    catMap.put(yapiSaveParam.getProjectId().toString(),catMenuMapSub);
                }
            }
            return  new YapiResponse();
        } catch (IOException e) {
            e.printStackTrace();
           return  new YapiResponse(0,e.toString());
        }
    }


    /**
     * @description: 新增菜单
     * @param: [yapiSaveParam, parent_id]
     * @return: java.lang.Integer
     * @author: chengsheng@qbb6.com
     * @date: 2019/7/28
     */ 
    private Integer addMenu(YapiSaveParam yapiSaveParam,Integer parent_id) throws IOException{
        YapiCatMenuParam  yapiCatMenuParam=new YapiCatMenuParam(yapiSaveParam.getMenu(),yapiSaveParam.getProjectId(),yapiSaveParam.getToken(),parent_id);
        String responseCat=HttpClientUtil.ObjectToString(HttpClientUtil.getHttpclient().execute(this.getHttpPost(yapiSaveParam.getYapiUrl()+YapiConstant.yapiAddCat,gson.toJson(yapiCatMenuParam))),"utf-8");
        YapiCatResponse yapiCatResponse=gson.fromJson(gson.fromJson(responseCat,YapiResponse.class).getData().toString(),YapiCatResponse.class);
        Map<String,Integer> catMenuMapSub=catMap.get(yapiSaveParam.getProjectId().toString());
        if(catMenuMapSub!=null){
            catMenuMapSub.put(yapiCatResponse.getName(),yapiCatResponse.get_id());
        }else{
            catMenuMapSub=new HashMap<>();
            catMenuMapSub.put(yapiCatResponse.getName(),yapiCatResponse.get_id());
            catMap.put(yapiSaveParam.getProjectId().toString(),catMenuMapSub);
        }
        return yapiCatResponse.get_id();
    }


}
