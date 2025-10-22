package dbu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    private Cloud cloud = new Cloud();

    @Getter
    @Setter
    public static class Cloud {
        private Aws aws = new Aws();
        private Azure azure = new Azure();
        private Gcp gcp = new Gcp();

        @Getter
        @Setter
        public static class Aws {
            private String accessKey;
            private String secretKey;
            private String bucketName;
            private String region;
        }

        @Getter
        @Setter
        public static class Azure {
            private String connectionString;
            private String containerName;
        }

        @Getter
        @Setter
        public static class Gcp {
            private String credentialsPath;
            private String bucketName;
            private String projectId;
        }
    }
 
}
