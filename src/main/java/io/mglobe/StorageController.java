package io.mglobe;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@Configurable
class StorageController {

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    @Autowired
    private Storage storage;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Value("${bucketname}")
    String bucketName;
    @Value("${subdirectory}")
    String subdirectory;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<URL> uploadFile(@RequestPart("file") FilePart filePart) {
        //Convert the file to a byte array
        final byte[] byteArray = convertToByteArray(filePart);

        //Prepare the blobId
        //BlobId is a combination of bucketName + subdirectiory(optional) + fileName
        final BlobId blobId = constructBlobId(bucketName, subdirectory, filePart.filename());

        return Mono.just(blobId)
                //Create the blobInfo
                .map(bId -> BlobInfo.newBuilder(blobId)
                        .setContentType("application/json")
                        .build())
                //Upload the blob to GCS
                .doOnNext(blobInfo -> getStorage().create(blobInfo, byteArray))
                //Create a Signed "Path Style" URL to access the newly created Blob
                //Set the URL expiry to 10 Minutes
                .map(blobInfo -> createSignedPathStyleUrl(blobInfo, 10, TimeUnit.MINUTES));
    }

    private URL createSignedPathStyleUrl(BlobInfo blobInfo,
                                         int duration, TimeUnit timeUnit) {
        return getStorage().signUrl(blobInfo, duration, timeUnit, Storage.SignUrlOption.withPathStyle());
    }

    /**
     * Construct Blob ID
     *
     * @param bucketName
     * @param subdirectory optional
     * @param fileName
     * @return
     */
    private BlobId constructBlobId(String bucketName, @Nullable String subdirectory,
                                   String fileName) {
        return Optional.ofNullable(subdirectory)
                .map(s -> BlobId.of(bucketName, subdirectory + "/" + fileName))
                .orElse(BlobId.of(bucketName, fileName));
    }

    /**
     * Here, we convert the file to a byte array to be sent to GCS Libraries
     *
     * @param filePart File to be used
     * @return Byte Array with all the contents of the file
     */
    @SneakyThrows
    private byte[] convertToByteArray(FilePart filePart) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            filePart.content()
                    .subscribe(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        log.trace("readable byte count:" + dataBuffer.readableByteCount());
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        try {
                            bos.write(bytes);
                        } catch (IOException e) {
                            log.error("read request body error...", e);
                        }
                    });

            return bos.toByteArray();
        }
    }

}
