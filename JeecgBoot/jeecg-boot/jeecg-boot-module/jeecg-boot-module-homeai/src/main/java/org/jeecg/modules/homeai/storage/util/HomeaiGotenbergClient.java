package org.jeecg.modules.homeai.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 调用 Gotenberg LibreOffice 路由把 Office 转为 PDF。
 */
@Slf4j
@Component
public class HomeaiGotenbergClient {

    public Path convertToPdf(Path sourcePath, Path outDir, String baseUrl, int timeoutSeconds) throws Exception {
        if (!Files.isRegularFile(sourcePath)) {
            throw new IllegalArgumentException("源文件不存在");
        }
        Files.createDirectories(outDir);
        if (!HomeaiLocalHttpUrl.isLoopbackOrPrivate(baseUrl)) {
            throw new IllegalArgumentException("Gotenberg 地址不是本机或局域网，已拒绝上传文件");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int ms = Math.max(30, timeoutSeconds) * 1000;
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(ms);
        RestTemplate rest = new RestTemplate(factory);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("files", new FileSystemResource(sourcePath.toFile()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<byte[]> res = rest.postForEntity(
                HomeaiGotenbergConvert.convertUri(baseUrl),
                new HttpEntity<>(body, headers),
                byte[].class);
        byte[] pdf = res.getBody();
        if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P') {
            throw new RuntimeException("Gotenberg 未返回 PDF");
        }
        String base = sourcePath.getFileName().toString();
        int dot = base.lastIndexOf('.');
        String name = (dot > 0 ? base.substring(0, dot) : base) + ".pdf";
        Path dest = outDir.resolve(name);
        Files.write(dest, pdf);
        log.info("Gotenberg 完成转换: {} -> {}", sourcePath.getFileName(), dest.getFileName());
        return dest;
    }
}
