package dbu.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jline.utils.AttributedString;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.jline.PromptProvider;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

        @Bean
        @SuppressWarnings("unused")
        PromptProvider prompt() {
                return () -> new AttributedString("dbu:");
        }

        @Bean
        @SuppressWarnings("unused")
        S3Client s3Client(AppProperties props) {
                AwsCredentials credentials = AwsBasicCredentials.create(props.getCloud().getAws().getAccessKey(),
                                props.getCloud().getAws().getSecretKey());
                return S3Client.builder().region(Region.of(props.getCloud().getAws().getRegion()))
                                .credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
        }

        @Bean
        @SuppressWarnings("unused")
        BlobContainerClient blobContainerClient(AppProperties props) {
                return new BlobContainerClientBuilder()
                                .connectionString(props.getCloud().getAzure().getConnectionString())
                                .containerName(props.getCloud().getAzure().getContainerName())
                                .buildClient();
        }

        @Bean
        @SuppressWarnings("unused")
        Storage storage(AppProperties props) throws FileNotFoundException, IOException {
                Credentials credentials = GoogleCredentials
                                .fromStream(new FileInputStream(props.getCloud().getGcp().getCredentialsPath()));
                return StorageOptions.newBuilder().setCredentials(credentials)
                                .setProjectId(props.getCloud().getGcp().getProjectId()).build().getService();
        }

}
