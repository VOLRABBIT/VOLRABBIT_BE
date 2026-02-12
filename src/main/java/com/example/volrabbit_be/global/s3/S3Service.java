package com.example.volrabbit_be.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;

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
}
