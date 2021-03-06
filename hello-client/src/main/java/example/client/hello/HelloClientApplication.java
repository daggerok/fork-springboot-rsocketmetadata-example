package example.client.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.Random;
import java.util.UUID;

import static picocli.CommandLine.Parameters;
import static picocli.CommandLine.populateCommand;

@SpringBootApplication
public class HelloClientApplication {
    private static final Logger LOG = LoggerFactory.getLogger(HelloClientApplication.class);

    public static void main(String... args) {
        SpringApplication.run(HelloClientApplication.class, args);
    }

    /**
     * Runs the application.
     */
    @Component
    public class Runner implements CommandLineRunner {

        @Autowired
        private RSocketRequester rSocketRequester;

        @Override
        public void run(String... args) throws Exception {
            ClientArguments params = populateCommand(new ClientArguments(), args);

            LOG.debug("method: {}", params.method);
            LOG.debug("name: {}", params.name);

            LOG.info("Sending message...");

            Random rand = new Random(System.currentTimeMillis());

            String message = rSocketRequester.route(params.method)
                    .metadata(metadataSpec -> {
                        metadataSpec.metadata(UUID.randomUUID().toString(), MimeType.valueOf("message/x.hello.trace"));
                        metadataSpec.metadata(Integer.toString(rand.nextInt()), MimeType.valueOf("message/x.hello.span"));
                    })
                    .data(params.name)
                    .retrieveMono(String.class)
                    .doOnError(throwable -> {
                        LOG.error(throwable.getMessage(), throwable);
                    })
                    .block();

            LOG.info("Response: {}", message);
        }
    }

    /**
     * Hello client command line arguments.
     */
    public static class ClientArguments {

        /**
         * RSocket method name
         */
        @Parameters(index = "0", arity = "1", description = "the method to call")
        public String method;

        /**
         * "name" argument to send to the method
         */
        @Parameters(index = "1", arity = "1", defaultValue = "name argument for method")
        public String name;
    }
}
