package study.querydsl;

import static org.assertj.core.api.Assertions.*;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

@SpringBootTest
@Transactional
@Commit
class QuerydslApplicationTests {

  @Autowired EntityManager em;

  @Test
  void contextLoads() {
    Hello hello = new Hello();
    em.persist(hello);

    JPAQueryFactory query = new JPAQueryFactory(em);
    QHello qHello = QHello.hello;

    Hello result = query.selectFrom(qHello).fetchOne();

    // result와 hello는 같다. 왜냐하면 같은 트랜잭션 안에서 영속성 컨텍스트가 유지되기 때문이다.
    assertThat(result).isEqualTo(hello);

    // result와 hello의 id는 같다. 왜냐하면 같은 트랜잭션 안에서 영속성 컨텍스트가 유지되기 때문이다.ㄴ
    assert result != null;
    assertThat(result.getId()).isEqualTo(hello.getId());
  }
}
