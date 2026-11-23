package demo;

import graphql.schema.DataFetchingFieldSelectionSet;
import jakarta.persistence.*;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

@SpringBootApplication
@Controller
public class CustomersApp {
  public static void main(String[] args) {
    SpringApplication.run(CustomersApp.class, args);
  }

  @Entity(name = "Customer") // nested class: name it, or the table becomes customers_app$customer
  public static class Customer {
    @Id public Long id;
    public String name;
    public String email;
    @OneToMany @JoinColumn(name = "customer_id") public List<Address> addresses; // LAZY
  }

  @Entity(name = "Address")
  public static class Address {
    @Id public Long id;
    public String city;
  }

  @PersistenceContext EntityManager em;

  @QueryMapping
  Customer customer(@Argument long id, DataFetchingFieldSelectionSet graphqlFields) {
    boolean addressesRequested = graphqlFields.contains("addresses");
    String join = addressesRequested ? " left join fetch c.addresses" : ""; // ONE query, joined only if asked
    String jpql = "select c from Customer c" + join + " where c.id = :id";
    return em.createQuery(jpql, Customer.class).setParameter("id", id).getSingleResult();
  }
}
