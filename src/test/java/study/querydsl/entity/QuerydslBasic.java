package study.querydsl.entity;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslBasic {
  @Autowired EntityManager em;
  JPAQueryFactory queryFactory;

  @BeforeEach
  public void before() {
    queryFactory = new JPAQueryFactory(em);
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  public void startJPQL() {
    // member1을 찾아라
    String qlString = "select m from Member m where m.username = :username";
    Member findMember =
        em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void startQuerydsl() {
    // member1을 찾아라

    Member findMember =
        queryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();

    assert findMember != null;
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void search() {
    Member findMember =
        queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1").and(member.age.eq(10)))
            .fetchOne();

    assert findMember != null;
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void searchAndParam() {
    Member findMember =
        queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1"), member.age.eq(10))
            .fetchOne();

    assert findMember != null;
    assertThat(findMember.getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
    // List
    // List<Member> fetch = queryFactory.selectFrom(member).fetch();

    // 단 건
    // Member fetchOne = queryFactory.selectFrom(member).fetchOne();

    // 처음 한 건 조회
    // Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

    // 페이징에서 사용
    //    QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();
    //    results.getTotal();
    //    List<Member> content = results.getResults();
    //
    // count 쿼리로 변경
    long total = queryFactory.selectFrom(member).fetchCount();
  }

  /** 회원 정렬 순서 1. 회원 나이 내림차순 2. 회원 이름 올림차순 단 2에서 회원 이름이 없으면 마지막에 출력 */
  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.asc().nullsLast())
            .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);

    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  // 페이징
  // 회원을 나이 내림차순으로 정렬하고 이름 올림차순으로 정렬해서
  //
  @Test
  public void paging1() {
    List<Member> result =
        queryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2).fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  // 전체 조회 수가 필요하다면 fetchResults()를 사용하면 된다.
  // fetchResults()는 QueryResults를 반환하는데, QueryResults는 Querydsl이 제공하는 페이징을 위한 데이터를 제공한다.
  // QueryResults는 getTotal()을 제공하는데, 이를 사용하면 전체 데이터 수를 알 수 있다.
  // QueryResults는 getResults()를 제공하는데, 이를 사용하면 조회한 데이터를 List로 반환한다.
  // QueryResults는 getLimit(), getOffset()을 제공하는데, 이를 사용하면 페이징 시작 위치, 조회할 데이터 수를 알 수 있다.
  @Test
  public void paging2() {
    QueryResults<Member> results =
        queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

    assertThat(results.getTotal()).isEqualTo(4);
    assertThat(results.getLimit()).isEqualTo(2);
    assertThat(results.getOffset()).isEqualTo(1);
    assertThat(results.getResults().size()).isEqualTo(2);
  }
}
