package demo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

@SpringBootTest
@AutoConfigureGraphQlTester
class CustomersTest {
  @Autowired GraphQlTester graphql;
  Statistics sql;

  @BeforeEach
  void countSql(@Autowired EntityManagerFactory emf) {
    sql = emf.unwrap(SessionFactory.class).getStatistics();
    sql.clear();
  }

  @Test
  void withoutAddresses_oneQuery_noJoin() {
    GraphQlTester.Response response = graphql.document("""
        {
          customer(id: 1) {
            name
            email
          }
        }
        """).execute();

    response.path("customer.name").entity(String.class).isEqualTo("Ana");
    assertThat(sql.getPrepareStatementCount()).isEqualTo(1);
    assertThat(sql.getQueries()).singleElement().asString().doesNotContain("join");
  }

  @Test
  void withAddresses_oneQuery_withJoin() {
    GraphQlTester.Response response = graphql.document("""
        {
          customer(id: 1) {
            name
            addresses {
              city
            }
          }
        }
        """).execute();

    response.path("customer.addresses[*].city").entityList(String.class).containsExactly("Cluj", "Iasi");
    assertThat(sql.getPrepareStatementCount()).isEqualTo(1);
    assertThat(sql.getQueries()).singleElement().asString().contains("join fetch c.addresses");
  }
}
