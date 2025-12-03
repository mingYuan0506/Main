package com.example.main;

import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiApiUtils {
    // 警告：请勿硬编码提交到公开仓库
    private static final String TONGYI_API_KEY = "sk-dd11928b19644d5e873811554611ef82";

    private static final String TONGYI_VL_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static final String TONGYI_TEXT_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    /**
     * 【已修正】
     * 调用通义千问，根据用户历史行为推荐活动
     * @param userId 用户ID
     * @param interacted 用户已互动的活动列表
     * @param candidate 候选活动列表
     * @return 推荐结果的JSON字符串
     */
    public static String getAiRecommendations(String userId, List<ActivityBean> interacted, List<ActivityBean> candidate) {
        // 1. 构造Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是校园活动推荐助手，基于用户已参与的活动，从候选活动中推荐几个最匹配的。\n");
        prompt.append("### 要求：\n");
        prompt.append("1. 只返回JSON，无多余文字！JSON包含\"recommendations\"数组；\n");
        prompt.append("2. 每个元素含2个字段：activity_id（候选活动ID，整数）、reason（推荐理由）；\n");

        prompt.append("\n### 已参与活动：\n");
        for (ActivityBean activity : interacted) {
            prompt.append(String.format("ID：%d，标题：%s，类型：%s，时间：%s，地点：%s，描述：%s，组织者：%s，标签：%s\n", activity.getId(), activity.getTitle(), activity.getType(), activity.getTime(), activity.getLocation(), activity.getDescription(), activity.getOrganizer(), activity.getTag()));
        }

        prompt.append("\n### 候选活动：\n");
        for (ActivityBean activity : candidate) {
            prompt.append(String.format("ID：%d，标题：%s，类型：%s，时间：%s，地点：%s，描述：%s，组织者：%s，标签：%s\n", activity.getId(), activity.getTitle(), activity.getType(), activity.getTime(), activity.getLocation(), activity.getDescription(), activity.getOrganizer(), activity.getTag()));
        }

        // 2. 构造通义千问请求体
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "qwen-turbo");
            requestBody.put("input", new JSONObject()
                    .put("messages", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content", prompt.toString()))));
            requestBody.put("parameters", new JSONObject()
                    .put("result_format", "json")
                    .put("temperature", 0.6)
                    .put("max_tokens", 1024));
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"recommendations\": []}";
        }

        // 3. 发送请求
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(TONGYI_TEXT_API_URL) // 使用纯文本模型URL
                .addHeader("Authorization", "Bearer " + TONGYI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        String rawResponse = "";
        try {
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                rawResponse = response.body().string();
            }

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(rawResponse);
                if (responseJson.has("output") && responseJson.getJSONObject("output").has("choices")) {
                    return responseJson.getJSONObject("output")
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{\"recommendations\": []}";
    }

    /**
     * 调用通义千问，根据用户提供的文本内容生成活动摘要。
     * @param textContent 用户输入的笔记或从图片OCR识别出的文字。
     * @return 大模型生成的活动摘要文本。
     */
    public static String generateActivitySummary(String textContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的活动总结助手。\n");
        prompt.append("请根据以下用户提供的活动笔记或照片文字记录，整理成一份清晰、精炼的活动摘要。\n\n");
        prompt.append("### 总结要求：\n");
        prompt.append("1. 提炼活动的核心主题和关键观点。\n");
        prompt.append("2. 使用分点或编号的方式列出主要内容，使其条理清晰。\n");
        prompt.append("3. 语言流畅，去除冗余和口语化的表达。\n");
        prompt.append("4. 忽略与活动无关的杂乱信息（如OCR识别错误）。\n");
        prompt.append("5. 摘要内容应客观、准确，忠实于原文。\n\n");
        prompt.append("### 原始文本：\n");
        prompt.append("```\n");
        prompt.append(textContent);
        prompt.append("\n```");

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "qwen-turbo");
            JSONObject input = new JSONObject();
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt.toString());
            messages.put(userMessage);
            input.put("messages", messages);
            requestBody.put("input", input);
            JSONObject parameters = new JSONObject();
            parameters.put("temperature", 0.3);
            parameters.put("max_tokens", 1500);
            requestBody.put("parameters", parameters);
        } catch (Exception e) {
            e.printStackTrace();
            return "请求构建失败。";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(TONGYI_TEXT_API_URL) // 使用纯文本模型URL
                .addHeader("Authorization", "Bearer " + TONGYI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        String rawResponse = "";
        try {
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                rawResponse = response.body().string();
            }

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(rawResponse);
                if (responseJson.has("output") && responseJson.getJSONObject("output").has("text")) {
                    return responseJson.getJSONObject("output").getString("text").trim();
                } else {
                    return "AI服务返回错误: " + responseJson.optString("message", "未知错误");
                }
            } else {
                return "AI服务请求失败, HTTP状态码: " + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "生成摘要时发生网络或解析异常。";
        }
    }

    /**
     * 调用通义千问-VL多模态模型，从图片中识别文字 (OCR)
     * @param imageBitmap 用户选择的图片
     * @return 识别出的文字，如果失败则返回错误信息
     */
    public static String getTextFromImage(Bitmap imageBitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "qwen-vl-plus");

            JSONObject input = new JSONObject();
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");

            JSONArray contentArray = new JSONArray();
            JSONObject textContent = new JSONObject();
            textContent.put("text", "这张图片里的所有文字是什么？请直接返回识别出的纯文本。");
            contentArray.put(textContent);
            JSONObject imageContent = new JSONObject();
            imageContent.put("image", "data:image/jpeg;base64," + imageBase64);
            contentArray.put(imageContent);

            message.put("content", contentArray);
            messages.put(message);
            input.put("messages", messages);
            requestBody.put("input", input);

        } catch (Exception e) {
            e.printStackTrace();
            return "图片识别请求构建失败。";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(TONGYI_VL_API_URL) // 使用多模态模型URL
                .addHeader("Authorization", "Bearer " + TONGYI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        String rawResponse = "";
        try {
            Response response = client.newCall(request).execute();
            if (response.body() != null) {
                rawResponse = response.body().string();
            }

            if (response.isSuccessful()) {
                JSONObject responseJson = new JSONObject(rawResponse);
                if (responseJson.has("output") && responseJson.getJSONObject("output").has("choices")) {
                    return responseJson.getJSONObject("output")
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                } else {
                    return "图片识别服务返回错误: " + responseJson.optString("message", "未知错误");
                }
            } else {
                return "图片识别服务请求失败, HTTP状态码: " + response.code();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "图片识别时发生网络或解析异常。";
        }
    }
}