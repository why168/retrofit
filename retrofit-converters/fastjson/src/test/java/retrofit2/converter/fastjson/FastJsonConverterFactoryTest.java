package retrofit2.converter.fastjson;

import com.alibaba.fastjson.annotation.JSONField;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * @author Edwin.Wu edwin.wu05@gmail.com
 * @version 2019-08-01 11:30
 * @since JDK1.8
 */
public class FastJsonConverterFactoryTest {
    interface AnInterface {
        @JSONField(name = "name")
        String getName();

        @JSONField(name = "name")
        void setName(String name);
    }

    static class AnImplementation implements AnInterface {

        @JSONField(name = "name")
        private String theName;

        AnImplementation() {
        }

        AnImplementation(String name) {
            theName = name;
        }

        @JSONField(name = "name")
        @Override
        public String getName() {
            return theName;
        }

        @JSONField(name = "name")
        @Override
        public void setName(String name) {
            theName = name;
        }
    }

    interface Service {
        @POST("/")
        Call<AnImplementation> anImplementation(@Body AnImplementation impl);

        @POST("/")
        Call<AnInterface> anInterface(@Body AnInterface impl);
    }

    @Rule
    public final MockWebServer server = new MockWebServer();

    private Service service;

    @Before
    public void setUp() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(FastJsonConverterFactory.create())
                .build();
        service = retrofit.create(Service.class);
    }

    @Test
    public void anInterface() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{\"name\":\"value\"}"));

        Call<AnInterface> call = service.anInterface(new AnImplementation("value"));
        Response<AnInterface> response = call.execute();
        AnInterface body = response.body();
        assertThat(body.getName()).isEqualTo("value");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    public void anImplementation() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{\"theName\":\"value\"}"));

        Call<AnImplementation> call = service.anImplementation(new AnImplementation("value"));
        Response<AnImplementation> response = call.execute();
        AnImplementation body = response.body();
        assertThat(body.theName).isNull();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"value\"}");
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

    @Test
    public void serializeUsesConfiguration() throws IOException, InterruptedException {
        server.enqueue(new MockResponse().setBody("{}"));

        service.anImplementation(new AnImplementation(null)).execute();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getBody().readUtf8()).isEqualTo("{}"); // Null value was not serialized.
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json; charset=UTF-8");
    }

}