package com.example.volrabbit_be.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Uri;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * S3 객체로부터 InputStream을 얻어 반환함
     * @param bucketName 버킷 이름
     * @param key 객체 키
     * @return InputStream
     */
    public InputStream getInputStreamFromS3Object(String bucketName, String key) {
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                request -> request.bucket(bucketName).key(key)
        );

        return response;
    }

    /**
     * S3 객체 URI로부터 해당 객체에 대한 InputStream을 얻어 반환함
     * @param s3ObjectUri 객체 URI
     * @return InputStream
     */
    public InputStream getInputStreamFromS3ObjectUri(String s3ObjectUri) {
        Map<String, String> map = parseS3ObjectUri(s3ObjectUri);

        return getInputStreamFromS3Object(map.get("bucketName"), map.get("key"));
    }

    /**
     * S3 객체에 대해 사전 서명된 URL을 얻음
     * @param bucketName 버킷 이름
     * @param key 객체 키
     * @return 사전 서명된 URL 문자열
     */
    public String getPresignedUrlForS3Object(String bucketName, String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(objectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * S3 객체 URI로부터 해당 객체에 대한 사전 서명된 URL을 얻음
     * @param s3ObjectUri 객체 URI
     * @return 사전 서명된 URL 문자열
     */
    public String getPresignedUrlFromS3ObjectUri(String s3ObjectUri) {
        Map<String, String> map = parseS3ObjectUri(s3ObjectUri);

        return getPresignedUrlForS3Object(map.get("bucket"), map.get("key"));
    }

    private Map<String, String> parseS3ObjectUri(String s3ObjectUri) {
        Map<String, String> result = new HashMap<>();

        S3Utilities s3Utilities = s3Client.utilities();

        URI uri = URI.create(s3ObjectUri);
        S3Uri s3Uri = s3Utilities.parseUri(uri);

        String bucketName = s3Uri.bucket().orElseThrow(IllegalArgumentException::new);
        String key = s3Uri.key().orElseThrow(IllegalArgumentException::new);

        result.put("bucketName", bucketName);
        result.put("key", key);

        return result;
    }
}
