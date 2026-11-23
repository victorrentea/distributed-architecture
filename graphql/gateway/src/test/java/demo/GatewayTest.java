package demo;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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

  @BeforeEach
  void stubs() { // each service answers in 1s
    customers.stubFor(get(anyUrl()).willReturn(okJson("{\"name\":\"Ana\",\"email\":\"ana@shop.ro\"}").withFixedDelay(1000)));
    orders.stubFor(get(anyUrl()).willReturn(okJson("[{\"id\":7,\"total\":9.5}]").withFixedDelay(1000)));
  }

  @Test
  void nameOnly_callsOnlyCustomers() {
    graphql.document("""
        {
          customer(id: 1) {
            name
          }
        }
        """).execute().path("customer.name").entity(String.class).isEqualTo("Ana");

    customers.verify(1, anyRequestedFor(anyUrl()));
    orders.verify(0, anyRequestedFor(anyUrl()));
  }

  @Test
  void ordersOnly_callsOnlyOrders() {
    graphql.document("""
        {
          customer(id: 1) {
            orders {
              total
            }
          }
        }
        """).execute().path("customer.orders[0].total").entity(Double.class).isEqualTo(9.5);

    customers.verify(0, anyRequestedFor(anyUrl()));
    orders.verify(1, anyRequestedFor(anyUrl()));
  }

  @Test
  void everything_callsEachServiceOnce_inParallel() {
    long t0 = System.currentTimeMillis();
    graphql.document("""
        {
          customer(id: 1) {
            id
            name
            email
            orders {
              total
            }
          }
        }
        """).execute().errors().verify();

    assertThat(System.currentTimeMillis() - t0).isLessThan(1800); // ~1s, not 2s
    customers.verify(1, anyRequestedFor(anyUrl())); // name + email: one call
    orders.verify(1, anyRequestedFor(anyUrl()));
  }
}
