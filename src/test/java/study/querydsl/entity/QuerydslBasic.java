package study.querydsl.entity;

import static com.querydsl.jpa.JPAExpressions.select;
import static java.util.Optional.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

@SpringBootTest
@Transactional
@Commit
public class QuerydslBasic {
  @Autowired EntityManager em;
  JPAQueryFactory queryFactory;
  @PersistenceUnit EntityManagerFactory emf;

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

  // 집합
  // JPQL에서 제공하는 집합 함수를 Querydsl에서도 제공한다.
  // 실무에선 Tuple 보다 DTO로 조회하는 것을 권장한다.
  @Test
  public void aggregation() {
    List<Tuple> result =
        queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min())
            .from(member)
            .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  // 팀의 이름과 각 팀의 평균 연령을 구해라.
  @Test
  public void group() throws Exception {

    List<Tuple> result =
        queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2

    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) / 2
  }

  @Test
  public void join() throws Exception {

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

    assertThat(result).extracting("username").containsExactly("member1", "member2");
  }

  @Test
  public void theta_join() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));

    List<Member> result =
        queryFactory.select(member).from(member, team).where(member.username.eq(team.name)).fetch();

    assertThat(result).extracting("username").containsExactly("teamA", "teamB");
  }

  // 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
  // JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
  @Test
  public void join_on_filtering() throws Exception {
    List<Tuple> result =
        queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team)
            .on(team.name.eq("teamA"))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  // 연관관계 없는 엔티티 외부 조인
  // 회원의 이름이 팀 이름과 같은 대상 외부 조인
  @Test
  public void join_on_no_relation() throws Exception {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result =
        queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team)
            .on(member.username.eq(team.name))
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  @Test
  public void fetchJoinNo() throws Exception {
    em.flush();
    em.clear();

    Member findMember =
        queryFactory.selectFrom(member).where(member.username.eq("member1")).fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  public void fetchJoinUse() throws Exception {
    em.flush();
    em.clear();

    Member findMember =
        queryFactory
            .selectFrom(member)
            .join(member.team, team)
            .fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
  }

  // 서브쿼리
  // 나이가 가장 많은 회원 조회
  @Test
  public void subQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.eq(select(memberSub.age.max()).from(memberSub)))
            .fetch();
    assertThat(result).extracting("age").containsExactly(40);
  }

  // 나이가 평균 이상인 회원
  // 서브쿼리
  // in절

  // 나이가 평균 이상인 회원
  // 서브쿼리
  @Test
  public void subQueryGoe() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.goe(select(memberSub.age.avg()).from(memberSub)))
            .fetch();

    assertThat(result).extracting("age").containsExactly(30, 40);
  }

  // select 절 서브쿼리

  @Test
  public void subQueryIn() {
    QMember memberSub = new QMember("memberSub");

    List<Member> result =
        queryFactory
            .selectFrom(member)
            .where(member.age.in(select(memberSub.age).from(memberSub).where(memberSub.age.gt(10))))
            .fetch();

    assertThat(result).extracting("age").containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");

    List<Tuple> result =
        queryFactory
            .select(member.username, select(memberSub.age.avg()).from(memberSub))
            .from(member)
            .fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  // Case문
  @Test
  public void caseQuery() {
    List<String> result =
        queryFactory
            .select(member.age.when(10).then("열살").when(20).then("스무살").otherwise("기타"))
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  // 복잡한 case문
  // 단순한 조건이 아닌 복잡한 조건을 사용하고 싶다면 CaseBuilder를 사용하면 된다.
  // CaseBuilder는 조건을 체인으로 이어서 작성할 수 있다.
  // then()을 이어서 계속 사용할 수 있다.
  // otherwise()는 필수이다.
  // 이때, then()은 조건을 만족하면 반환할 값을 지정하고, otherwise()는 조건을 만족하지 않을 때 반환할 값을 지정한다.
  // 예를 들어서 다음과 같은 임의의 순서로 회원을 출력하고시다면?
  // 1. 0~20살이면 "최연소 회원"
  // 2. 21~30살이면 "중간 연령 회원"
  // 3. 그 외에는 "기타"
  @Test
  public void rankPath() throws Exception {

    NumberExpression<Integer> rankPath =
        new CaseBuilder()
            .when(member.age.between(0, 20))
            .then(2)
            .when(member.age.between(21, 30))
            .then(1)
            .otherwise(3);

    List<Tuple> result =
        queryFactory
            .select(member.username, member.age, rankPath)
            .from(member)
            .orderBy(rankPath.asc())
            .fetch();

    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      Integer rank = tuple.get(rankPath);
      System.out.println("username = " + username + ", age = " + age + ", rank = " + rank);
    }
  }

  // 상수
  // 상수를 조회하고 싶다면 select()에 상수를 입력하면 된다.

  @Test
  public void constant() throws Exception {
    List<Tuple> result =
        queryFactory.select(member.username, Expressions.constant("A")).from(member).fetch();

    for (Tuple tuple : result) {
      System.out.println("tuple = " + tuple);
    }
  }

  // 문자 더하기
  // 문자 더하기는 concat()을 사용하면 된다.
  @Test
  public void concat() throws Exception {
    List<String> result =
        queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue()))
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  // 프로젝션
  // 프로젝션은 select()에 원하는 대상을 입력하면 된다.
  // 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있다.
  @Test
  public void simpleProjection() {
    List<String> result = queryFactory.select(member.username).from(member).fetch();
    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  // 프로젝션 대상이 둘 이상이면 튜플이나 DTO로 조회할 수 있다.
  // 튜플은 Querydsl이 제공하는 Tuple 타입이다.
  // Tuple은 querydsl-core 라이브러리에 있으므로 별도로 의존성을 추가해야 한다.
  // Tuple은 querydsl-core의 Q 타입과 함께 사용해야 한다
  // Tuple도 Querydsl에 종속적인것이라서 가급적이면 DTO로 조회하는 것을 권장한다.
  @Test
  public void tupleProjection() {
    List<Tuple> result = queryFactory.select(member.username, member.age).from(member).fetch();
    for (Tuple tuple : result) {
      String username = tuple.get(member.username);
      Integer age = tuple.get(member.age);
      System.out.println("username = " + username);
      System.out.println("age = " + age);
    }
  }

  // DTO로 조회
  // DTO로 조회하려면 프로젝션 대상이 되는 필드를 DTO의 생성자나 setter에 넣어주면 된다.
  // DTO로 조회하려면 패키지와 클래스가 같아야 한다.
  // DTO로 조회하려면 생성자가 필요하다.
  // DTO로 조회하려면 필드의 이름이 같아야 한다.
  // DTO로 조회하려면 순서가 같아야 한다.
  // DTO로 조회하려면 타입이 같아야 한다.
  // DTO로 조회하려면 조회 대상이 root 엔티티면 생성자 방식, 연관 엔티티면 setter 방식을 사용하면 된다.

  @Test
  public void findDtoByJPQL() {
    List<MemberDto> result =
        em.createQuery(
                "select new study.querydsl.entity.MemberDto(m.username, m.age) from Member m",
                MemberDto.class)
            .getResultList();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // 프로퍼티 접근
  // 프로퍼티 접근은 getter, setter를 사용한다.
  // 프로퍼티 접근은 getter, setter가 있어야 한다.
  // 프로퍼티 접근은 이름이 같아야 한다.
  @Test
  public void findDtoBySetter() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.bean(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // 필드 접근
  // 필드 접근은 필드에 직접 접근한다.
  // 필드 접근은 getter, setter가 없어도 된다.
  @Test
  public void findDtoByField() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.fields(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // 생성자
  // 생성자 방식은 생성자를 통해 값을 바로 넣어준다.
  // 생성자 방식은 타입이 일치하는 필드만 넣어준다.
  // 생성자 방식은 순서가 일치하는 필드만 넣어준다.
  // 생성자 방식은 이름이 같아도 타입이 다르면 안된다.
  @Test
  public void findDtoByConstructor() {
    List<MemberDto> result =
        queryFactory
            .select(Projections.constructor(MemberDto.class, member.username, member.age))
            .from(member)
            .fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // 필드의 이름이 다를 때
  // 필드의 이름이 다르면 as를 사용하면 된다.
  // 필드의 이름이 다르면 ExpressionUtils.as를 사용하면 된다.

  @Test
  public void findUserDto() {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result =
        queryFactory
            .select(
                Projections.fields(
                    UserDto.class,
                    member.username.as("name"),
                    ExpressionUtils.as(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  //  생성자 방식으로 DTO 조회
  // 생성자 방식으로 DTO를 조회할 때는 ExpressionUtils.as를 사용하면 된다.
  // 여기서는 runtime에 오류가 발생한다.
  @Test
  public void findUserDtoByConstructor() {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result =
        queryFactory
            .select(
                Projections.constructor(
                    UserDto.class,
                    member.username,
                    ExpressionUtils.as(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")))
            .from(member)
            .fetch();

    for (UserDto userDto : result) {
      System.out.println("userDto = " + userDto);
    }
  }

  // @QueryProjection
  // @QueryProjection을 사용하면 컴파일 시점에 Q 타입을 생성할 수 있다.
  // @QueryProjection을 사용하면 DTO에 Querydsl 관련 의존성을 제거할 수 있다.
  // @QueryProjection을 사용하려면 DTO에 기본 생성자가 있어야 한다.
  // @QueryProjection을 사용하려면 필드의 순서와 타입이 일치해야 한다.
  // @QueryProjection을 사용하려면 필드의 이름이 일치해야 한다.
  // 여기서는 compile타임에 에러를 발생시켜준다

  @Test
  public void findDtoByQueryProjection() {
    List<MemberDto> result =
        queryFactory.select(new QMemberDto(member.username, member.age)).from(member).fetch();

    for (MemberDto memberDto : result) {
      System.out.println("memberDto = " + memberDto);
    }
  }

  // 동적 쿼리 - BooleanBuilder
  // 동적 쿼리를 처리할 때 BooleanBuilder를 사용하면 편리하다.
  // BooleanBuilder는 조립할 때 and(), or()를 사용하면 된다.
  // BooleanBuilder는 where()에 넣으면 된다.
  // BooleanBuilder는 null을 체크하지 않아도 된다.
  // BooleanBuilder는 조립 조건이 없으면 where()에 null을 넣으면 된다.

  @Test
  public void dynamicQuery_BooleanBuilder() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember1(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember1(String usernameParam, Integer ageParam) {

    BooleanBuilder builder = new BooleanBuilder();

    ofNullable(usernameParam).ifPresent(username -> builder.and(member.username.eq(username)));

    ofNullable(ageParam).ifPresent(age -> builder.and(member.age.eq(age)));

    return queryFactory.selectFrom(member).where(builder).fetch();
  }

  // 동적 쿼리 - Where 다중 파라미터 사용
  // where 다중 파라미터를 사용하면 조립이 자유롭다.

  @Test
  public void dynamicQuery_WhereParam() {
    String usernameParam = "member1";
    Integer ageParam = 10;

    List<Member> result = searchMember2(usernameParam, ageParam);
    assertThat(result.size()).isEqualTo(1);
  }

  private List<Member> searchMember2(String usernameCond, Integer ageCond) {
    return queryFactory.selectFrom(member).where(allEq(usernameCond, ageCond)).fetch();
  }

  //
  private BooleanExpression usernameEq(String usernameCond) {
    return usernameCond != null ? member.username.eq(usernameCond) : null;
  }

  // 동적 쿼리 - 조립
  private BooleanExpression ageEq(Integer ageCond) {
    return ageCond != null ? member.age.eq(ageCond) : null;
  }

  // 동적 쿼리 - 조립

  private BooleanExpression allEq(String usernameCond, Integer ageCond) {
    return usernameEq(usernameCond).and(ageEq(ageCond));
  }

  // 수정, 삭제 벌크 연산
  // 벌크 연산은 영속성 컨텍스트를 무시하고 실행한다.
  // 벌크 연산을 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.

  @Test
  public void bulkUpdate() {

    // member1 = 10 -> member1
    // member2 = 20 -> member2
    // member3 = 30 -> member3
    // member4 = 40 -> member4

    long count =
        queryFactory.update(member).set(member.username, "비회원").where(member.age.lt(28)).execute();

    // member1 = 10 -> 비회원
    // member2 = 20 -> 비회원
    // member3 = 30 -> member3
    // member4 = 40 -> member4

    // 벌크 연산을 실행하고 나면 영속성 컨텍스트를 초기화 하는 것이 안전하다.
    // 스프링 Data JPA를 사용하면 벌크 연산을 실행하고 나면 영속성 컨텍스트를 초기화 한다.

    em.flush();
    em.clear();

    List<Member> result = queryFactory.selectFrom(member).fetch();

    for (Member member1 : result) {
      System.out.println("member1 = " + member1);
    }
  }

  // 벌크 더하기 연산
  @Test
  public void bulkAdd() {
    long count = queryFactory.update(member).set(member.age, member.age.add(1)).execute();
  }

  // 벌크 삭제
  @Test
  public void bulkDelete() {
    long count = queryFactory.delete(member).where(member.age.gt(18)).execute();
  }

  // SQL function 호출하기
  // SQL function 호출은 JPA와 같이 Dialect에 등록된 SQL function을 호출할 수 있다.
  // SQL function 호출은 Querydsl이 제공하는 Expressions를 사용하면 된다.
  @Test
  public void sqlFunction() {
    List<String> result =
        queryFactory
            .select(
                Expressions.stringTemplate(
                    "function('replace', {0}, {1}, {2})", member.username, "member", "M"))
            .from(member)
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }

  @Test
  public void sqlFunction2() {
    // lower

    List<String> result =
        queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(member.username.lower()))
            .fetch();

    for (String s : result) {
      System.out.println("s = " + s);
    }
  }
}
