package study.querydsl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

  // select m from Member m where m.username = ?
  // findByUsername 이런식으로 작성하면 자동으로 쿼리를 생성해준다.
  // SpringDataJpa가 제공하는 기능이다.
  // 네이밍 규칙을 맞추면 구현체를 자동으로 만들어준다.

  List<Member> findByUsername(String username);

  List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
}
