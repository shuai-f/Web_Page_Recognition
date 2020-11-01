package org.example.work.crawl;


import org.example.kit.StreamKit;
import org.example.kit.entity.BiSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @Classname WebCrawl
 * @Description 网络爬虫
 * @Date 2020/10/27 20:53
 * @Created by shuaif
 */
public class WebCrawl {


    /**
     * 获取指定URL的响应报文数据
     *
     * @param httpURL 指定 URL
     * @return URL + 响应报文【含头部和网页实体】 （由于存在URL跳转，最后返回数据的URL不一定是指定的 httpURL）
     */
    public static BiSupplier<URL,byte[]> getHttpPacketLoadedWithHTML(String httpURL) throws IOException{
        // 系统代理
        System.setProperty("http.proxyHost","127.0.0.1");
        System.setProperty("http.proxyPort","10809");

        HttpURLConnection conn = null;
        InputStream in = null;
        BiSupplier<URL,byte[]> result = null;
        try{
            URL url = new URL(httpURL);
            conn = (HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(true);  // 允许 URL 跳转
            conn.setConnectTimeout(30000);  // 连接超时
            conn.setReadTimeout(90000);     // 读取超时
            conn.setRequestMethod("GET");   // 请求方法
            conn.setRequestProperty("Accept", "text/html");
//            conn.setRequestProperty("User-Agent", "Chrome/73.0.3683.103 Mozilla/5.0");
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.connect();
            if(conn.getResponseCode() == 200){ //响应码 200
                List<String> headers = new ArrayList<>();
                headers.add(conn.getHeaderField(0)); // 返回服务器的状态行,非键值对
                for(int i = 1; ; i++){ // 遍历头部，重写成 key:value 的形式
                    String k = conn.getHeaderFieldKey(i);
                    String v = conn.getHeaderField(i);
//                    System.out.println(k + ":" + v);
                    if(k == null && v == null)
                        break;
                    headers.add(k + ": " + v);
                }

                // 检测响应数据是否为 HTML
                String type = conn.getHeaderField("Content-Type");
                if(type == null || !type.contains("text/html"))
                    return null;

                ByteArrayOutputStream baos = new ByteArrayOutputStream(20 * 1024);
                for(String header : headers){ //头部数据写入
                    baos.write(header.getBytes());
                    baos.write('\r');
                    baos.write('\n');
                }
                baos.write('\r');
                baos.write('\n');
                in = conn.getInputStream();
                StreamKit.transform(in, baos); // 响应主体数据写入
                result = new BiSupplier<>(conn.getURL(), baos.toByteArray());
            }else if(conn.getResponseCode() / 100 == 3){
                String location = conn.getHeaderField("Location");
                throw new WorkerException("Error code is " + conn.getResponseCode() + " with message \"" + conn.getResponseMessage() + "\"." + (location == null ? "" : " You can visit \"" + location + "\"."));
            }
        }finally{
            if(null != in){
                try{
                    in.close();
                }catch(IOException e){
                }
            }
            if(conn != null){
                conn.disconnect();
            }
        }
        return result;
    }


    /**
     * 开源库Jsoup网页捕获+解析
     * @param URL
     * @return
     */
    public static Document webCrawl(String URL){
        Document doc = null;
        try {
              doc = Jsoup.connect(URL).get();
//            Elements elements = doc.select("");
//            for (Element element : elements) {
//                System.out.println(element.text() + ":" + element.attr("href"));
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }
}
