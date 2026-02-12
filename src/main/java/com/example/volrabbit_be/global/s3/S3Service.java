package com.example.volrabbit_be.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

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
     * S3 객체에 대해 사전 서명된 URL을 얻음
     * @param bucketName 버킷 이름
     * @param key 객체 키
     * @return 사전 서명된 URL 문자열
     */
    public static String getPresignedUrlForS3Object(String bucketName, String key) {
        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(objectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        }
    }
}
