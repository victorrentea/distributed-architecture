package demo;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureGraphQlTester
class GatewayTest {
  @RegisterExtension static WireMockExtension customers = WireMockExtension.newInstance().build();
  @RegisterExtension static WireMockExtension orders = WireMockExtension.newInstance().build();

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry r) {
    r.add("customers", () -> "http://127.0.0.1:" + customers.getPort()); // not localhost: ::1 may hit another app
    r.add("orders", () -> "http://127.0.0.1:" + orders.getPort());
  }

  @Autowired GraphQlTester graphql;

  @ParameterizedTest(name = "{0} → customers:{1} orders:{2}")
  @CsvSource(delimiter = '|', textBlock = """
      id                          | 0 | 0
      name                        | 1 | 0
      orders { total }            | 0 | 1
      id name email orders { total } | 1 | 1
      """)
  void callsOnlyNeededServices_onceEach_inParallel(String fields, int customerCalls, int orderCalls) {
    customers.stubFor(get(anyUrl()).willReturn(okJson("{\"name\":\"Ana\",\"email\":\"ana@shop.ro\"}").withFixedDelay(1000)));
    orders.stubFor(get(anyUrl()).willReturn(okJson("[{\"id\":7,\"total\":9.5}]").withFixedDelay(1000)));

    long t0 = System.currentTimeMillis();
    graphql.document("{ customer(id: 1) { " + fields + " } }").execute().errors().verify();

    assertThat(System.currentTimeMillis() - t0).isLessThan(1800); // parallel, not 2s
    customers.verify(customerCalls, anyRequestedFor(anyUrl()));
    orders.verify(orderCalls, anyRequestedFor(anyUrl()));
  }
}
