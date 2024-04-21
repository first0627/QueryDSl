package study.querydsl.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

public interface MemberRepositoryCustom {

  List<MemberTeamDto> search(MemberSearchCondition condition);

  // 내가 직접 totalCount를 구하는 쿼리를 만들어야 한다.
  // totalCount를 구하는 쿼리를 최적화해서 구현해야 한다.

  Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
